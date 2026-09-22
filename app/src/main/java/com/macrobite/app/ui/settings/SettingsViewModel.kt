package com.macrobite.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.macrobite.app.domain.model.AppDate
import com.macrobite.app.domain.model.DailyApiUsage
import com.macrobite.app.domain.model.UserTargets
import com.macrobite.app.domain.model.WeightLog
import com.macrobite.app.domain.repository.UserPreferencesRepository
import com.macrobite.app.domain.repository.WeightRepository
import android.content.Context
import com.macrobite.app.domain.model.NotificationPreferences
import com.macrobite.app.domain.model.GoogleHealthStatus
import com.macrobite.app.health.GoogleHealthManager
import com.macrobite.app.notification.NotificationHelper
import com.macrobite.app.notification.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.macrobite.app.notification.AiWellWisherCoach
import javax.inject.Inject
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

data class SettingsUiState(
    val targets: UserTargets = UserTargets(),
    val useGemini: Boolean = true,
    val geminiApiKey: String = "",
    val darkModePreference: String = "dark",
    val weightLogs: List<WeightLog> = emptyList(),
    val isTestingApi: Boolean = false,
    val apiTestMessage: String? = null,
    val saveMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val weightRepository: WeightRepository,
    private val googleHealthManager: GoogleHealthManager,
    private val aiWellWisherCoach: AiWellWisherCoach,
    private val backupManager: com.macrobite.app.data.backup.BackupManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val targets: StateFlow<UserTargets> = userPreferencesRepository.getTargets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserTargets())

    val stepGoal: StateFlow<Int> = userPreferencesRepository.getStepGoal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10_000)

    val googleHealthSyncEnabled: StateFlow<Boolean> = userPreferencesRepository.getGoogleHealthSyncEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _googleHealthHasPermissions = MutableStateFlow(false)
    val googleHealthHasPermissions: StateFlow<Boolean> = _googleHealthHasPermissions.asStateFlow()

    init {
        checkGoogleHealthPermissions()
    }

    fun checkGoogleHealthPermissions() {
        viewModelScope.launch {
            _googleHealthHasPermissions.value = googleHealthManager.hasPermissions(context)
        }
    }

    fun setStepGoal(goal: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setStepGoal(goal)
        }
    }

    fun setGoogleHealthSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setGoogleHealthSyncEnabled(enabled)
        }
    }

    fun getGoogleHealthStatus(): GoogleHealthStatus {
        return googleHealthManager.getSdkStatus(context)
    }

    fun openHealthConnectSettings() {
        googleHealthManager.openHealthConnectSettings(context)
    }

    fun openHealthConnectPlayStore() {
        googleHealthManager.openPlayStoreForHealthConnect(context)
    }

    val notificationPreferences: StateFlow<NotificationPreferences> = userPreferencesRepository.getNotificationPreferences()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotificationPreferences())

    val useGemini: StateFlow<Boolean> = userPreferencesRepository.getUseGemini()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val customApiBaseUrl: StateFlow<String> = userPreferencesRepository.getCustomApiBaseUrl()
        .stateIn(viewModelScope, SharingStarted.Lazily, "")
    val customApiModel: StateFlow<String> = userPreferencesRepository.getCustomApiModel()
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    val geminiApiKey: StateFlow<String> = userPreferencesRepository.getGeminiApiKey()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val darkModePreference: StateFlow<String> = userPreferencesRepository.getDarkModePreference()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "dark")

    val weightLogs: StateFlow<List<WeightLog>> = weightRepository.getAllWeights()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyApiUsage: StateFlow<DailyApiUsage> = userPreferencesRepository.getDailyApiUsage()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyApiUsage(date = AppDate.todayIso()))

    val geminiModel: StateFlow<String> = userPreferencesRepository.getGeminiModel()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "gemini-3.5-flash-lite")

    val themeColorPreference: StateFlow<String> = userPreferencesRepository.getThemeColor()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "amber")

    val contactAliases: StateFlow<Map<String, String>> = userPreferencesRepository.getContactAliases()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val autoBackupEnabled: StateFlow<Boolean> = userPreferencesRepository.getAutoBackupEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setAutoBackupEnabled(enabled)
            _saveMessage.value = if (enabled) "Auto-backup enabled" else "Auto-backup disabled"
        }
    }

    fun setContactAlias(alias: String, target: String) {
        viewModelScope.launch {
            userPreferencesRepository.setContactAlias(alias, target)
        }
    }

    fun removeContactAlias(alias: String) {
        viewModelScope.launch {
            userPreferencesRepository.removeContactAlias(alias)
        }
    }

    private val _isTestingApi = MutableStateFlow(false)
    val isTestingApi: StateFlow<Boolean> = _isTestingApi.asStateFlow()

    private val _apiTestMessage = MutableStateFlow<String?>(null)
    val apiTestMessage: StateFlow<String?> = _apiTestMessage.asStateFlow()

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()

    data class LiveQuotaStatus(
        val isProbing: Boolean = false,
        val isApiAlive: Boolean? = null,
        val lastProbeTime: String? = null,
        val errorMessage: String? = null,
        val isRateLimited: Boolean = false
    )

    private val _liveQuotaStatus = MutableStateFlow(LiveQuotaStatus())
    val liveQuotaStatus: StateFlow<LiveQuotaStatus> = _liveQuotaStatus.asStateFlow()

    fun refreshLiveQuotaStatus() {
        viewModelScope.launch {
            val apiKey = geminiApiKey.value.trim()
            if (apiKey.isBlank()) {
                _liveQuotaStatus.value = LiveQuotaStatus(
                    isProbing = false,
                    isApiAlive = null,
                    errorMessage = "No API key configured"
                )
                return@launch
            }

            _liveQuotaStatus.value = _liveQuotaStatus.value.copy(isProbing = true, errorMessage = null)

            try {
                val currentModel = geminiModel.value.ifBlank { "gemini-3.5-flash-lite" }
                val model = GenerativeModel(
                    modelName = currentModel,
                    apiKey = apiKey
                )
                // countTokens does NOT count against RPD quota
                val tokenResponse = model.countTokens(content { text("test") })
                val now = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                _liveQuotaStatus.value = LiveQuotaStatus(
                    isProbing = false,
                    isApiAlive = true,
                    lastProbeTime = now,
                    isRateLimited = false
                )
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: e.message ?: "Unknown error"
                val isRateLimited = msg.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                        msg.contains("429", ignoreCase = true) ||
                        msg.contains("quota", ignoreCase = true) ||
                        msg.contains("rate limit", ignoreCase = true)
                val now = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                _liveQuotaStatus.value = LiveQuotaStatus(
                    isProbing = false,
                    isApiAlive = !isRateLimited,
                    lastProbeTime = now,
                    isRateLimited = isRateLimited,
                    errorMessage = if (isRateLimited) "Daily quota exhausted (429)" else msg
                )
            }
        }
    }

    fun saveTargets(calories: Int, protein: Int, carbs: Int, fats: Int) {
        viewModelScope.launch {
            val newTargets = UserTargets(
                calories = calories.coerceAtLeast(0),
                protein = protein.coerceAtLeast(0),
                carbs = carbs.coerceAtLeast(0),
                fats = fats.coerceAtLeast(0)
            )
            userPreferencesRepository.saveTargets(newTargets)
            _saveMessage.value = "Targets updated successfully!"
        }
    }

    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus: StateFlow<String?> = _backupStatus.asStateFlow()

    fun saveLocalBackup() {
        viewModelScope.launch {
            _backupStatus.value = "Creating local backup..."
            val (success, message) = backupManager.saveBackupToLocalStorage()
            _backupStatus.value = message
            _saveMessage.value = message
        }
    }

    suspend fun getGoogleDriveUploadIntent(): android.content.Intent {
        return backupManager.getGoogleDriveUploadIntent()
    }

    fun restoreBackup(jsonString: String) {
        viewModelScope.launch {
            _backupStatus.value = "Restoring data..."
            val result = backupManager.restoreBackup(jsonString)
            _backupStatus.value = result.message
            _saveMessage.value = result.message
        }
    }

    fun restoreLatestBackupAuto() {
        viewModelScope.launch {
            _backupStatus.value = "Scanning phone storage for latest backup..."
            val result = backupManager.restoreLatestBackupAuto()
            _backupStatus.value = result.message
            _saveMessage.value = result.message
        }
    }

    fun setUseGemini(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setUseGemini(enabled)
        }
    }

    fun setCustomApiBaseUrl(url: String) {
        viewModelScope.launch {
            userPreferencesRepository.setCustomApiBaseUrl(url)
        }
    }

    fun setCustomApiModel(model: String) {
        viewModelScope.launch {
            userPreferencesRepository.setCustomApiModel(model)
        }
    }

    fun saveGeminiApiKey(key: String) {
        viewModelScope.launch {
            userPreferencesRepository.setGeminiApiKey(key)
            _saveMessage.value = "Gemini API key saved!"
        }
    }

    fun testGeminiConnection(key: String, customBaseUrl: String, customModel: String) {
        val apiKeyToTest = key.trim()
        if (apiKeyToTest.isBlank()) {
            _apiTestMessage.value = "Please enter an API key to test."
            return
        }

        viewModelScope.launch {
            _isTestingApi.value = true
            _apiTestMessage.value = null

            if (customBaseUrl.isNotBlank()) {
                val rawUrl = customBaseUrl.trim()
                val fullUrl = if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
                    "http://$rawUrl"
                } else {
                    rawUrl
                }
                val cleanBase = fullUrl.removeSuffix("/")
                val endpoint = if (cleanBase.endsWith("/chat/completions")) {
                    cleanBase
                } else if (cleanBase.endsWith("/v1")) {
                    "$cleanBase/chat/completions"
                } else {
                    "$cleanBase/v1/chat/completions"
                }
                val targetModel = customModel.ifBlank { "Gemini 3.8 Flash (Low)" }
                try {
                    val requestUrl = endpoint.toHttpUrlOrNull()
                    if (requestUrl == null) throw IllegalArgumentException("Invalid URL: $endpoint")
                    val url = requestUrl.toUrl()
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Authorization", "Bearer $apiKeyToTest")
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true
                    conn.doInput = true

                    val jsonPayload = com.google.gson.JsonObject()
                    jsonPayload.addProperty("model", targetModel)
                    val messagesArray = com.google.gson.JsonArray()
                    val messageObj = com.google.gson.JsonObject()
                    messageObj.addProperty("role", "user")
                    messageObj.addProperty("content", "ping")
                    messagesArray.add(messageObj)
                    jsonPayload.add("messages", messagesArray)

                    val payloadString = jsonPayload.toString()
                    conn.outputStream.use { os ->
                        val input = payloadString.toByteArray(Charsets.UTF_8)
                        os.write(input, 0, input.size)
                    }

                    val responseCode = conn.responseCode
                    if (responseCode in 200..299) {
                        _apiTestMessage.value = "Connection successful! Custom model is active."
                        userPreferencesRepository.setGeminiApiKey(apiKeyToTest)
                        userPreferencesRepository.setUseGemini(true)
                    } else {
                        val errorStr = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                        _apiTestMessage.value = "Custom API Test failed ($responseCode): $errorStr"
                    }
                } catch (e: Exception) {
                    _apiTestMessage.value = "Custom API Test failed: ${e.localizedMessage ?: e.message}"
                }
                _isTestingApi.value = false
                return@launch
            }

            val rawCurrent = geminiModel.value
            val current = if (rawCurrent == "gemini-3.7-flash" || rawCurrent.isBlank()) "gemini-3.5-flash-lite" else rawCurrent
            val baseModels = listOf(
                "gemini-3.5-flash-lite",
                "gemini-3.1-flash-lite",
                "gemini-flash-lite-latest",
                "gemini-3.5-flash",
                "gemini-3.6-flash"
            )
            val candidateModels = if (baseModels.contains(current)) {
                listOf(current) + baseModels.filter { it != current }
            } else {
                listOf(current) + baseModels
            }
            var succeeded = false
            var activeModel = ""
            var lastError = ""

            for (modelName in candidateModels) {
                try {
                    val model = GenerativeModel(
                        modelName = modelName,
                        apiKey = apiKeyToTest
                    )
                    val response = model.generateContent("Reply with 'OK' if you can read this.")
                    val text = response.text ?: ""
                    if (text.isNotBlank()) {
                        succeeded = true
                        activeModel = modelName
                        break
                    }
                } catch (e: Exception) {
                    lastError = e.localizedMessage ?: e.message ?: "Unknown error"
                }
            }

            if (succeeded) {
                _apiTestMessage.value = "Connection successful! Model $activeModel is active."
                userPreferencesRepository.setGeminiApiKey(apiKeyToTest)
                userPreferencesRepository.setUseGemini(true)
                // Record test call: approx 12 prompt tokens, 4 candidate tokens
                userPreferencesRepository.recordApiUsage(promptTokens = 12, candidateTokens = 4, totalTokens = 16)
            } else {
                _apiTestMessage.value = "Test failed: $lastError"
            }
            _isTestingApi.value = false
        }
    }

    fun setGeminiModel(model: String) {
        viewModelScope.launch {
            userPreferencesRepository.setGeminiModel(model)
            _saveMessage.value = "AI engine set to $model"
        }
    }

    fun resetTodayApiUsage() {
        viewModelScope.launch {
            userPreferencesRepository.resetTodayApiUsage()
            _saveMessage.value = "Daily API token stats reset to 0"
        }
    }

    fun setExternalRequestCount(count: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setExternalRequestOffset(count)
            _saveMessage.value = "External requests updated to $count"
        }
    }

    fun addExternalRequests(delta: Int) {
        viewModelScope.launch {
            userPreferencesRepository.addExternalRequests(delta)
            val sign = if (delta >= 0) "+$delta" else "$delta"
            _saveMessage.value = "Adjusted external usage by $sign"
        }
    }

    fun setDarkMode(mode: String) {
        viewModelScope.launch {
            userPreferencesRepository.setDarkModePreference(mode)
        }
    }

    fun setThemeColor(colorId: String) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeColor(colorId)
        }
    }

    fun logWeight(weightKg: Float) {
        viewModelScope.launch {
            val log = WeightLog(
                date = AppDate.todayIso(),
                weightKg = weightKg
            )
            weightRepository.insertWeight(log)
            _saveMessage.value = "Logged weight: %.1f kg".format(weightKg)
        }
    }

    fun deleteWeight(id: Long) {
        viewModelScope.launch {
            weightRepository.deleteWeight(id)
            _saveMessage.value = "Weight entry removed"
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setNotificationsEnabled(enabled)
            val updated = notificationPreferences.value.copy(enabled = enabled)
            NotificationScheduler.scheduleAll(context, updated)
            _saveMessage.value = if (enabled) "Reminders activated!" else "Reminders paused"
        }
    }

    fun setMealReminderEnabled(mealType: String, enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setMealReminderEnabled(mealType, enabled)
            val current = notificationPreferences.value
            val updated = when (mealType) {
                NotificationPreferences.MEAL_BREAKFAST -> current.copy(breakfastEnabled = enabled)
                NotificationPreferences.MEAL_LUNCH -> current.copy(lunchEnabled = enabled)
                NotificationPreferences.MEAL_SNACK -> current.copy(snackEnabled = enabled)
                NotificationPreferences.MEAL_DINNER -> current.copy(dinnerEnabled = enabled)
                NotificationPreferences.SUMMARY_DAILY -> current.copy(dailySummaryEnabled = enabled)
                else -> current
            }
            NotificationScheduler.scheduleAll(context, updated)
        }
    }

    fun setMealReminderTime(mealType: String, timeStr: String) {
        viewModelScope.launch {
            userPreferencesRepository.setMealReminderTime(mealType, timeStr)
            val current = notificationPreferences.value
            val updated = when (mealType) {
                NotificationPreferences.MEAL_BREAKFAST -> current.copy(breakfastTime = timeStr)
                NotificationPreferences.MEAL_LUNCH -> current.copy(lunchTime = timeStr)
                NotificationPreferences.MEAL_SNACK -> current.copy(snackTime = timeStr)
                NotificationPreferences.MEAL_DINNER -> current.copy(dinnerTime = timeStr)
                NotificationPreferences.SUMMARY_DAILY -> current.copy(dailySummaryTime = timeStr)
                else -> current
            }
            NotificationScheduler.scheduleAll(context, updated)
            _saveMessage.value = "Updated reminder time to $timeStr"
        }
    }

    fun sendTestNotification() {
        viewModelScope.launch {
            val aiName = try {
                userPreferencesRepository.getRaayaName().first().trim().ifBlank { "AI Assistant" }
            } catch (e: Exception) {
                "AI Assistant"
            }
            val advice = try {
                aiWellWisherCoach.generateWellWisherAdvice(
                    mealType = NotificationPreferences.MEAL_LUNCH,
                    aiName = aiName
                )
            } catch (e: Exception) {
                null
            }
            NotificationHelper.showMealReminder(
                context = context,
                mealType = NotificationPreferences.MEAL_LUNCH,
                customTitle = advice?.title ?: "$aiName: Midday Fuel Check 💛",
                customBody = advice?.body ?: "Haven't logged yet today? Grab an egg/paneer roll or sandwich (+450 kcal) to stay fueled!",
                aiName = aiName,
                replacementSuggestion = advice?.recommendedActionFood
            )
            _saveMessage.value = "AI Well-Wisher reminder sent! Check notification bar."
        }
    }

    fun setInAppNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setInAppNotificationsEnabled(enabled)
            _saveMessage.value = if (enabled) "In-app notifications enabled" else "In-app notifications disabled"
        }
    }

    fun setFoodLogNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setFoodLogNotificationEnabled(enabled)
            _saveMessage.value = if (enabled) "Food logged alerts enabled" else "Food logged alerts disabled"
        }
    }

    fun setChatResponseNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setChatResponseNotificationEnabled(enabled)
            _saveMessage.value = if (enabled) "AI chat response alerts enabled" else "AI chat response alerts disabled"
        }
    }

    fun setTokenAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setTokenAlertsEnabled(enabled)
            _saveMessage.value = if (enabled) "Token threshold alerts enabled" else "Token threshold alerts disabled"
        }
    }

    fun setDailyTokenAlertLimit(limit: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setDailyTokenAlertLimit(limit)
            _saveMessage.value = "Daily token alert budget set to %,d tokens".format(limit)
        }
    }

    fun sendTestFoodLogNotification() {
        NotificationHelper.showTestFoodLogNotification(context)
        _saveMessage.value = "Test food logged alert sent! Check notification bar."
    }

    fun sendTestChatResponseNotification() {
        viewModelScope.launch {
            val aiName = try {
                userPreferencesRepository.getRaayaName().first().trim().ifBlank { "AI Assistant" }
            } catch (e: Exception) {
                "AI Assistant"
            }
            NotificationHelper.showTestChatResponseNotification(context, aiName = aiName)
            _saveMessage.value = "Test chat response alert sent! Check notification bar."
        }
    }

    fun sendTestTokenAlertNotification() {
        NotificationHelper.showTestTokenAlertNotification(context)
        _saveMessage.value = "Test token usage alert sent! Check notification bar."
    }

    fun clearMessages() {
        _saveMessage.value = null
        _apiTestMessage.value = null
    }
}
