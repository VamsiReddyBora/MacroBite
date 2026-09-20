package com.macrobite.app.ui.chat.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.macrobite.app.R
import java.util.LinkedList
import java.util.Locale
import java.util.Queue

/**
 * Text-to-Speech & Authentic Minion Audio Engine for MacroBite Assistant.
 */
class AssistantTtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var mediaPlayer: MediaPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val segmentQueue: Queue<MinionSegment> = LinkedList()
    private var isPlaying = false

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Throwable) {
            Log.e("AssistantTts", "Failed to construct TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.language = Locale.US
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                
                override fun onDone(utteranceId: String?) {
                    if (utteranceId?.startsWith("minion_tts_") == true) {
                        mainHandler.post { playNextSegment() }
                    }
                }
                
                override fun onError(utteranceId: String?) {
                    if (utteranceId?.startsWith("minion_tts_") == true) {
                        mainHandler.post { playNextSegment() }
                    }
                }
            })
        } else {
            Log.w("AssistantTts", "TextToSpeech init failed with code $status")
        }
    }

    fun playMinionSound(soundName: String, onComplete: (() -> Unit)? = null) {
        mainHandler.post {
            try {
                stopMediaPlayer()

                val resId = when (soundName.lowercase().trim()) {
                    "banana" -> R.raw.minion_banana
                    "tulaliloo", "ti_amo" -> R.raw.minion_tulaliloo
                    "hehe", "laugh" -> R.raw.minion_hehe
                    "yay" -> R.raw.minion_yay
                    "tada" -> R.raw.minion_tada
                    else -> R.raw.minion_hello
                }

                mediaPlayer = MediaPlayer.create(context.applicationContext, resId)?.apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                            .build()
                    )
                    setOnCompletionListener { mp ->
                        try {
                            mp.release()
                        } catch (_: Throwable) {}
                        if (mediaPlayer == mp) mediaPlayer = null
                        onComplete?.invoke()
                    }
                    start()
                }
            } catch (e: Throwable) {
                Log.e("AssistantTts", "Failed to play Minion audio clip", e)
                onComplete?.invoke()
            }
        }
    }

    fun speak(text: String, voiceStyle: String = "normal") {
        if (!isInitialized || tts == null) return
        stop()

        try {
            val isMinion = voiceStyle.equals("minion", ignoreCase = true)
            val cleanText = sanitizeTextForSpeech(text, isMinion = isMinion)
            if (cleanText.isBlank()) return

            if (isMinion) {
                speakMinionese(cleanText)
            } else {
                // Natural warm assistant voice
                tts?.setPitch(1.0f)
                tts?.setSpeechRate(1.0f)
                tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "assistant_tts_${System.currentTimeMillis()}")
            }
        } catch (e: Throwable) {
            Log.e("AssistantTts", "Error during TTS speak", e)
        }
    }

    private fun speakMinionese(text: String) {
        val segments = parseMinioneseSegments(text)
        segmentQueue.clear()
        segmentQueue.addAll(segments)
        isPlaying = true
        playNextSegment()
    }

    private data class MinionSegment(
        val type: SegmentType,
        val content: String,
        val audioResId: Int? = null,
        val audioName: String? = null
    )

    private enum class SegmentType { AUDIO_CLIP, TTS_MINIONESE }

    private fun parseMinioneseSegments(text: String): List<MinionSegment> {
        val segments = mutableListOf<MinionSegment>()
        
        val sentences = text.split(Regex("(?<=[.!?])\\s+"))
        
        for ((index, sentence) in sentences.withIndex()) {
            val lowerSent = sentence.lowercase()
            
            if (index == 0 && (lowerSent.contains("hello") || lowerSent.contains("hi") || lowerSent.contains("hey"))) {
                segments.add(MinionSegment(SegmentType.AUDIO_CLIP, "", audioName = "hello"))
            } 
            else if (lowerSent.contains("yay") || lowerSent.contains("great") || lowerSent.contains("awesome") || lowerSent.contains("good")) {
                segments.add(MinionSegment(SegmentType.AUDIO_CLIP, "", audioName = "yay"))
            }
            else if (lowerSent.contains("tada") || lowerSent.contains("wow") || lowerSent.contains("here")) {
                segments.add(MinionSegment(SegmentType.AUDIO_CLIP, "", audioName = "tada"))
            }
            else if (lowerSent.contains("banana") || lowerSent.contains("fruit") || lowerSent.contains("sweet")) {
                segments.add(MinionSegment(SegmentType.AUDIO_CLIP, "", audioName = "banana"))
            }
            else if (lowerSent.contains("funny") || lowerSent.contains("haha") || lowerSent.contains("hehe")) {
                segments.add(MinionSegment(SegmentType.AUDIO_CLIP, "", audioName = "hehe"))
            }
            else if (lowerSent.contains("love")) {
                segments.add(MinionSegment(SegmentType.AUDIO_CLIP, "", audioName = "tulaliloo"))
            }
            
            val minioneseSentence = convertToMinionese(sentence)
            if (minioneseSentence.isNotBlank()) {
                segments.add(MinionSegment(SegmentType.TTS_MINIONESE, minioneseSentence))
            }
        }
        
        return segments
    }
    
    private fun convertToMinionese(sentence: String): String {
        val keepList = setOf("calories", "protein", "carbs", "fat", "gram", "grams", "kcal", "g")
        val minionSyllables = listOf(
            "ba", "na", "po", "ka", "tu", "la", "lee", "lo",
            "bee", "do", "pa", "ta", "me", "ti", "mo", "ge"
        )
        
        val words = sentence.split(Regex("\\s+"))
        val result = StringBuilder()
        
        for (word in words) {
            val cleanWord = word.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
            
            val isNumber = cleanWord.matches(Regex("\\d+"))
            
            if (isNumber || keepList.contains(cleanWord)) {
                result.append(word).append(" ")
            } else if (cleanWord.isNotBlank()) {
                val syllableCount = (cleanWord.length / 2).coerceAtLeast(1).coerceAtMost(4)
                val newWord = StringBuilder()
                
                val hash = cleanWord.hashCode()
                for (i in 0 until syllableCount) {
                    val index = Math.abs(hash + i) % minionSyllables.size
                    newWord.append(minionSyllables[index])
                }
                
                val prefix = word.takeWhile { !it.isLetterOrDigit() }
                val suffix = word.takeLastWhile { !it.isLetterOrDigit() }
                
                // Capitalize first letter if the original word was capitalized
                val finalNewWord = if (word.firstOrNull()?.isUpperCase() == true) {
                    newWord.toString().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                } else {
                    newWord.toString()
                }
                
                result.append(prefix).append(finalNewWord).append(suffix).append(" ")
            }
        }
        
        return result.toString().trim()
    }

    private fun playNextSegment() {
        if (!isPlaying || segmentQueue.isEmpty()) {
            isPlaying = false
            return
        }

        val segment = segmentQueue.poll() ?: return

        when (segment.type) {
            SegmentType.AUDIO_CLIP -> {
                playMinionSound(segment.audioName ?: "hello") {
                    mainHandler.post { playNextSegment() }
                }
            }
            SegmentType.TTS_MINIONESE -> {
                tts?.setPitch(2.0f)
                tts?.setSpeechRate(1.40f)
                tts?.speak(segment.content, TextToSpeech.QUEUE_FLUSH, null, "minion_tts_${System.currentTimeMillis()}")
            }
        }
    }

    fun testSampleVoice(voiceStyle: String) {
        if (voiceStyle.equals("minion", ignoreCase = true)) {
            speak("Hello! Wow! Banana! Tulaliloo ti amo! I have 500 calories. Hehehe!", "minion")
        } else {
            speak("Hello! I am your AI nutrition assistant. Ready to log your meals.", "normal")
        }
    }

    fun stop() {
        try {
            isPlaying = false
            segmentQueue.clear()
            stopMediaPlayer()
            tts?.stop()
        } catch (e: Throwable) {
            Log.e("AssistantTts", "Error stopping TTS", e)
        }
    }

    private fun stopMediaPlayer() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
        } catch (e: Throwable) {
            Log.e("AssistantTts", "Error stopping MediaPlayer", e)
        }
    }

    fun shutdown() {
        try {
            stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (e: Throwable) {
            Log.e("AssistantTts", "Error shutting down TTS", e)
        }
    }

    private fun sanitizeTextForSpeech(input: String, isMinion: Boolean = false): String {
        var text = input
            .replace(Regex("```[\\s\\S]*?```"), "") // remove json code blocks
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1") // remove markdown bold
            .replace(Regex("[#*_•~`]"), "") // remove symbols
            .replace(Regex("\\s+"), " ")
            .trim()

        return text
    }
}
