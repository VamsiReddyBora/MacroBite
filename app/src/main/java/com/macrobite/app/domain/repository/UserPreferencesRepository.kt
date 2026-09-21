package com.macrobite.app.domain.repository

import com.macrobite.app.domain.model.DailyApiUsage
import com.macrobite.app.domain.model.UserTargets
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    fun getTargets(): Flow<UserTargets>
    suspend fun saveTargets(targets: UserTargets)
    fun getUseGemini(): Flow<Boolean>
    suspend fun setUseGemini(enabled: Boolean)
    fun getGeminiApiKey(): Flow<String>
    suspend fun setGeminiApiKey(apiKey: String)
    fun getDarkModePreference(): Flow<String> // "system", "dark", "light"
    suspend fun setDarkModePreference(mode: String)
    fun getDailyApiUsage(): Flow<DailyApiUsage>
    suspend fun recordApiUsage(promptTokens: Int, candidateTokens: Int, totalTokens: Int)
    suspend fun resetTodayApiUsage()
    fun getExternalRequestOffset(): Flow<Int>
    suspend fun setExternalRequestOffset(offset: Int)
    suspend fun addExternalRequests(delta: Int)
    fun getGeminiModel(): Flow<String>
    suspend fun setGeminiModel(model: String)
    fun getThemeColor(): Flow<String>
    suspend fun setThemeColor(colorId: String)

    // Raaya AI Profile & Chat Settings (Isolated exclusively for Chat Screen)
    fun getRaayaName(): Flow<String>
    suspend fun setRaayaName(name: String)
    fun getRaayaAvatar(): Flow<String>
    suspend fun setRaayaAvatar(avatarId: String)
    fun getRaayaPersonality(): Flow<String>
    suspend fun setRaayaPersonality(personality: String)
    fun getRaayaAutoLog(): Flow<Boolean>
    suspend fun setRaayaAutoLog(enabled: Boolean)
    fun getRaayaIncludeMicros(): Flow<Boolean>
    suspend fun setRaayaIncludeMicros(enabled: Boolean)
    fun getRaayaDietaryNotes(): Flow<String>
    suspend fun setRaayaDietaryNotes(notes: String)
    fun getRaayaWebSearchEnabled(): Flow<Boolean>
    suspend fun setRaayaWebSearchEnabled(enabled: Boolean)
    fun getVoiceStyle(): Flow<String>
    suspend fun setVoiceStyle(style: String)
    fun getReplyStyle(): Flow<String>
    suspend fun setReplyStyle(style: String)
    fun getModelUsageStats(): Flow<Map<String, Int>>
    suspend fun incrementModelUsage(modelId: String)

    // Notification & Precise Reminder Settings
    fun getNotificationPreferences(): Flow<com.macrobite.app.domain.model.NotificationPreferences>
    suspend fun saveNotificationPreferences(preferences: com.macrobite.app.domain.model.NotificationPreferences)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    fun getInAppNotificationsEnabled(): Flow<Boolean>
    suspend fun setInAppNotificationsEnabled(enabled: Boolean)
    suspend fun setMealReminderEnabled(mealType: String, enabled: Boolean)
    suspend fun setMealReminderTime(mealType: String, time: String)
    suspend fun setFoodLogNotificationEnabled(enabled: Boolean)
    suspend fun setChatResponseNotificationEnabled(enabled: Boolean)
    suspend fun setTokenAlertsEnabled(enabled: Boolean)
    suspend fun setDailyTokenAlertLimit(limit: Int)

    // Google Health & Activity Preferences
    fun getStepGoal(): Flow<Int>
    suspend fun setStepGoal(goal: Int)
    fun getGoogleHealthSyncEnabled(): Flow<Boolean>
    suspend fun setGoogleHealthSyncEnabled(enabled: Boolean)

    // Backup
    fun getAutoBackupEnabled(): Flow<Boolean>
    suspend fun setAutoBackupEnabled(enabled: Boolean)

    // Jarvis Contact Aliases (e.g. "Dad" -> "Daddy")
    fun getContactAliases(): Flow<Map<String, String>>
    suspend fun setContactAlias(alias: String, contactTarget: String)
    suspend fun removeContactAlias(alias: String)

    // Custom Models Registered by User
    fun getCustomModels(): Flow<List<String>>
    suspend fun addCustomModel(model: String)
    suspend fun removeCustomModel(model: String)

    // Custom Barcodes Memory
    fun getCustomBarcodes(): Flow<Map<String, String>>
    suspend fun saveCustomBarcode(barcode: String, resultJson: String)
}

