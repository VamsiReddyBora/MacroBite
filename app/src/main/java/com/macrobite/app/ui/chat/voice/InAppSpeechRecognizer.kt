package com.macrobite.app.ui.chat.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * In-App Speech Recognizer that matches Google Assistant voice input behavior:
 *
 * 1. Initial State:
 *    - Opens microphone once for single-turn recognition.
 *    - Ambient noise / breathing does NOT count as human speech.
 *    - If no human words are detected, it cleanly times out and turns off the mic.
 *    - No rapid toggling or looping when silent.
 *
 * 2. Human Word Detection:
 *    - Only actual transcribed text from onPartialResults or onResults marks speech as detected.
 *    - Displays transcription live on screen.
 *
 * 3. Google Assistant Style Extension:
 *    - Once human words ARE detected, if the user pauses mid-sentence, it extends the listening
 *      period with a smooth silence debounce window (1.8s) so the user is not cut off.
 *    - If the user continues speaking, additional phrases are appended seamlessly.
 *    - When silence is detected after words, it cleanly finalizes and submits the full query.
 */
class InAppSpeechRecognizer(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onErrorCallback: ((String) -> Unit)? = null
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _liveSpokenText = MutableStateFlow("")
    val liveSpokenText: StateFlow<String> = _liveSpokenText.asStateFlow()

    private val _statusMessage = MutableStateFlow("Listening... Speak now")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    // Accumulated transcription across sentence pauses
    private val accumulatedText = StringBuilder()
    private var currentPartialText = ""

    // Flags for Google Assistant behavior
    private var hasDetectedHumanWords = false
    private var isExplicitlyStopped = false
    private var extensionCount = 0
    private val MAX_EXTENSIONS = 4

    // Debounce timer for silence cutoff after human words have been detected
    private var debounceSubmitJob: Job? = null

    init {
        mainHandler.post {
            initRecognizer()
        }
    }

    private fun initRecognizer() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null

            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createListener())
                }
                Log.d("InAppSpeech", "SpeechRecognizer created")
            } else {
                Log.w("InAppSpeech", "Speech recognition service not available on device")
            }
        } catch (e: Throwable) {
            Log.e("InAppSpeech", "Failed to create SpeechRecognizer", e)
            speechRecognizer = null
        }
    }

    private fun buildRecognizerIntent(): Intent {
        val languageTag = Locale.getDefault().toLanguageTag()
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            // Safe silence lengths for devices that support them
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("InAppSpeech", "onReadyForSpeech")
                if (!isExplicitlyStopped) {
                    _isListening.value = true
                }
            }

            override fun onBeginningOfSpeech() {
                Log.d("InAppSpeech", "onBeginningOfSpeech")
                // User started speaking a phrase: cancel pending silence timer
                debounceSubmitJob?.cancel()
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Audio level updates only.
                // Crucial: Ambient noise/breathing must NOT trigger word detection or restart logic.
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d("InAppSpeech", "onEndOfSpeech")
                // End of current speech phrase detected by engine.
                // Keep _isListening.value true to avoid UI flickering while onResults arrives.
            }

            override fun onError(error: Int) {
                Log.w("InAppSpeech", "SpeechRecognizer onError: $error, hasWords: $hasDetectedHumanWords")
                if (isExplicitlyStopped || !_isListening.value) return

                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        if (hasDetectedHumanWords && accumulatedText.isNotBlank()) {
                            // Words were already detected; silence followed. User is done speaking!
                            finishListeningSession()
                        } else {
                            // No words were spoken at all: close cleanly without restarting.
                            stopListeningCleanly(message = "Didn't hear speech. Tap mic to speak.")
                        }
                    }
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                    SpeechRecognizer.ERROR_CLIENT -> {
                        // Recognizer busy or client error: NEVER restart in a loop.
                        if (hasDetectedHumanWords && accumulatedText.isNotBlank()) {
                            finishListeningSession()
                        } else {
                            stopListeningCleanly()
                        }
                    }
                    else -> {
                        if (hasDetectedHumanWords && accumulatedText.isNotBlank()) {
                            finishListeningSession()
                        } else {
                            stopListeningCleanly(message = "Voice error ($error). Tap mic to retry.")
                            onErrorCallback?.invoke("Voice recognition error ($error)")
                        }
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                Log.d("InAppSpeech", "onResults received")
                if (isExplicitlyStopped || !_isListening.value) return

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val segment = matches?.firstOrNull()?.trim() ?: ""

                if (segment.isNotBlank()) {
                    hasDetectedHumanWords = true
                    if (accumulatedText.isNotEmpty()) {
                        accumulatedText.append(" ")
                    }
                    accumulatedText.append(segment)
                    currentPartialText = ""

                    val fullText = accumulatedText.toString()
                    _liveSpokenText.value = fullText
                    _statusMessage.value = fullText
                }

                if (!hasDetectedHumanWords || accumulatedText.isBlank()) {
                    // No human words recognized: stop cleanly
                    stopListeningCleanly(message = "Didn't catch that. Tap mic to speak.")
                    return
                }

                // Google Assistant style pause tolerance:
                // Words were spoken! Allow user a 1.8s grace window to continue speaking mid-sentence.
                if (extensionCount < MAX_EXTENSIONS) {
                    extensionCount++
                    scheduleDebounceSubmit(delayMs = 1800L)
                    restartRecognizerSessionForExtension()
                } else {
                    finishListeningSession()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                if (isExplicitlyStopped || !_isListening.value) return

                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim() ?: ""
                if (text.isNotBlank()) {
                    // Actual human words detected!
                    hasDetectedHumanWords = true
                    currentPartialText = text
                    debounceSubmitJob?.cancel()

                    val full = if (accumulatedText.isNotEmpty()) {
                        "$accumulatedText $currentPartialText"
                    } else {
                        currentPartialText
                    }
                    _liveSpokenText.value = full
                    _statusMessage.value = full
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    /**
     * Starts listening. Opens the mic once calmly.
     * If user doesn't say any words, it stops cleanly without rapid toggling.
     */
    fun startListening() {
        isExplicitlyStopped = false
        hasDetectedHumanWords = false
        extensionCount = 0
        accumulatedText.clear()
        currentPartialText = ""
        debounceSubmitJob?.cancel()

        _isListening.value = true
        _liveSpokenText.value = ""
        _statusMessage.value = "Listening... Speak now"

        mainHandler.post {
            try {
                if (speechRecognizer == null) {
                    initRecognizer()
                }
                speechRecognizer?.cancel()
                val intent = buildRecognizerIntent()
                speechRecognizer?.startListening(intent)
                Log.d("InAppSpeech", "startListening initiated")
            } catch (e: Throwable) {
                Log.e("InAppSpeech", "Failed to start listening", e)
                stopListeningCleanly("Microphone initialization failed.")
            }
        }
    }

    /**
     * Schedules submission of accumulated words after a period of silence.
     * If new words arrive before this fires, the timer is canceled and reset.
     */
    private fun scheduleDebounceSubmit(delayMs: Long) {
        debounceSubmitJob?.cancel()
        debounceSubmitJob = scope.launch {
            delay(delayMs)
            if (!isExplicitlyStopped && _isListening.value) {
                Log.d("InAppSpeech", "Silence threshold reached after words. Submitting.")
                finishListeningSession()
            }
        }
    }

    /**
     * Seamlessly opens the recognizer to catch the next words in a multi-phrase sentence.
     * Crucially: _isListening remains true throughout so the mic halo remains steady.
     */
    private fun restartRecognizerSessionForExtension() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
            } catch (_: Throwable) {}

            mainHandler.postDelayed({
                if (_isListening.value && !isExplicitlyStopped) {
                    try {
                        val intent = buildRecognizerIntent()
                        speechRecognizer?.startListening(intent)
                        Log.d("InAppSpeech", "Extension listening session started")
                    } catch (e: Throwable) {
                        Log.e("InAppSpeech", "Failed to start extension session", e)
                        finishListeningSession()
                    }
                }
            }, 150)
        }
    }

    /**
     * Finishes listening cleanly and delivers the final query.
     */
    private fun finishListeningSession() {
        if (!_isListening.value && isExplicitlyStopped) return

        debounceSubmitJob?.cancel()
        _isListening.value = false

        val fullText = (if (currentPartialText.isNotBlank()) {
            if (accumulatedText.isNotEmpty()) "$accumulatedText $currentPartialText" else currentPartialText
        } else {
            accumulatedText.toString()
        }).trim()

        accumulatedText.clear()
        currentPartialText = ""

        mainHandler.post {
            try {
                speechRecognizer?.cancel()
            } catch (e: Throwable) {
                Log.e("InAppSpeech", "Error canceling recognizer", e)
            }
        }

        if (fullText.isNotBlank()) {
            _statusMessage.value = "Understood: \"$fullText\""
            _liveSpokenText.value = fullText
            onResult(fullText)
        } else {
            _statusMessage.value = "Didn't catch that."
        }
    }

    /**
     * Cleanly stops listening when no words were spoken or when aborted.
     * Prevents any restart loops.
     */
    private fun stopListeningCleanly(message: String = "") {
        debounceSubmitJob?.cancel()
        _isListening.value = false
        accumulatedText.clear()
        currentPartialText = ""
        _liveSpokenText.value = ""
        if (message.isNotBlank()) {
            _statusMessage.value = message
        }
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
            } catch (_: Throwable) {}
        }
    }

    /**
     * Stop manually when user taps the glowing microphone button to finish speaking immediately.
     */
    fun stopListening() {
        isExplicitlyStopped = true
        debounceSubmitJob?.cancel()
        _isListening.value = false

        val fullText = (if (currentPartialText.isNotBlank()) {
            if (accumulatedText.isNotEmpty()) "$accumulatedText $currentPartialText" else currentPartialText
        } else {
            accumulatedText.toString()
        }).trim()

        accumulatedText.clear()
        currentPartialText = ""

        mainHandler.post {
            try {
                speechRecognizer?.cancel()
            } catch (_: Throwable) {}
        }

        if (fullText.isNotBlank()) {
            _statusMessage.value = "Understood: \"$fullText\""
            _liveSpokenText.value = fullText
            onResult(fullText)
        }
    }

    fun destroy() {
        isExplicitlyStopped = true
        debounceSubmitJob?.cancel()
        _isListening.value = false
        accumulatedText.clear()
        currentPartialText = ""

        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Throwable) {
                Log.e("InAppSpeech", "Error destroying recognizer", e)
            }
        }
    }
}
