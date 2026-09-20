package com.macrobite.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.RemoteViews
import androidx.activity.ComponentActivity
import com.macrobite.app.R
import com.macrobite.app.domain.model.AppDate
import com.macrobite.app.domain.model.MealCategory
import com.macrobite.app.domain.model.MealEntry
import com.macrobite.app.domain.repository.ChatRepository
import com.macrobite.app.domain.repository.MealRepository
import com.macrobite.app.domain.usecase.ParseFoodUseCase
import com.macrobite.app.ui.chat.ChatFoodPayload
import com.macrobite.app.ui.chat.ChatMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WidgetVoiceActivity : ComponentActivity() {

    @Inject
    lateinit var parseFoodUseCase: ParseFoodUseCase

    @Inject
    lateinit var mealRepository: MealRepository

    @Inject
    lateinit var chatRepository: ChatRepository

    @Inject
    lateinit var userPreferencesRepository: com.macrobite.app.domain.repository.UserPreferencesRepository

    private var speechRecognizer: SpeechRecognizer? = null
    private var breathingJob: Job? = null
    private var isListening = true

    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(0, 0)
        super.onCreate(savedInstanceState)

        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

        val emptyView = View(this).apply {
            layoutParams = ViewGroup.LayoutParams(0, 0)
        }
        setContentView(emptyView)

        val prefs = getSharedPreferences("macrobite_widget_prefs", Context.MODE_PRIVATE)
        val themeId = prefs.getString("theme_color", "amber") ?: "amber"
        val themeColor = MacroBiteChatWidgetProvider.getThemeColorInt(themeId)

        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        val componentName = ComponentName(applicationContext, MacroBiteChatWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

        // 1. Initial widget state: "Listening..."
        if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
            val rv = RemoteViews(packageName, R.layout.widget_chat_bar)
            rv.setTextViewText(R.id.widget_placeholder_text, "Listening...")
            appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, rv)
        }

        // 2. Start breathing animation on the mic button in the widget
        breathingJob = CoroutineScope(Dispatchers.Main).launch {
            // Breathing color shifts between theme accent and glowing bright white / subtle alpha
            val breatheColors = listOf(
                themeColor,
                Color.WHITE,
                Color.parseColor("#E0E7FF"),
                themeColor,
                Color.parseColor("#9CA3AF")
            )
            var idx = 0
            while (isActive && isListening) {
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    val rv = RemoteViews(packageName, R.layout.widget_chat_bar)
                    rv.setInt(R.id.widget_mic_button, "setColorFilter", breatheColors[idx % breatheColors.size])
                    appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, rv)
                }
                idx++
                delay(300)
            }
        }

        // 3. Initialize Speech Recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                stopListeningAndReset(themeColor)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partialText = matches?.firstOrNull()
                if (!partialText.isNullOrBlank() && appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    val rv = RemoteViews(packageName, R.layout.widget_chat_bar)
                    rv.setTextViewText(R.id.widget_placeholder_text, partialText)
                    appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, rv)
                }
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                breathingJob?.cancel()

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.firstOrNull()?.trim() ?: ""

                if (spokenText.isNotBlank()) {
                    // Update widget with spoken text immediately
                    if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                        val rv = RemoteViews(packageName, R.layout.widget_chat_bar)
                        rv.setTextViewText(R.id.widget_placeholder_text, "$spokenText...")
                        rv.setInt(R.id.widget_mic_button, "setColorFilter", themeColor)
                        appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, rv)
                    }

                    // Finish activity with zero visual flashes
                    finish()
                    overridePendingTransition(0, 0)

                    // Unified execution: handles Assistant Tasks, Device Automation, and Food Logging!
                    val appContext = applicationContext
                    CoroutineScope(Dispatchers.IO).launch {
                        WidgetActionEngine.processInput(
                            context = appContext,
                            rawInput = spokenText,
                            parseFoodUseCase = parseFoodUseCase,
                            mealRepository = mealRepository,
                            chatRepository = chatRepository,
                            userPreferencesRepository = userPreferencesRepository
                        )
                    }
                } else {
                    stopListeningAndReset(themeColor)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            stopListeningAndReset(themeColor)
        }
    }

    private fun stopListeningAndReset(themeColor: Int) {
        isListening = false
        breathingJob?.cancel()
        val appContext = applicationContext
        MacroBiteChatWidgetProvider.updateAllWidgets(appContext)
        finish()
        overridePendingTransition(0, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        isListening = false
        breathingJob?.cancel()
        speechRecognizer?.destroy()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }
}
