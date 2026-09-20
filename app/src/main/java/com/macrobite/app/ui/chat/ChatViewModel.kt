package com.macrobite.app.ui.chat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.gson.Gson
import com.macrobite.app.notification.ChatVisibilityTracker
import com.macrobite.app.notification.NotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import com.google.gson.annotations.SerializedName
import com.macrobite.app.data.websearch.LiveWebSearchEngine
import com.macrobite.app.domain.model.AppDate
import com.macrobite.app.domain.model.ContactCandidate
import com.macrobite.app.domain.model.JarvisActionPayload
import com.macrobite.app.domain.model.JarvisActionType
import com.macrobite.app.domain.model.MealCategory
import com.macrobite.app.domain.model.MealEntry
import com.macrobite.app.domain.repository.ChatRepository
import com.macrobite.app.domain.repository.MealRepository
import com.macrobite.app.domain.repository.UserPreferencesRepository
import com.macrobite.app.ui.chat.jarvis.ContactResolver
import com.macrobite.app.ui.chat.jarvis.ContactSearchResult
import com.macrobite.app.ui.chat.jarvis.JarvisActionDispatcher
import com.macrobite.app.ui.chat.jarvis.JarvisDispatchResult
import com.macrobite.app.ui.chat.jarvis.LocalJarvisActionParser
import com.macrobite.app.ui.chat.voice.AssistantTtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class ChatFoodPayload(
    val foodName: String,
    val portion: String = "1 serving",
    val category: MealCategory = MealCategory.SNACKS,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fats: Float,
    val fiber: Float = 0f,
    val sugar: Float = 0f,
    val sodium: Float = 0f,
    val vitaminsAndMinerals: String = ""
)

data class FoodJsonExtraction(
    @SerializedName("has_food") val hasFood: Boolean? = false,
    @SerializedName("food_name") val foodName: String? = null,
    @SerializedName("portion") val portion: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("calories") val calories: Int? = 0,
    @SerializedName("protein") val protein: Float? = 0f,
    @SerializedName("carbs") val carbs: Float? = 0f,
    @SerializedName("fats") val fats: Float? = 0f,
    @SerializedName("fiber") val fiber: Float? = 0f,
    @SerializedName("sugar") val sugar: Float? = 0f,
    @SerializedName("sodium") val sodium: Float? = 0f,
    @SerializedName("vitamins") val vitamins: String? = null
)

data class JarvisActionExtraction(
    @SerializedName("has_action") val hasAction: Boolean? = false,
    @SerializedName("action_type") val actionType: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("contact_name") val contactName: String? = null,
    @SerializedName("phone_number") val phoneNumber: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("time_hour") val timeHour: Int? = null,
    @SerializedName("time_minute") val timeMinute: Int? = null,
    @SerializedName("timer_seconds") val timerSeconds: Int? = null,
    @SerializedName("query") val query: String? = null,
    @SerializedName("app_name") val appName: String? = null,
    @SerializedName("package_name") val packageName: String? = null,
    @SerializedName("turn_on") val turnOn: Boolean? = true,
    @SerializedName("auto_execute") val autoExecute: Boolean? = false
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val foodPayload: ChatFoodPayload? = null,
    val actionPayload: JarvisActionPayload? = null,
    val isLogged: Boolean = false,
    val imageUri: String? = null,
    val isWebSearch: Boolean = false
)

data class ChatModelOption(
    val id: String,
    val name: String
)

val availableChatModels = listOf(
    ChatModelOption("gemini-3.5-flash-lite", "Gemini 3.5 Flash-Lite"),
    ChatModelOption("gemini-3.1-flash-lite", "Gemini 3.1 Flash-Lite"),
    ChatModelOption("gemini-3.5-flash", "Gemini 3.5 Flash"),
    ChatModelOption("gemini-3.6-flash", "Gemini 3.6 Flash"),
    ChatModelOption("gemini-2.5-flash", "Gemini 2.5 Flash"),
    ChatModelOption("gemini-flash-lite-latest", "Gemini Flash-Lite Latest")
)

sealed class ChatUiEvent {
    data class ShowSnackbar(val message: String) : ChatUiEvent()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val mealRepository: MealRepository,
    private val chatRepository: ChatRepository,
    private val gson: Gson
) : ViewModel() {

    val raayaName: StateFlow<String> = userPreferencesRepository.getRaayaName()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "AI")

    val raayaAvatar: StateFlow<String> = userPreferencesRepository.getRaayaAvatar()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "default")

    val raayaPersonality: StateFlow<String> = userPreferencesRepository.getRaayaPersonality()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Friendly & Encouraging")

    val raayaAutoLog: StateFlow<Boolean> = userPreferencesRepository.getRaayaAutoLog()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val raayaIncludeMicros: StateFlow<Boolean> = userPreferencesRepository.getRaayaIncludeMicros()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val raayaDietaryNotes: StateFlow<String> = userPreferencesRepository.getRaayaDietaryNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val raayaWebSearchEnabled: StateFlow<Boolean> = userPreferencesRepository.getRaayaWebSearchEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val modelUsageStats: StateFlow<Map<String, Int>> = userPreferencesRepository.getModelUsageStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val geminiApiKey: StateFlow<String> = userPreferencesRepository.getGeminiApiKey()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun saveGeminiApiKey(key: String) {
        viewModelScope.launch {
            userPreferencesRepository.setGeminiApiKey(key)
        }
    }

    val voiceStyle: StateFlow<String> = userPreferencesRepository.getVoiceStyle()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "normal")

    val replyStyle: StateFlow<String> = userPreferencesRepository.getReplyStyle()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "normal")

    private val ttsManager = AssistantTtsManager(context)

    fun updateVoiceStyle(style: String) {
        viewModelScope.launch {
            userPreferencesRepository.setVoiceStyle(style)
        }
    }

    fun updateReplyStyle(style: String) {
        viewModelScope.launch {
            userPreferencesRepository.setReplyStyle(style)
        }
    }

    fun speak(text: String) {
        ttsManager.speak(text, voiceStyle.value)
    }

    fun testVoice(style: String) {
        ttsManager.testSampleVoice(style)
    }

    fun playMinionSound(soundName: String) {
        ttsManager.playMinionSound(soundName)
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    val initialGreeting: ChatMessage
        get() = ChatMessage(
            id = "initial-greeting",
            text = "Hey there! 👋 I'm **${raayaName.value}**, your personal AI & nutrition co-pilot.\n\nTell me what you ate today to calculate macros, or ask me to make phone calls, send WhatsApp/SMS messages, set alarms, start timers, search YouTube, and navigate anywhere!",
            isUser = false
        )

    private val _selectedChatDate = MutableStateFlow(AppDate.todayIso())
    val selectedChatDate: StateFlow<String> = _selectedChatDate.asStateFlow()

    val currentModel: StateFlow<String> = userPreferencesRepository.getGeminiModel()
        .map { stored ->
            if (stored == "gemini-3.7-flash" || stored.isBlank() || stored == "offline-local") "gemini-3.5-flash-lite" else stored
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "gemini-3.5-flash-lite")

    val customModels: StateFlow<List<String>> = userPreferencesRepository.getCustomModels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customBarcodes: StateFlow<Map<String, String>> = userPreferencesRepository.getCustomBarcodes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun registerCustomModel(model: String) {
        val clean = model.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            userPreferencesRepository.addCustomModel(clean)
            userPreferencesRepository.setGeminiModel(clean)
            _uiEvents.emit(ChatUiEvent.ShowSnackbar("Registered & activated model: $clean"))
        }
    }

    fun removeCustomModel(model: String) {
        val clean = model.trim()
        viewModelScope.launch {
            userPreferencesRepository.removeCustomModel(clean)
            if (currentModel.value == clean) {
                userPreferencesRepository.setGeminiModel("gemini-3.5-flash-lite")
            }
            _uiEvents.emit(ChatUiEvent.ShowSnackbar("Removed custom model: $clean"))
        }
    }

    // Distinct dates that have stored chat history, always ensuring Today is in the list
    val chatHistoryDates: StateFlow<List<String>> = chatRepository.getDistinctChatDates()
        .map { datesFromDb ->
            val today = AppDate.todayIso()
            val combined = (datesFromDb + today).distinct().sortedDescending()
            combined
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(AppDate.todayIso()))

    val messages: StateFlow<List<ChatMessage>> = combine(
        _selectedChatDate,
        raayaName
    ) { date, name ->
        date to name
    }.flatMapLatest { (date, currentName) ->
        chatRepository.getMessagesForDate(date).map { dbList ->
            if (dbList.isEmpty()) {
                if (date == AppDate.todayIso()) {
                    listOf(
                        ChatMessage(
                            id = "initial-greeting",
                            text = "Hey there! 👋 I'm **$currentName**.\n\nTell me what you ate today or describe any meal in detail (portions, ingredients, brand snacks) and I'll calculate your exact calories and macros so you can log them with a single tap!",
                            isUser = false
                        )
                    )
                } else {
                    emptyList()
                }
            } else {
                dbList.map { msg ->
                    if (!msg.isUser && msg.text.contains("Hey there! 👋 I'm **")) {
                        msg.copy(
                            text = "Hey there! 👋 I'm **$currentName**.\n\nTell me what you ate today or describe any meal in detail (portions, ingredients, brand snacks) and I'll calculate your exact calories and macros so you can log them with a single tap!"
                        )
                    } else {
                        msg
                    }
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _uiEvents = MutableSharedFlow<ChatUiEvent>()
    val uiEvents: SharedFlow<ChatUiEvent> = _uiEvents.asSharedFlow()

    private val candidateModels = listOf(
        "gemini-3.5-flash-lite",
        "gemini-3.1-flash-lite",
        "gemini-3.5-flash",
        "gemini-3.6-flash",
        "gemini-2.5-flash",
        "gemini-flash-lite-latest"
    )

    fun selectChatDate(date: String) {
        _selectedChatDate.value = date
    }

    fun newTodayChat() {
        _selectedChatDate.value = AppDate.todayIso()
    }

    fun setGeminiModel(model: String) {
        viewModelScope.launch {
            userPreferencesRepository.setGeminiModel(model)
            val friendlyName = availableChatModels.find { it.id == model }?.name ?: model
            _uiEvents.emit(ChatUiEvent.ShowSnackbar("Switched AI model to $friendlyName"))
        }
    }

    fun updateRaayaName(name: String) {
        viewModelScope.launch {
            val cleanName = name.trim().ifBlank { "Raaya" }
            userPreferencesRepository.setRaayaName(cleanName)

            // Update any persisted greeting message in Room DB for today so it doesn't stay stuck with old name
            try {
                val today = AppDate.todayIso()
                val existing = chatRepository.getMessagesForDate(today).first()
                val greetingMsg = existing.find { !it.isUser && it.text.contains("Hey there! 👋 I'm **") }
                if (greetingMsg != null) {
                    val updatedText = "Hey there! 👋 I'm **$cleanName**.\n\nTell me what you ate today or describe any meal in detail (portions, ingredients, brand snacks) and I'll calculate your exact calories and macros so you can log them with a single tap!"
                    chatRepository.saveMessage(today, greetingMsg.copy(text = updatedText))
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error updating greeting message in DB", e)
            }

            _uiEvents.emit(ChatUiEvent.ShowSnackbar("AI name updated to $cleanName"))
        }
    }

    fun updateRaayaAvatar(avatarId: String) {
        viewModelScope.launch {
            userPreferencesRepository.setRaayaAvatar(avatarId)
        }
    }

    fun updateRaayaPersonality(personality: String) {
        viewModelScope.launch {
            userPreferencesRepository.setRaayaPersonality(personality)
            _uiEvents.emit(ChatUiEvent.ShowSnackbar("Coaching tone updated"))
        }
    }

    fun updateRaayaAutoLog(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setRaayaAutoLog(enabled)
        }
    }

    fun updateRaayaIncludeMicros(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setRaayaIncludeMicros(enabled)
        }
    }

    fun updateRaayaDietaryNotes(notes: String) {
        viewModelScope.launch {
            userPreferencesRepository.setRaayaDietaryNotes(notes)
            _uiEvents.emit(ChatUiEvent.ShowSnackbar("Dietary context saved"))
        }
    }

    fun updateRaayaWebSearch(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setRaayaWebSearchEnabled(enabled)
            val msg = if (enabled) "Live Web Search Grounding enabled" else "Web Search disabled (Pre-trained model active)"
            _uiEvents.emit(ChatUiEvent.ShowSnackbar(msg))
        }
    }

    fun deleteChatForDate(date: String) {
        viewModelScope.launch {
            chatRepository.deleteChatForDate(date)
            if (_selectedChatDate.value == date) {
                _selectedChatDate.value = AppDate.todayIso()
            }
            _uiEvents.emit(ChatUiEvent.ShowSnackbar("Chat history deleted for $date"))
        }
    }

    fun clearCurrentChat() {
        viewModelScope.launch {
            chatRepository.deleteChatForDate(_selectedChatDate.value)
            _uiEvents.emit(ChatUiEvent.ShowSnackbar("Cleared chat for this day"))
        }
    }

    fun sendMessage(input: String, imageUri: String? = null, barcodeToSave: String? = null) {
        val trimmed = input.trim()
        if ((trimmed.isBlank() && imageUri == null) && barcodeToSave == null || _isGenerating.value) return
        val chatDate = _selectedChatDate.value

        val userMessage = ChatMessage(
            text = trimmed,
            isUser = true,
            imageUri = imageUri
        )

        viewModelScope.launch {
            val currentAiName = try {
                userPreferencesRepository.getRaayaName().first().trim().ifBlank { "Raaya" }
            } catch (e: Exception) {
                raayaName.value.trim().ifBlank { "Raaya" }
            }
            // If starting a fresh chat today, persist the greeting first so it stays at the top
            val existing = chatRepository.getMessagesForDate(chatDate).first()
            if (existing.isEmpty() && chatDate == AppDate.todayIso()) {
                val freshGreeting = ChatMessage(
                    id = "initial-greeting",
                    text = "Hey there! 👋 I'm **$currentAiName**.\n\nTell me what you ate today or describe any meal in detail (portions, ingredients, brand snacks) and I'll calculate your exact calories and macros so you can log them with a single tap!",
                    isUser = false,
                    timestamp = System.currentTimeMillis() - 1000
                )
                chatRepository.saveMessage(chatDate, freshGreeting)
            }

            // Save user message to Room DB
            chatRepository.saveMessage(chatDate, userMessage)

            _isGenerating.value = true
            try {
                processAiResponse(chatDate, trimmed, imageUri, barcodeToSave)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error in AI response", e)
                val errorMessage = ChatMessage(
                    text = "Sorry, I couldn't reach the nutrition server right now: ${e.localizedMessage ?: "Network error"}. Please check your connection or API key.",
                    isUser = false
                )
                chatRepository.saveMessage(chatDate, errorMessage)
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private suspend fun processAiResponse(
        chatDate: String,
        latestUserText: String,
        imageUri: String? = null,
        barcodeToSave: String? = null
    ) = withContext(Dispatchers.IO) {
        val currentAiName = try {
            userPreferencesRepository.getRaayaName().first().trim().ifBlank { "Raaya" }
        } catch (e: Exception) {
            raayaName.value.trim().ifBlank { "Raaya" }
        }

        // 1. Intercept Contact Alias Commands (e.g. "set alias dad = daddy", "my dad is daddy", "alias mom as mummy")
        val aliasCmd = LocalJarvisActionParser.parseAliasCommand(latestUserText)
        if (aliasCmd != null) {
            userPreferencesRepository.setContactAlias(aliasCmd.alias, aliasCmd.target)
            val confirmationMsg = ChatMessage(
                text = "✅ Saved contact alias! From now on, saying **\"call ${aliasCmd.alias}\"** will automatically call **${aliasCmd.target}**.",
                isUser = false
            )
            chatRepository.saveMessage(chatDate, confirmationMsg)
            return@withContext
        }

        // 2. Intercept Direct Jarvis Device Actions locally for instant direct execution (Call, Open App, YouTube, Maps, Flashlight, Timer, Alarm, etc.)
        val localAction = LocalJarvisActionParser.parse(latestUserText)
        if (localAction != null) {
            val handled = executeDirectJarvisAction(chatDate, localAction)
            if (handled) return@withContext
        }

        val useGemini = try { userPreferencesRepository.getUseGemini().first() } catch (e: Exception) { true }
        val apiKey = try { userPreferencesRepository.getGeminiApiKey().first().trim() } catch (e: Exception) { "" }
        if (!useGemini || apiKey.isBlank()) {
            if (localAction != null) {
                val responseMsg = when (localAction.actionType) {
                    JarvisActionType.BATTERY -> {
                        val res = JarvisActionDispatcher.executeBatteryCheck(context)
                        if (res is JarvisDispatchResult.Success) res.message else "Here is your battery status."
                    }
                    JarvisActionType.FLASHLIGHT -> "Right away! Flashlight control ready below."
                    JarvisActionType.CALL -> "Opening phone dialer for ${localAction.contactName ?: localAction.phoneNumber ?: "call"}."
                    JarvisActionType.WHATSAPP -> "Opening WhatsApp for ${localAction.contactName ?: "contact"}."
                    JarvisActionType.SMS -> "Drafting text message for ${localAction.contactName ?: "contact"}."
                    JarvisActionType.ALARM -> "Alarm ready for ${localAction.displayTitle}."
                    JarvisActionType.TIMER -> "Countdown timer ready below."
                    JarvisActionType.OPEN_APP -> "Opening ${localAction.appName ?: "app"} for you."
                    JarvisActionType.MAPS -> "Directions ready for ${localAction.query}."
                    JarvisActionType.YOUTUBE -> "Searching YouTube for ${localAction.query}."
                    JarvisActionType.NOTE -> "Here is your note."
                }
                val localMessage = ChatMessage(
                    text = responseMsg,
                    isUser = false,
                    actionPayload = localAction
                )
                chatRepository.saveMessage(chatDate, localMessage)
                return@withContext
            }

            val unconfiguredMsg = ChatMessage(
                text = "Gemini AI is currently not configured or disabled. Please set your Gemini API key in Settings to chat with $currentAiName and log meals.",
                isUser = false
            )
            chatRepository.saveMessage(chatDate, unconfiguredMsg)
            return@withContext
        }

        val preferredModel = try {
            userPreferencesRepository.getGeminiModel().first()
        } catch (e: Exception) {
            "gemini-3.5-flash-lite"
        }
        val safeModel = if (preferredModel == "gemini-3.7-flash" || preferredModel.isBlank() || preferredModel == "offline-local") {
            "gemini-3.5-flash-lite"
        } else {
            preferredModel
        }

        val orderedModels = if (candidateModels.contains(safeModel)) {
            listOf(safeModel) + candidateModels.filter { it != safeModel }
        } else {
            listOf(safeModel) + candidateModels
        }

        val currentList = messages.value
        val conversationHistory = currentList.takeLast(6).joinToString("\n") { msg ->
            val speaker = if (msg.isUser) "User" else currentAiName
            val sanitizedText = if (currentAiName.lowercase() != "raaya") {
                msg.text.replace("Raaya", currentAiName, ignoreCase = true)
            } else {
                msg.text
            }
            "$speaker: $sanitizedText"
        }

        val tone = raayaPersonality.value
        val dietaryNotes = raayaDietaryNotes.value
        val includeMicros = raayaIncludeMicros.value
        val autoLog = raayaAutoLog.value

        val personaInstruction = when (tone) {
            "Strict Fitness Coach" -> "You are a disciplined, high-standards athletic coach. Focus heavily on hitting high protein, adhering to caloric deficit/surplus, and calling out poor macro balance directly without fluff."
            "Detailed Clinical Nutritionist" -> "You are a biochemical clinical nutritionist. Provide precise micronutrient, electrolyte, glycemic index, and metabolic breakdown alongside standard macros."
            "Concise & Fast" -> "Be concise, rapid, and straight to the point. Provide food breakdowns with minimal chat and maximum clarity."
            else -> "You are friendly, motivating, enthusiastic, and scientifically rigorous."
        }

        val dietaryContextPrompt = if (dietaryNotes.isNotBlank()) {
            "\nUSER DIETARY CONTEXT & PREFERENCES: $dietaryNotes (Always respect these dietary preferences/restrictions when advising or logging)."
        } else ""

        val microsPrompt = if (includeMicros) {
            "- MICRONUTRIENTS: Always calculate and include fiber (g), sugar (g), sodium (mg), and notable vitamins/minerals."
        } else {
            "- Keep focus primarily on calories (kcal), protein (g), carbs (g), and fats (g)."
        }

        val loggingPrompt = if (autoLog) {
            """
            STRUCTURED FOOD LOGGING FORMAT:
            Whenever the user describes a meal or food item to calculate or log, provide your friendly advice AND output a single JSON block at the very end of your response enclosed strictly in ```json and ``` with this exact structure:
            ```json
            {
              "has_food": true,
              "food_name": "Food or Meal Title",
              "portion": "e.g. 20g pack or 3 rotis with 1 bowl dal",
              "category": "BREAKFAST | LUNCH | DINNER | SNACKS",
              "calories": 350,
              "protein": 18.5,
              "carbs": 42.0,
              "fats": 8.5,
              "fiber": 4.0,
              "sugar": 3.0,
              "sodium": 280.0,
              "vitamins": "Optional vitamins/minerals summary"
            }
            ```
            If no specific food or meal was discussed to log, do not include the JSON block.
            """.trimIndent()
        } else {
            """
            STRUCTURED FOOD LOGGING FORMAT:
            Only output a JSON food block if the user explicitly asks to log or record the food. Otherwise provide a conversational nutrition estimate.
            """.trimIndent()
        }

        val jarvisDeviceActionsPrompt = """
            DEVICE ACTIONS & ASSISTANT CONTROLS:
            You also act as the user's personal automated device assistant on their Android device.
            You can understand and orchestrate device tasks:
            1. Calls: "Call Mom", "Call 9876543210", "Phone Alex"
            2. WhatsApp: "Send WhatsApp to Rahul: On my way", "WhatsApp Dad"
            3. SMS / Text: "SMS Priya: I reached the gym", "Text Sam"
            4. Alarms: "Set alarm for 6:30 AM", "Wake me up at 7"
            5. Timers: "Set timer for 15 minutes", "Timer for 5 mins for boiling eggs"
            6. Open Apps: "Open YouTube", "Launch Spotify", "Open Settings", "Open Camera", "Open Chrome"
            7. Maps & Navigation: "Directions to nearest gym", "Navigate to Subway", "Find protein stores"
            8. YouTube Search: "Search chest workout on YouTube", "Play high protein meal prep"
            9. Flashlight / Torch: "Turn on flashlight", "Turn off torch"
            10. Battery Check: "Check battery status", "Battery percentage"
            11. Notes: "Note: Buy 2kg whey protein and peanut butter"

            Whenever the user asks for ANY device action or automation, provide an encouraging confirmation AND append an action JSON block enclosed strictly in ```json and ``` with this exact structure:
            ```json
            {
              "has_action": true,
              "action_type": "CALL | WHATSAPP | SMS | ALARM | TIMER | OPEN_APP | MAPS | YOUTUBE | FLASHLIGHT | BATTERY | NOTE",
              "title": "Action title",
              "contact_name": "Contact name if applicable",
              "phone_number": "Phone number if given or empty",
              "message": "Message text for WhatsApp or SMS",
              "time_hour": 6,
              "time_minute": 30,
              "timer_seconds": 900,
              "query": "Search query for YouTube or Maps",
              "app_name": "App name (e.g. YouTube, Spotify)",
              "package_name": "Package name if known",
              "turn_on": true,
              "auto_execute": false
            }
            ```
            If both a food item AND a device action are mentioned, you may output both blocks.
        """.trimIndent()

        val currentReplyStyle = replyStyle.value
        val minionPersonaPrompt = if (currentReplyStyle.equals("minion", ignoreCase = true)) {
            """
            MINIONS PERSONA & BANANA LANGUAGE (ACTIVE):
            - You are a loyal, adorable, hyperactive Minion from Despicable Me obsessed with food and bananas!
            - You communicate in Minion Language (Banana Language) mixed with English words so the user clearly understands their calories and macros!
            - Frequently and naturally use classic Minion expressions:
              * "Bello!" (Hello / greeting)
              * "Banana! 🍌" (Yum! Delicious food!)
              * "Poopaye!" (Goodbye / finish)
              * "Tulaliloo ti amo!" (Cheers / congratulations / great job!)
              * "Tank yu!" (Thank you!)
              * "Me want food!" / "Me want banana!"
              * "Bi-do bi-do!" (Alert / watch out!)
              * "Gelato!" / "Choko!" / "Papoy!"
            - Keep all nutritional facts, calorie numbers, and JSON blocks 100% scientifically accurate, but make your text explanation bursting with Minion excitement, cheers, and fun!
            """.trimIndent()
        } else ""

        val systemPrompt = """
            You are $currentAiName, an expert AI sports nutritionist, wellness coach, and meal assistant inside MacroBite.
            STRICT IDENTITY & NAME MANDATE:
            - Your official name is strictly and exclusively "$currentAiName".
            - You MUST NEVER call yourself "Raaya" or any other name under any circumstances.
            - If asked your name, who you are, or in greetings and sign-offs, ALWAYS introduce and refer to yourself strictly as "$currentAiName".
            $personaInstruction
            $dietaryContextPrompt
            
            GENERAL CONVERSATION & VERSATILITY:
            - While your specialty is sports nutrition and fitness, you are also knowledgeable, versatile, and happy to chat about everyday life, current events, movies, entertainment, facts, science, and general questions.
            - When answering general non-food questions (such as movies, daily topics, or trivia), converse naturally and helpfully. Do NOT force a food breakdown or JSON block unless a meal or food is actually discussed.
            
            SCIENTIFIC NUTRITIONAL ACCURACY GUIDELINES:
            - ACCURACY REFERENCE: Calculate macros and calories using verified reference nutritional databases (USDA FoodData Central and Indian Food Composition Tables / NIN).
            - ATWATER ENERGY FORMULA VERIFICATION: Calories must mathematically align with the macronutrient formula:
              Total Calories ≈ (Protein × 4) + (Carbs × 4) + (Fats × 9).
              Ensure your breakdown is internally consistent and mathematically sound.
            - STRICT USER QUANTITY OVERRIDE:
              * If the user specifies any quantity, weight, volume, or pack size (e.g. "20g pack", "70 grams", "3 rotis", "200ml", "1.5 scoops"), you MUST calculate macros proportionally for that EXACT specified portion. NEVER substitute or guess a different default weight.
            - STANDARD MEASUREMENTS:
              * 1 cup = 250ml.
              * 1 bowl / katori = 150ml.
              * 1 roti / chapati (medium whole wheat without ghee) = ~85 kcal, 3.0g protein, 17.5g carbs, 0.5g fat. With ghee = ~120 kcal, 3.0g protein, 17.5g carbs, 4.5g fat.
              * 1 whole boiled egg (~50g) = ~72 kcal, 6.3g protein, 0.4g carbs, 4.8g fat.
              * 1 scoop standard whey powder (~30g) = ~120-130 kcal, 24-25g protein, 2-3g carbs, 1.5-2g fat.
            - ZERO CALORIE ITEMS: Plain water, ice, plain black coffee, unsweetened green/black tea have 0 calories and 0 macros. Mixing powder in water adds 0 calories from the water.
            - PRECISION: Always use 1-decimal float precision for protein, carbs, fats, fiber, sugar, and saturated fat (e.g. 1.4g, 22.5g, 0.5g). Keep calories as whole numbers (kcal).
            $microsPrompt
            - CLEAN TEXT FORMATTING: Write clean, readable conversational text. DO NOT use triple asterisks (like ***calories***). Use simple bold headers and bullet points (•) where helpful.
            
            $loggingPrompt

            $jarvisDeviceActionsPrompt

            $minionPersonaPrompt
        """.trimIndent()

        val promptText = if (latestUserText.isNotBlank()) {
            latestUserText
        } else if (imageUri != null) {
            "Please analyze this attached meal photo, accurately identify all food items and portions, and calculate the exact calories, protein, carbs, fats and micronutrients so I can log it."
        } else {
            "Hello"
        }

        val fullPrompt = """
            $systemPrompt
            
            Recent conversation:
            $conversationHistory
            
            User's latest message:
            $promptText
        """.trimIndent()

        val uploadBitmap = imageUri?.let { path ->
            try {
                val f = File(path.removePrefix("file://"))
                if (f.exists()) {
                    BitmapFactory.decodeFile(f.absolutePath)?.let { scaleDownBitmap(it) }
                } else null
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to decode image from $path", e)
                null
            }
        }

        val isWebSearchRequested = try {
            userPreferencesRepository.getRaayaWebSearchEnabled().first()
        } catch (e: Exception) {
            false
        }

        var responseText: String? = null
        var wasWebSearchUsed = false
        var lastError: Exception? = null

        var promptToUse = fullPrompt

        // 1. If Live Web Search Grounding is enabled, execute real-time web search
        if (isWebSearchRequested) {
            try {
                val searchQuery = if (latestUserText.isNotBlank()) latestUserText else "latest nutrition and meals"
                val liveResults = LiveWebSearchEngine.search(searchQuery)
                if (liveResults.isNotEmpty()) {
                    val todayFormatted = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.US).format(Date())
                    val liveContext = buildString {
                        appendLine()
                        appendLine("CURRENT REAL-TIME DATE: $todayFormatted")
                        appendLine("LIVE REAL-TIME WEB SEARCH RESULTS (Retrieved from live internet just now):")
                        liveResults.forEachIndexed { idx, itm ->
                            val meta = listOfNotNull(
                                itm.snippet.takeIf { it.isNotBlank() },
                                itm.date.takeIf { it.isNotBlank() },
                                itm.source.takeIf { it.isNotBlank() }
                            ).joinToString(" | ")
                            appendLine("[${idx + 1}] ${itm.title}" + if (meta.isNotBlank()) " — $meta" else "")
                        }
                        appendLine()
                        appendLine("REAL-TIME GROUNDING MANDATE:")
                        appendLine("- The user has turned ON Live Web Search. You MUST synthesize your response using the real-time live web search results above.")
                        appendLine("- Reference specific titles, current dates, news, and live nutrition data from these web results.")
                        appendLine("- Do NOT say you only know from a local database or past training cutoff.")
                    }
                    promptToUse = "$fullPrompt\n\n$liveContext"
                    wasWebSearchUsed = true
                }
            } catch (e: Exception) {
                Log.w("ChatViewModel", "Live search retrieval failed", e)
            }
        }

        // 2. Generate content with Gemini using promptToUse
        for (modelName in orderedModels) {
            try {
                val model = GenerativeModel(
                    modelName = modelName,
                    apiKey = apiKey
                )
                val response = if (uploadBitmap != null) {
                    val inputContent = content {
                        image(uploadBitmap)
                        text(promptToUse)
                    }
                    model.generateContent(inputContent)
                } else {
                    model.generateContent(promptToUse)
                }
                val t = response.text
                if (!t.isNullOrBlank()) {
                    responseText = t
                    userPreferencesRepository.incrementModelUsage(modelName)
                    val promptTokens = (promptToUse.length / 4).coerceAtLeast(1) + (if (uploadBitmap != null) 258 else 0)
                    val candidateTokens = (t.length / 4).coerceAtLeast(1)
                    userPreferencesRepository.recordApiUsage(
                        promptTokens = promptTokens,
                        candidateTokens = candidateTokens,
                        totalTokens = promptTokens + candidateTokens
                    )
                    break
                }
            } catch (e: Exception) {
                lastError = e
            }
        }

        if (responseText.isNullOrBlank()) {
            throw lastError ?: IllegalStateException("Unable to generate response from Gemini.")
        }

        // Parse response and separate text from json using strict sanitization protocols
        val parsed = ChatResponseParser.parse(responseText, gson, isWebSearch = wasWebSearchUsed)
        val candidateMessage = if (parsed.actionPayload == null) {
            val inferred = LocalJarvisActionParser.parse(latestUserText)
            if (inferred != null) parsed.copy(actionPayload = inferred) else parsed
        } else {
            parsed
        }
        val finalMessage = when {
            candidateMessage.actionPayload?.actionType == JarvisActionType.CALL -> {
                resolveCallMessage(candidateMessage)
            }
            candidateMessage.actionPayload != null -> {
                autoExecuteDirectMessage(candidateMessage)
            }
            else -> candidateMessage
        }
        chatRepository.saveMessage(chatDate, finalMessage)

        // Save barcode if applicable
        if (barcodeToSave != null && finalMessage.foodPayload != null) {
            val fp = finalMessage.foodPayload
            val mealEntry = MealEntry(
                date = AppDate.todayIso(),
                category = fp.category,
                foodName = fp.foodName,
                portion = fp.portion,
                calories = fp.calories,
                protein = fp.protein,
                carbs = fp.carbs,
                fats = fp.fats,
                fiber = fp.fiber,
                sugar = fp.sugar,
                sodium = fp.sodium
            )
            val resultJson = com.google.gson.Gson().toJson(mealEntry)
            userPreferencesRepository.saveCustomBarcode(barcodeToSave, resultJson)
        }

        // If user navigated away or put app in background while AI was generating, send minimal notification
        if (!ChatVisibilityTracker.isChatScreenVisible) {
            try {
                val notifPrefs = userPreferencesRepository.getNotificationPreferences().firstOrNull()
                if (notifPrefs?.enabled == true && notifPrefs.chatResponseNotificationEnabled) {
                    val rawCleanText = parsed.text
                        .replace(Regex("""```[\s\S]*?```"""), "")
                        .replace(Regex("""[*#_~]"""), "")
                        .trim()
                    val snippet = if (rawCleanText.length > 90) {
                        rawCleanText.take(87) + "..."
                    } else if (rawCleanText.isNotBlank()) {
                        rawCleanText
                    } else {
                        "Your nutrition breakdown is ready. Tap to view."
                    }
                    NotificationHelper.showChatResponseNotification(
                        context = context,
                        aiName = currentAiName,
                        responseSnippet = snippet
                    )
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to trigger chat response notification", e)
            }
        }
    }

    private fun scaleDownBitmap(bitmap: Bitmap, maxDim: Int = 1024): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDim && height <= maxDim) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (ratio > 1f) {
            newWidth = maxDim
            newHeight = (maxDim / ratio).toInt()
        } else {
            newHeight = maxDim
            newWidth = (maxDim * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    fun logMealFromChat(messageId: String, payload: ChatFoodPayload) {
        viewModelScope.launch {
            try {
                val entry = MealEntry(
                    date = _selectedChatDate.value,
                    category = payload.category,
                    foodName = payload.foodName,
                    portion = payload.portion,
                    calories = payload.calories,
                    protein = payload.protein,
                    carbs = payload.carbs,
                    fats = payload.fats,
                    fiber = payload.fiber,
                    sugar = payload.sugar,
                    sodium = payload.sodium,
                    vitaminsAndMinerals = payload.vitaminsAndMinerals
                )
                mealRepository.insertMeal(entry)

                // Trigger food logged notification
                notifyFoodLogged(entry)

                // Mark message as logged persistently in database
                chatRepository.updateLoggedStatus(messageId, true)

                _uiEvents.emit(ChatUiEvent.ShowSnackbar("Logged \"${payload.foodName}\" to ${_selectedChatDate.value}! (+${payload.calories} kcal, +${payload.protein}g protein)"))
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to log meal from chat", e)
                _uiEvents.emit(ChatUiEvent.ShowSnackbar("Failed to log meal: ${e.localizedMessage}"))
            }
        }
    }

    private suspend fun notifyFoodLogged(entry: MealEntry) {
        try {
            val notifPrefs = userPreferencesRepository.getNotificationPreferences().firstOrNull()
            if (notifPrefs?.enabled == true && notifPrefs.foodLogNotificationEnabled) {
                NotificationHelper.showFoodLoggedNotification(context, entry)
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Failed to show food logged notification", e)
        }
    }

    private suspend fun executeDirectJarvisAction(
        chatDate: String,
        action: JarvisActionPayload
    ): Boolean {
        // Special case: Phone Call (uses contact resolution with aliases & disambiguation)
        if (action.actionType == JarvisActionType.CALL) {
            return executeCallAction(chatDate, action)
        }

        // Direct auto-execution for apps, youtube, maps, flashlight, timer, alarm, battery, etc.
        val payload = action.copy(autoExecute = true)
        val dispatchResult = JarvisActionDispatcher.dispatch(context, payload)
        val responseText = when (dispatchResult) {
            is JarvisDispatchResult.Success -> dispatchResult.message
            is JarvisDispatchResult.Info -> dispatchResult.message
            is JarvisDispatchResult.Error -> dispatchResult.message
        }

        val message = ChatMessage(
            text = responseText,
            isUser = false,
            actionPayload = payload
        )
        chatRepository.saveMessage(chatDate, message)
        return true
    }

    private suspend fun executeCallAction(
        chatDate: String,
        callAction: JarvisActionPayload
    ): Boolean {
        val rawTarget = callAction.contactName?.trim()
        val rawNumber = callAction.phoneNumber?.trim()

        if (!rawNumber.isNullOrBlank()) {
            val clean = rawNumber.replace(Regex("[^0-9+]"), "")
            val payload = callAction.copy(
                phoneNumber = clean,
                autoExecute = true
            )
            val res = JarvisActionDispatcher.dispatch(context, payload)
            val msgText = when (res) {
                is JarvisDispatchResult.Success -> res.message
                is JarvisDispatchResult.Info -> res.message
                is JarvisDispatchResult.Error -> res.message
            }
            val message = ChatMessage(
                text = msgText,
                isUser = false,
                actionPayload = payload
            )
            chatRepository.saveMessage(chatDate, message)
            return true
        }

        if (!rawTarget.isNullOrBlank()) {
            val aliases = try {
                userPreferencesRepository.getContactAliases().first()
            } catch (e: Exception) {
                emptyMap()
            }

            when (val searchResult = ContactResolver.searchContacts(context, rawTarget, aliases)) {
                is ContactSearchResult.Single -> {
                    val match = searchResult.contact
                    val payload = callAction.copy(
                        title = "Call ${match.displayName}",
                        contactName = match.displayName,
                        phoneNumber = match.phoneNumber,
                        autoExecute = true
                    )
                    val res = JarvisActionDispatcher.dispatch(context, payload)
                    val msgText = when (res) {
                        is JarvisDispatchResult.Success -> res.message
                        is JarvisDispatchResult.Info -> res.message
                        is JarvisDispatchResult.Error -> res.message
                    }
                    val message = ChatMessage(
                        text = msgText,
                        isUser = false,
                        actionPayload = payload
                    )
                    chatRepository.saveMessage(chatDate, message)
                    return true
                }
                is ContactSearchResult.Multiple -> {
                    val candidates = searchResult.contacts.map {
                        ContactCandidate(name = it.displayName, number = it.phoneNumber)
                    }
                    val payload = callAction.copy(
                        title = "Matches for \"$rawTarget\"",
                        contactName = rawTarget,
                        candidates = candidates,
                        autoExecute = false
                    )
                    val message = ChatMessage(
                        text = "I found ${candidates.size} contacts matching **\"$rawTarget\"**. Which one would you like to call?",
                        isUser = false,
                        actionPayload = payload
                    )
                    chatRepository.saveMessage(chatDate, message)
                    return true
                }
                is ContactSearchResult.None -> {
                    val payload = callAction.copy(
                        title = "Dial $rawTarget",
                        contactName = rawTarget,
                        autoExecute = false
                    )
                    val message = ChatMessage(
                        text = "I couldn't find **\"$rawTarget\"** in your contacts. You can open the phone dialer below, or set an alias (e.g. \"Set alias dad = daddy\") in Settings or chat.",
                        isUser = false,
                        actionPayload = payload
                    )
                    chatRepository.saveMessage(chatDate, message)
                    return true
                }
            }
        }
        return false
    }

    private suspend fun resolveCallMessage(message: ChatMessage): ChatMessage {
        val payload = message.actionPayload ?: return message
        val rawTarget = payload.contactName?.trim()
        val rawNumber = payload.phoneNumber?.trim()

        if (!rawNumber.isNullOrBlank()) {
            val clean = rawNumber.replace(Regex("[^0-9+]"), "")
            val resolvedPayload = payload.copy(phoneNumber = clean, autoExecute = true)
            JarvisActionDispatcher.dispatch(context, resolvedPayload)
            return message.copy(actionPayload = resolvedPayload)
        }

        if (!rawTarget.isNullOrBlank()) {
            val aliases = try {
                userPreferencesRepository.getContactAliases().first()
            } catch (e: Exception) {
                emptyMap()
            }
            when (val res = ContactResolver.searchContacts(context, rawTarget, aliases)) {
                is ContactSearchResult.Single -> {
                    val match = res.contact
                    val resolvedPayload = payload.copy(
                        title = "Call ${match.displayName}",
                        contactName = match.displayName,
                        phoneNumber = match.phoneNumber,
                        autoExecute = true
                    )
                    JarvisActionDispatcher.dispatch(context, resolvedPayload)
                    return message.copy(
                        text = "Calling **${match.displayName}** (${match.phoneNumber})...\n\n${message.text}",
                        actionPayload = resolvedPayload
                    )
                }
                is ContactSearchResult.Multiple -> {
                    val candidates = res.contacts.map {
                        ContactCandidate(name = it.displayName, number = it.phoneNumber)
                    }
                    val resolvedPayload = payload.copy(
                        title = "Matches for \"$rawTarget\"",
                        contactName = rawTarget,
                        candidates = candidates,
                        autoExecute = false
                    )
                    return message.copy(
                        text = "I found ${candidates.size} contacts matching **\"$rawTarget\"**. Which one would you like to call?\n\n${message.text}",
                        actionPayload = resolvedPayload
                    )
                }
                is ContactSearchResult.None -> {
                    val resolvedPayload = payload.copy(
                        title = "Dial $rawTarget",
                        contactName = rawTarget,
                        autoExecute = false
                    )
                    return message.copy(
                        text = "I couldn't find **\"$rawTarget\"** in your contacts. You can open the dialer below or add an alias.\n\n${message.text}",
                        actionPayload = resolvedPayload
                    )
                }
            }
        }
        return message
    }

    private suspend fun autoExecuteDirectMessage(message: ChatMessage): ChatMessage {
        val payload = message.actionPayload ?: return message
        val directPayload = payload.copy(autoExecute = true)
        val dispatchResult = JarvisActionDispatcher.dispatch(context, directPayload)
        val responseText = when (dispatchResult) {
            is JarvisDispatchResult.Success -> dispatchResult.message
            is JarvisDispatchResult.Info -> dispatchResult.message
            is JarvisDispatchResult.Error -> dispatchResult.message
        }
        val fullText = if (message.text.isNotBlank() && !message.text.contains(responseText)) {
            "${message.text}\n\n$responseText"
        } else {
            responseText
        }
        return message.copy(
            text = fullText,
            actionPayload = directPayload
        )
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}

object ChatResponseParser {
    private val citationRegex = Regex("""\[\s*(?:cite:)?\s*\d+(?:\s*[,-]\s*\d+)*\s*\]|【[^】]+】|\[\^\d+\]""")
    private val numericCitationFixRegex = Regex("""(:\s*\d+(?:\.\d+)?)\s*\[[^\]]+\]""")
    private val prefixNumericCitationFixRegex = Regex("""(:\s*)\[[^\]]+\]\s*(\d+(?:\.\d+)?)""")
    private val trailingCommaCitationRegex = Regex("""(\d+(?:\.\d+)?)\s*\[[^\]]+\]\s*(,?)""")

    fun stripCitations(input: String): String {
        return input
            .replace(citationRegex, "")
            .replace(Regex("""\s{2,}"""), " ")
            .trim()
    }

    fun sanitizeJsonBlock(rawJson: String): String {
        var cleaned = rawJson
        // 1. Fix numeric fields with attached citations like "calories": 450 [1], -> "calories": 450,
        cleaned = numericCitationFixRegex.replace(cleaned, "$1")
        cleaned = prefixNumericCitationFixRegex.replace(cleaned, "$1$2")
        cleaned = trailingCommaCitationRegex.replace(cleaned, "$1$2")
        // 2. Remove inline citation markers anywhere within strings/keys/values
        cleaned = citationRegex.replace(cleaned, "")
        return cleaned
    }

    fun sanitizeConversationalText(text: String): String {
        return text
            // Strip raw system/source citations like 【1†source】 or [cite: 1]
            .replace(Regex("""【[^】]+】"""), "")
            .replace(Regex("""\[cite:\s*\d+(?:\s*[,-]\s*\d+)*\]"""), "")
            // For standard bracket citations [1], [2], clean up extra spaces around punctuation:
            .replace(Regex("""\s*\[\s*\d+(?:\s*[,-]\s*\d+)*\s*\]\s*([.,!?:;])"""), "$1")
            // Clean up multiple spaces
            .replace(Regex("""[ \t]{2,}"""), " ")
            .trim()
    }

    fun extractFoodPayloadFallback(sanitizedJson: String): ChatFoodPayload? {
        return try {
            val nameMatch = Regex(""""food_name"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE).find(sanitizedJson)
            val calMatch = Regex(""""calories"\s*:\s*(\d+)""", RegexOption.IGNORE_CASE).find(sanitizedJson)
            if (nameMatch != null && calMatch != null) {
                val foodName = stripCitations(nameMatch.groupValues[1])
                val calories = calMatch.groupValues[1].toIntOrNull() ?: 0
                val portionMatch = Regex(""""portion"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE).find(sanitizedJson)
                val catMatch = Regex(""""category"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE).find(sanitizedJson)
                val protMatch = Regex(""""protein"\s*:\s*([\d.]+)""", RegexOption.IGNORE_CASE).find(sanitizedJson)
                val carbsMatch = Regex(""""carbs"\s*:\s*([\d.]+)""", RegexOption.IGNORE_CASE).find(sanitizedJson)
                val fatsMatch = Regex(""""fats"\s*:\s*([\d.]+)""", RegexOption.IGNORE_CASE).find(sanitizedJson)
                val fiberMatch = Regex(""""fiber"\s*:\s*([\d.]+)""", RegexOption.IGNORE_CASE).find(sanitizedJson)
                val sugarMatch = Regex(""""sugar"\s*:\s*([\d.]+)""", RegexOption.IGNORE_CASE).find(sanitizedJson)
                val sodiumMatch = Regex(""""sodium"\s*:\s*([\d.]+)""", RegexOption.IGNORE_CASE).find(sanitizedJson)
                val vitMatch = Regex(""""vitamins"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE).find(sanitizedJson)

                val portion = portionMatch?.let { stripCitations(it.groupValues[1]) } ?: "1 serving"
                val rawCat = catMatch?.let { stripCitations(it.groupValues[1]) } ?: "SNACKS"
                val category = MealCategory.fromString(rawCat)
                val protein = protMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
                val carbs = carbsMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
                val fats = fatsMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
                val fiber = fiberMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
                val sugar = sugarMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
                val sodium = sodiumMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
                val vitamins = vitMatch?.let { stripCitations(it.groupValues[1]) } ?: ""

                ChatFoodPayload(
                    foodName = foodName,
                    portion = portion,
                    category = category,
                    calories = calories.coerceAtLeast(0),
                    protein = protein.coerceAtLeast(0f),
                    carbs = carbs.coerceAtLeast(0f),
                    fats = fats.coerceAtLeast(0f),
                    fiber = fiber.coerceAtLeast(0f),
                    sugar = sugar.coerceAtLeast(0f),
                    sodium = sodium.coerceAtLeast(0f),
                    vitaminsAndMinerals = vitamins
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun extractActionPayloadFallback(sanitizedJson: String): JarvisActionPayload? {
        return try {
            val typeMatch = Regex(""""action_type"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE).find(sanitizedJson)
            if (typeMatch != null) {
                val actType = JarvisActionType.fromString(typeMatch.groupValues[1])
                val titleMatch = Regex(""""title"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE).find(sanitizedJson)?.groupValues?.get(1)
                val contactMatch = Regex(""""contact_name"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE).find(sanitizedJson)?.groupValues?.get(1)
                val phoneMatch = Regex(""""phone_number"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE).find(sanitizedJson)?.groupValues?.get(1)
                val msgMatch = Regex(""""message"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE).find(sanitizedJson)?.groupValues?.get(1)
                val qMatch = Regex(""""query"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE).find(sanitizedJson)?.groupValues?.get(1)
                val appMatch = Regex(""""app_name"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE).find(sanitizedJson)?.groupValues?.get(1)
                val pkgMatch = Regex(""""package_name"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE).find(sanitizedJson)?.groupValues?.get(1)
                val timerMatch = Regex(""""timer_seconds"\s*:\s*(\d+)""", RegexOption.IGNORE_CASE).find(sanitizedJson)?.groupValues?.get(1)?.toIntOrNull()
                val hourMatch = Regex(""""time_hour"\s*:\s*(\d+)""", RegexOption.IGNORE_CASE).find(sanitizedJson)?.groupValues?.get(1)?.toIntOrNull()
                val minMatch = Regex(""""time_minute"\s*:\s*(\d+)""", RegexOption.IGNORE_CASE).find(sanitizedJson)?.groupValues?.get(1)?.toIntOrNull()

                JarvisActionPayload(
                    actionType = actType,
                    title = titleMatch?.let { stripCitations(it) },
                    contactName = contactMatch?.let { stripCitations(it) },
                    phoneNumber = phoneMatch?.let { stripCitations(it) },
                    message = msgMatch?.let { stripCitations(it) },
                    query = qMatch?.let { stripCitations(it) },
                    appName = appMatch?.let { stripCitations(it) },
                    packageName = pkgMatch?.let { stripCitations(it) },
                    timerSeconds = timerMatch,
                    timeHour = hourMatch,
                    timeMinute = minMatch
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun parse(raw: String, gson: Gson, isWebSearch: Boolean): ChatMessage {
        val jsonRegex = Regex("""```(?:json)?\s*(\{[\s\S]*?\})\s*```""", RegexOption.IGNORE_CASE)
        val matches = jsonRegex.findAll(raw).toList()

        var rawWithoutJson = raw
        var foodPayload: ChatFoodPayload? = null
        var actionPayload: JarvisActionPayload? = null

        for (match in matches) {
            rawWithoutJson = rawWithoutJson.replace(match.value, "")
            val jsonString = match.groupValues[1]
            val cleanJsonString = sanitizeJsonBlock(jsonString)

            // 1. Try food payload extraction
            if (foodPayload == null && cleanJsonString.contains("has_food", ignoreCase = true)) {
                try {
                    val dto = gson.fromJson(cleanJsonString, FoodJsonExtraction::class.java)
                    if (dto != null && dto.hasFood == true && !dto.foodName.isNullOrBlank()) {
                        val cat = MealCategory.fromString(stripCitations(dto.category ?: "SNACKS"))
                        foodPayload = ChatFoodPayload(
                            foodName = stripCitations(dto.foodName),
                            portion = stripCitations(dto.portion ?: "1 serving"),
                            category = cat,
                            calories = (dto.calories ?: 0).coerceAtLeast(0),
                            protein = (dto.protein ?: 0f).coerceAtLeast(0f),
                            carbs = (dto.carbs ?: 0f).coerceAtLeast(0f),
                            fats = (dto.fats ?: 0f).coerceAtLeast(0f),
                            fiber = (dto.fiber ?: 0f).coerceAtLeast(0f),
                            sugar = (dto.sugar ?: 0f).coerceAtLeast(0f),
                            sodium = (dto.sodium ?: 0f).coerceAtLeast(0f),
                            vitaminsAndMinerals = stripCitations(dto.vitamins ?: "")
                        )
                    }
                } catch (e: Exception) {
                    foodPayload = extractFoodPayloadFallback(cleanJsonString)
                }
                if (foodPayload == null) {
                    foodPayload = extractFoodPayloadFallback(cleanJsonString)
                }
            }

            // 2. Try action payload extraction
            if (actionPayload == null && (cleanJsonString.contains("has_action", ignoreCase = true) || cleanJsonString.contains("action_type", ignoreCase = true))) {
                try {
                    val actionDto = gson.fromJson(cleanJsonString, JarvisActionExtraction::class.java)
                    if (actionDto != null && (actionDto.hasAction == true || !actionDto.actionType.isNullOrBlank())) {
                        val actType = JarvisActionType.fromString(actionDto.actionType)
                        actionPayload = JarvisActionPayload(
                            actionType = actType,
                            title = actionDto.title?.let { stripCitations(it) },
                            contactName = actionDto.contactName?.let { stripCitations(it) },
                            phoneNumber = actionDto.phoneNumber?.let { stripCitations(it) },
                            message = actionDto.message?.let { stripCitations(it) },
                            timeHour = actionDto.timeHour,
                            timeMinute = actionDto.timeMinute,
                            timerSeconds = actionDto.timerSeconds,
                            query = actionDto.query?.let { stripCitations(it) },
                            appName = actionDto.appName?.let { stripCitations(it) },
                            packageName = actionDto.packageName?.let { stripCitations(it) },
                            turnOn = actionDto.turnOn ?: true,
                            autoExecute = actionDto.autoExecute ?: false
                        )
                    }
                } catch (e: Exception) {
                    actionPayload = extractActionPayloadFallback(cleanJsonString)
                }
                if (actionPayload == null) {
                    actionPayload = extractActionPayloadFallback(cleanJsonString)
                }
            }
        }

        val cleanText = sanitizeConversationalText(rawWithoutJson.trim())

        val fallbackText = when {
            foodPayload != null -> "Here is the nutrition breakdown for your meal:"
            actionPayload != null -> "I've set up this action for you:"
            else -> sanitizeConversationalText(raw)
        }

        return ChatMessage(
            text = cleanText.ifBlank { fallbackText },
            isUser = false,
            foodPayload = foodPayload,
            actionPayload = actionPayload,
            isWebSearch = isWebSearch
        )
    }
}
