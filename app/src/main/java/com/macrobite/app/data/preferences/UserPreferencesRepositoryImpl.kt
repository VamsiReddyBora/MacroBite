package com.macrobite.app.data.preferences

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.macrobite.app.domain.model.AppDate
import com.macrobite.app.domain.model.DailyApiUsage
import com.macrobite.app.domain.model.NotificationPreferences
import com.macrobite.app.domain.model.UserTargets
import com.macrobite.app.domain.repository.UserPreferencesRepository
import com.macrobite.app.notification.NotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

internal val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "macrobite_prefs")

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : UserPreferencesRepository {

    private object PreferencesKeys {
        val TARGET_CALORIES = intPreferencesKey("target_calories")
        val TARGET_PROTEIN = intPreferencesKey("target_protein")
        val TARGET_CARBS = intPreferencesKey("target_carbs")
        val TARGET_FATS = intPreferencesKey("target_fats")
        val USE_GEMINI = booleanPreferencesKey("use_gemini")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val GEMINI_MODEL = stringPreferencesKey("gemini_model")
        val DAILY_USAGE_DATE = stringPreferencesKey("daily_usage_date")
        val DAILY_USAGE_TOTAL_TOKENS = intPreferencesKey("daily_usage_total_tokens")
        val DAILY_USAGE_PROMPT_TOKENS = intPreferencesKey("daily_usage_prompt_tokens")
        val DAILY_USAGE_CANDIDATE_TOKENS = intPreferencesKey("daily_usage_candidate_tokens")
        val DAILY_USAGE_REQUEST_COUNT = intPreferencesKey("daily_usage_request_count")
        val THEME_COLOR = stringPreferencesKey("theme_color")
        val RAAYA_NAME = stringPreferencesKey("raaya_name")
        val RAAYA_AVATAR = stringPreferencesKey("raaya_avatar")
        val RAAYA_PERSONALITY = stringPreferencesKey("raaya_personality")
        val RAAYA_AUTO_LOG = booleanPreferencesKey("raaya_auto_log")
        val RAAYA_INCLUDE_MICROS = booleanPreferencesKey("raaya_include_micros")
        val RAAYA_DIETARY_NOTES = stringPreferencesKey("raaya_dietary_notes")
        val RAAYA_WEB_SEARCH = booleanPreferencesKey("raaya_web_search")
        val NOTIF_ENABLED = booleanPreferencesKey("notif_enabled")
        val NOTIF_BREAKFAST_ENABLED = booleanPreferencesKey("notif_breakfast_enabled")
        val NOTIF_BREAKFAST_TIME = stringPreferencesKey("notif_breakfast_time")
        val NOTIF_LUNCH_ENABLED = booleanPreferencesKey("notif_lunch_enabled")
        val NOTIF_LUNCH_TIME = stringPreferencesKey("notif_lunch_time")
        val NOTIF_SNACK_ENABLED = booleanPreferencesKey("notif_snack_enabled")
        val NOTIF_SNACK_TIME = stringPreferencesKey("notif_snack_time")
        val NOTIF_DINNER_ENABLED = booleanPreferencesKey("notif_dinner_enabled")
        val NOTIF_DINNER_TIME = stringPreferencesKey("notif_dinner_time")
        val NOTIF_SUMMARY_ENABLED = booleanPreferencesKey("notif_summary_enabled")
        val NOTIF_SUMMARY_TIME = stringPreferencesKey("notif_summary_time")
        val NOTIF_FOOD_LOG_ENABLED = booleanPreferencesKey("notif_food_log_enabled")
        val NOTIF_IN_APP_ENABLED = booleanPreferencesKey("notif_in_app_enabled")
        val NOTIF_CHAT_RESPONSE_ENABLED = booleanPreferencesKey("notif_chat_response_enabled")
        val NOTIF_TOKEN_ALERTS_ENABLED = booleanPreferencesKey("notif_token_alerts_enabled")
        val NOTIF_TOKEN_ALERT_LIMIT = intPreferencesKey("notif_token_alert_limit")
        val TOKEN_ALERT_NOTIFIED_50_DATE = stringPreferencesKey("token_alert_notified_50_date")
        val TOKEN_ALERT_NOTIFIED_75_DATE = stringPreferencesKey("token_alert_notified_75_date")
        val TOKEN_ALERT_NOTIFIED_90_DATE = stringPreferencesKey("token_alert_notified_90_date")
        val STEP_GOAL = intPreferencesKey("step_goal")
        val GOOGLE_HEALTH_SYNC_ENABLED = booleanPreferencesKey("google_health_sync_enabled")
        val CONTACT_ALIASES_JSON = stringPreferencesKey("contact_aliases_json")
        val VOICE_STYLE = stringPreferencesKey("voice_style")
        val REPLY_STYLE = stringPreferencesKey("reply_style")
        val CUSTOM_MODELS = stringSetPreferencesKey("custom_models")
        val AUTO_BACKUP = booleanPreferencesKey("auto_backup")
        val CUSTOM_BARCODES_JSON = stringPreferencesKey("custom_barcodes_json")
        val EXTERNAL_REQUEST_DATE = stringPreferencesKey("external_request_date")
        val EXTERNAL_REQUEST_COUNT = intPreferencesKey("external_request_count")
    }

    private val safePreferencesFlow: Flow<Preferences> = context.dataStore.data
        .catch { exception ->
            Log.e("UserPrefs", "DataStore read error, defaulting to empty preferences: ${exception.message}")
            emit(emptyPreferences())
        }

    override fun getTargets(): Flow<UserTargets> {
        return safePreferencesFlow.map { preferences ->
            UserTargets(
                calories = preferences[PreferencesKeys.TARGET_CALORIES] ?: 0,
                protein = preferences[PreferencesKeys.TARGET_PROTEIN] ?: 0,
                carbs = preferences[PreferencesKeys.TARGET_CARBS] ?: 0,
                fats = preferences[PreferencesKeys.TARGET_FATS] ?: 0
            )
        }.catch {
            emit(UserTargets())
        }
    }

    override suspend fun saveTargets(targets: UserTargets) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.TARGET_CALORIES] = targets.calories
                preferences[PreferencesKeys.TARGET_PROTEIN] = targets.protein
                preferences[PreferencesKeys.TARGET_CARBS] = targets.carbs
                preferences[PreferencesKeys.TARGET_FATS] = targets.fats
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to save targets", e)
        }
    }

    override fun getUseGemini(): Flow<Boolean> {
        return safePreferencesFlow.map { preferences ->
            preferences[PreferencesKeys.USE_GEMINI] ?: true
        }.catch { emit(true) }
    }

    override suspend fun setUseGemini(enabled: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.USE_GEMINI] = enabled
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to set useGemini", e)
        }
    }

    override fun getGeminiApiKey(): Flow<String> {
        return safePreferencesFlow.map { preferences ->
            preferences[PreferencesKeys.GEMINI_API_KEY]?.trim() ?: ""
        }.catch { emit("") }
    }

    override suspend fun setGeminiApiKey(apiKey: String) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.GEMINI_API_KEY] = apiKey.trim()
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to set geminiApiKey", e)
        }
    }

    override fun getDarkModePreference(): Flow<String> {
        return safePreferencesFlow.map { preferences ->
            preferences[PreferencesKeys.DARK_MODE] ?: "dark"
        }.catch { emit("dark") }
    }

    override suspend fun setDarkModePreference(mode: String) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.DARK_MODE] = mode
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to set darkModePreference", e)
        }
    }

    override fun getDailyApiUsage(): Flow<DailyApiUsage> {
        val today = AppDate.todayIso()
        return safePreferencesFlow.map { preferences ->
            val storedDate = preferences[PreferencesKeys.DAILY_USAGE_DATE] ?: ""
            val model = preferences[PreferencesKeys.GEMINI_MODEL] ?: "gemini-3.5-flash-lite"
            val dailyLimit = when {
                model.contains("flash-lite", ignoreCase = true) -> 500
                model.contains("flash", ignoreCase = true) -> 20
                else -> 500
            }
            val storedExtDate = preferences[PreferencesKeys.EXTERNAL_REQUEST_DATE] ?: ""
            val extRequests = if (storedExtDate == today) {
                preferences[PreferencesKeys.EXTERNAL_REQUEST_COUNT] ?: 40
            } else {
                val backup = readPersistentQuotaBackup()
                if (backup != null && backup["date"] == today) {
                    (backup["externalRequests"] as? Number)?.toInt() ?: 40
                } else 40
            }

            if (storedDate == today) {
                DailyApiUsage(
                    date = today,
                    totalTokens = preferences[PreferencesKeys.DAILY_USAGE_TOTAL_TOKENS] ?: 0,
                    promptTokens = preferences[PreferencesKeys.DAILY_USAGE_PROMPT_TOKENS] ?: 0,
                    candidateTokens = preferences[PreferencesKeys.DAILY_USAGE_CANDIDATE_TOKENS] ?: 0,
                    requestCount = preferences[PreferencesKeys.DAILY_USAGE_REQUEST_COUNT] ?: 0,
                    externalRequestCount = extRequests,
                    dailyLimit = dailyLimit
                )
            } else {
                // Check if we have persistent backup from today across app reinstall
                val backup = readPersistentQuotaBackup()
                if (backup != null && backup["date"] == today) {
                    val totalTokens = (backup["totalTokens"] as? Number)?.toInt() ?: 0
                    val promptTokens = (backup["promptTokens"] as? Number)?.toInt() ?: 0
                    val candidateTokens = (backup["candidateTokens"] as? Number)?.toInt() ?: 0
                    val reqCount = (backup["requestCount"] as? Number)?.toInt() ?: 0
                    val extReqs = (backup["externalRequests"] as? Number)?.toInt() ?: extRequests

                    // Asynchronously sync into DataStore
                    CoroutineScope(Dispatchers.IO).launch {
                        restoreApiUsage(today, promptTokens, candidateTokens, totalTokens, reqCount, extReqs)
                    }

                    DailyApiUsage(
                        date = today,
                        totalTokens = totalTokens,
                        promptTokens = promptTokens,
                        candidateTokens = candidateTokens,
                        requestCount = reqCount,
                        externalRequestCount = extReqs,
                        dailyLimit = dailyLimit
                    )
                } else {
                    DailyApiUsage(
                        date = today,
                        externalRequestCount = extRequests,
                        dailyLimit = dailyLimit
                    )
                }
            }
        }.catch {
            emit(DailyApiUsage(date = today, dailyLimit = 500))
        }
    }

    override suspend fun recordApiUsage(promptTokens: Int, candidateTokens: Int, totalTokens: Int) {
        try {
            val today = AppDate.todayIso()
            context.dataStore.edit { preferences ->
                val storedDate = preferences[PreferencesKeys.DAILY_USAGE_DATE] ?: ""
                if (storedDate != today) {
                    preferences[PreferencesKeys.DAILY_USAGE_DATE] = today
                    preferences[PreferencesKeys.DAILY_USAGE_TOTAL_TOKENS] = totalTokens.coerceAtLeast(0)
                    preferences[PreferencesKeys.DAILY_USAGE_PROMPT_TOKENS] = promptTokens.coerceAtLeast(0)
                    preferences[PreferencesKeys.DAILY_USAGE_CANDIDATE_TOKENS] = candidateTokens.coerceAtLeast(0)
                    preferences[PreferencesKeys.DAILY_USAGE_REQUEST_COUNT] = 1
                } else {
                    val currentTotal = preferences[PreferencesKeys.DAILY_USAGE_TOTAL_TOKENS] ?: 0
                    val currentPrompt = preferences[PreferencesKeys.DAILY_USAGE_PROMPT_TOKENS] ?: 0
                    val currentCandidate = preferences[PreferencesKeys.DAILY_USAGE_CANDIDATE_TOKENS] ?: 0
                    val currentRequests = preferences[PreferencesKeys.DAILY_USAGE_REQUEST_COUNT] ?: 0

                    preferences[PreferencesKeys.DAILY_USAGE_TOTAL_TOKENS] = currentTotal + totalTokens.coerceAtLeast(0)
                    preferences[PreferencesKeys.DAILY_USAGE_PROMPT_TOKENS] = currentPrompt + promptTokens.coerceAtLeast(0)
                    preferences[PreferencesKeys.DAILY_USAGE_CANDIDATE_TOKENS] = currentCandidate + candidateTokens.coerceAtLeast(0)
                    preferences[PreferencesKeys.DAILY_USAGE_REQUEST_COUNT] = currentRequests + 1
                }

                // Check Token Quota Alerts (50%, 75%, 90%)
                val notifMasterEnabled = preferences[PreferencesKeys.NOTIF_ENABLED] ?: true
                val tokenAlertsEnabled = preferences[PreferencesKeys.NOTIF_TOKEN_ALERTS_ENABLED] ?: true
                if (notifMasterEnabled && tokenAlertsEnabled) {
                    val alertLimit = preferences[PreferencesKeys.NOTIF_TOKEN_ALERT_LIMIT] ?: 100_000
                    val finalTotal = preferences[PreferencesKeys.DAILY_USAGE_TOTAL_TOKENS] ?: 0
                    val finalRequests = preferences[PreferencesKeys.DAILY_USAGE_REQUEST_COUNT] ?: 0
                    val percentTokens = if (alertLimit > 0) (finalTotal.toDouble() / alertLimit.toDouble()) * 100.0 else 0.0

                    val notified90 = preferences[PreferencesKeys.TOKEN_ALERT_NOTIFIED_90_DATE] == today
                    val notified75 = preferences[PreferencesKeys.TOKEN_ALERT_NOTIFIED_75_DATE] == today
                    val notified50 = preferences[PreferencesKeys.TOKEN_ALERT_NOTIFIED_50_DATE] == today

                    if (percentTokens >= 90.0 && !notified90) {
                        preferences[PreferencesKeys.TOKEN_ALERT_NOTIFIED_90_DATE] = today
                        NotificationHelper.showTokenUsageAlert(context, 90, finalTotal, alertLimit, finalRequests)
                    } else if (percentTokens >= 75.0 && !notified75) {
                        preferences[PreferencesKeys.TOKEN_ALERT_NOTIFIED_75_DATE] = today
                        NotificationHelper.showTokenUsageAlert(context, 75, finalTotal, alertLimit, finalRequests)
                    } else if (percentTokens >= 50.0 && !notified50) {
                        preferences[PreferencesKeys.TOKEN_ALERT_NOTIFIED_50_DATE] = today
                        NotificationHelper.showTokenUsageAlert(context, 50, finalTotal, alertLimit, finalRequests)
                    }
                }

                val finalPrompt = preferences[PreferencesKeys.DAILY_USAGE_PROMPT_TOKENS] ?: 0
                val finalCand = preferences[PreferencesKeys.DAILY_USAGE_CANDIDATE_TOKENS] ?: 0
                val extRequests = preferences[PreferencesKeys.EXTERNAL_REQUEST_COUNT] ?: 0
                savePersistentQuotaBackup(today, finalPrompt, finalCand, preferences[PreferencesKeys.DAILY_USAGE_TOTAL_TOKENS] ?: 0, preferences[PreferencesKeys.DAILY_USAGE_REQUEST_COUNT] ?: 0, extRequests)
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to record API usage", e)
        }
    }

    override suspend fun resetTodayApiUsage() {
        try {
            val today = AppDate.todayIso()
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.DAILY_USAGE_DATE] = today
                preferences[PreferencesKeys.DAILY_USAGE_TOTAL_TOKENS] = 0
                preferences[PreferencesKeys.DAILY_USAGE_PROMPT_TOKENS] = 0
                preferences[PreferencesKeys.DAILY_USAGE_CANDIDATE_TOKENS] = 0
                preferences[PreferencesKeys.DAILY_USAGE_REQUEST_COUNT] = 0
                preferences[PreferencesKeys.EXTERNAL_REQUEST_DATE] = today
                preferences[PreferencesKeys.EXTERNAL_REQUEST_COUNT] = 0
                preferences.remove(PreferencesKeys.TOKEN_ALERT_NOTIFIED_50_DATE)
                preferences.remove(PreferencesKeys.TOKEN_ALERT_NOTIFIED_75_DATE)
                preferences.remove(PreferencesKeys.TOKEN_ALERT_NOTIFIED_90_DATE)
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to reset today's API usage", e)
        }
    }

    override fun getExternalRequestOffset(): Flow<Int> {
        val today = AppDate.todayIso()
        return safePreferencesFlow.map { preferences ->
            val storedDate = preferences[PreferencesKeys.EXTERNAL_REQUEST_DATE] ?: ""
            if (storedDate == today) {
                preferences[PreferencesKeys.EXTERNAL_REQUEST_COUNT] ?: 40
            } else 40
        }.catch { emit(40) }
    }

    override suspend fun setExternalRequestOffset(offset: Int) {
        try {
            val today = AppDate.todayIso()
            context.dataStore.edit { preferences ->
                val newCount = offset.coerceAtLeast(0)
                preferences[PreferencesKeys.EXTERNAL_REQUEST_DATE] = today
                preferences[PreferencesKeys.EXTERNAL_REQUEST_COUNT] = newCount
                val prompt = preferences[PreferencesKeys.DAILY_USAGE_PROMPT_TOKENS] ?: 0
                val cand = preferences[PreferencesKeys.DAILY_USAGE_CANDIDATE_TOKENS] ?: 0
                val total = preferences[PreferencesKeys.DAILY_USAGE_TOTAL_TOKENS] ?: 0
                val reqs = preferences[PreferencesKeys.DAILY_USAGE_REQUEST_COUNT] ?: 0
                savePersistentQuotaBackup(today, prompt, cand, total, reqs, newCount)
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to set external request count", e)
        }
    }

    override suspend fun addExternalRequests(delta: Int) {
        try {
            val today = AppDate.todayIso()
            context.dataStore.edit { preferences ->
                val storedDate = preferences[PreferencesKeys.EXTERNAL_REQUEST_DATE] ?: ""
                val current = if (storedDate == today) {
                    preferences[PreferencesKeys.EXTERNAL_REQUEST_COUNT] ?: 0
                } else 0
                val newCount = (current + delta).coerceAtLeast(0)
                preferences[PreferencesKeys.EXTERNAL_REQUEST_DATE] = today
                preferences[PreferencesKeys.EXTERNAL_REQUEST_COUNT] = newCount
                val prompt = preferences[PreferencesKeys.DAILY_USAGE_PROMPT_TOKENS] ?: 0
                val cand = preferences[PreferencesKeys.DAILY_USAGE_CANDIDATE_TOKENS] ?: 0
                val total = preferences[PreferencesKeys.DAILY_USAGE_TOTAL_TOKENS] ?: 0
                val reqs = preferences[PreferencesKeys.DAILY_USAGE_REQUEST_COUNT] ?: 0
                savePersistentQuotaBackup(today, prompt, cand, total, reqs, newCount)
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to add external requests", e)
        }
    }

    override fun getGeminiModel(): Flow<String> {
        return safePreferencesFlow.map { preferences ->
            val stored = preferences[PreferencesKeys.GEMINI_MODEL] ?: ""
            if (stored.isBlank() || stored == "gemini-3.7-flash") {
                "gemini-3.5-flash-lite"
            } else {
                stored
            }
        }.catch { emit("gemini-3.5-flash-lite") }
    }

    override suspend fun setGeminiModel(model: String) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.GEMINI_MODEL] = model.trim()
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to set geminiModel", e)
        }
    }

    override fun getThemeColor(): Flow<String> {
        return safePreferencesFlow.map { preferences ->
            preferences[PreferencesKeys.THEME_COLOR] ?: "amber"
        }.distinctUntilChanged()
    }

    override suspend fun setThemeColor(colorId: String) {
        try {
            val trimmed = colorId.trim()
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.THEME_COLOR] = trimmed
            }
            NotificationHelper.updateThemeColor(trimmed)
            com.macrobite.app.widget.MacroBiteChatWidgetProvider.savePreferences(context, themeColor = trimmed)
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to set themeColor", e)
        }
    }

    override fun getRaayaName(): Flow<String> {
        return safePreferencesFlow.map { preferences ->
            preferences[PreferencesKeys.RAAYA_NAME]?.takeIf { it.isNotBlank() } ?: "AI"
        }.catch { emit("AI") }
    }

    override suspend fun setRaayaName(name: String) {
        try {
            val resolvedName = name.trim().ifBlank { "AI" }
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.RAAYA_NAME] = resolvedName
            }
            com.macrobite.app.widget.MacroBiteChatWidgetProvider.savePreferences(context, assistantName = resolvedName)
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to set raayaName", e)
        }
    }

    override fun getRaayaAvatar(): Flow<String> {
        return safePreferencesFlow.map { preferences ->
            preferences[PreferencesKeys.RAAYA_AVATAR]?.takeIf { it.isNotBlank() } ?: "default"
        }.catch { emit("default") }
    }

    override suspend fun setRaayaAvatar(avatarId: String) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.RAAYA_AVATAR] = avatarId.trim()
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to set raayaAvatar", e)
        }
    }

    override fun getRaayaPersonality(): Flow<String> {
        return safePreferencesFlow.map { preferences ->
            preferences[PreferencesKeys.RAAYA_PERSONALITY]?.takeIf { it.isNotBlank() } ?: "Friendly & Encouraging"
        }.catch { emit("Friendly & Encouraging") }
    }

    override suspend fun setRaayaPersonality(personality: String) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.RAAYA_PERSONALITY] = personality.trim()
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to set raayaPersonality", e)
        }
    }

    override fun getRaayaAutoLog(): Flow<Boolean> {
        return safePreferencesFlow.map { preferences ->
            preferences[PreferencesKeys.RAAYA_AUTO_LOG] ?: true
        }.catch { emit(true) }
    }

    override suspend fun setRaayaAutoLog(enabled: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.RAAYA_AUTO_LOG] = enabled
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to set raayaAutoLog", e)
        }
    }

    override fun getRaayaIncludeMicros(): Flow<Boolean> {
        return safePreferencesFlow.map { preferences ->
            preferences[PreferencesKeys.RAAYA_INCLUDE_MICROS] ?: true
        }.catch { emit(true) }
    }

    override suspend fun setRaayaIncludeMicros(enabled: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.RAAYA_INCLUDE_MICROS] = enabled
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to set raayaIncludeMicros", e)
        }
    }

    override fun getRaayaDietaryNotes(): Flow<String> {
        return safePreferencesFlow.map { preferences ->
            preferences[PreferencesKeys.RAAYA_DIETARY_NOTES] ?: ""
        }.catch { emit("") }
    }

    override suspend fun setRaayaDietaryNotes(notes: String) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.RAAYA_DIETARY_NOTES] = notes.trim()
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to set raayaDietaryNotes", e)
        }
    }

    override fun getRaayaWebSearchEnabled(): Flow<Boolean> {
        return safePreferencesFlow.map { preferences ->
            preferences[PreferencesKeys.RAAYA_WEB_SEARCH] ?: false
        }.catch { emit(false) }
    }

    override suspend fun setRaayaWebSearchEnabled(enabled: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.RAAYA_WEB_SEARCH] = enabled
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to set raayaWebSearch", e)
        }
    }

    override fun getVoiceStyle(): Flow<String> {
        return safePreferencesFlow.map { preferences ->
            preferences[PreferencesKeys.VOICE_STYLE] ?: "normal"
        }.catch { emit("normal") }
    }

    override suspend fun setVoiceStyle(style: String) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.VOICE_STYLE] = style.trim().lowercase()
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to set voiceStyle", e)
        }
    }

    override fun getReplyStyle(): Flow<String> {
        return safePreferencesFlow.map { preferences ->
            preferences[PreferencesKeys.REPLY_STYLE] ?: "normal"
        }.catch { emit("normal") }
    }

    override suspend fun setReplyStyle(style: String) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.REPLY_STYLE] = style.trim().lowercase()
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to set replyStyle", e)
        }
    }

    override fun getModelUsageStats(): Flow<Map<String, Int>> {
        val allTrackedModels = listOf(
            "gemini-3.5-flash-lite",
            "gemini-3.1-flash-lite",
            "gemini-3.5-flash",
            "gemini-3.6-flash",
            "gemini-2.5-flash",
            "gemini-flash-lite-latest"
        )
        return safePreferencesFlow.map { preferences ->
            allTrackedModels.associateWith { modelId ->
                val key = intPreferencesKey("model_req_$modelId")
                preferences[key] ?: 0
            }
        }.catch {
            emit(emptyMap())
        }
    }

    override suspend fun incrementModelUsage(modelId: String) {
        try {
            val key = intPreferencesKey("model_req_$modelId")
            context.dataStore.edit { preferences ->
                val current = preferences[key] ?: 0
                preferences[key] = current + 1
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to increment model usage", e)
        }
    }

    override fun getNotificationPreferences(): Flow<NotificationPreferences> {
        return safePreferencesFlow.map { preferences ->
            NotificationPreferences(
                enabled = preferences[PreferencesKeys.NOTIF_ENABLED] ?: true,
                breakfastEnabled = preferences[PreferencesKeys.NOTIF_BREAKFAST_ENABLED] ?: true,
                breakfastTime = preferences[PreferencesKeys.NOTIF_BREAKFAST_TIME] ?: "08:30",
                lunchEnabled = preferences[PreferencesKeys.NOTIF_LUNCH_ENABLED] ?: true,
                lunchTime = preferences[PreferencesKeys.NOTIF_LUNCH_TIME] ?: "13:00",
                snackEnabled = preferences[PreferencesKeys.NOTIF_SNACK_ENABLED] ?: true,
                snackTime = preferences[PreferencesKeys.NOTIF_SNACK_TIME] ?: "17:30",
                dinnerEnabled = preferences[PreferencesKeys.NOTIF_DINNER_ENABLED] ?: true,
                dinnerTime = preferences[PreferencesKeys.NOTIF_DINNER_TIME] ?: "20:30",
                dailySummaryEnabled = preferences[PreferencesKeys.NOTIF_SUMMARY_ENABLED] ?: true,
                dailySummaryTime = preferences[PreferencesKeys.NOTIF_SUMMARY_TIME] ?: "21:30",
                inAppNotificationsEnabled = preferences[PreferencesKeys.NOTIF_IN_APP_ENABLED] ?: true,
                foodLogNotificationEnabled = preferences[PreferencesKeys.NOTIF_FOOD_LOG_ENABLED] ?: false,
                chatResponseNotificationEnabled = preferences[PreferencesKeys.NOTIF_CHAT_RESPONSE_ENABLED] ?: true,
                tokenUsageAlertsEnabled = preferences[PreferencesKeys.NOTIF_TOKEN_ALERTS_ENABLED] ?: true,
                dailyTokenAlertLimit = preferences[PreferencesKeys.NOTIF_TOKEN_ALERT_LIMIT] ?: 100_000
            )
        }.catch {
            emit(NotificationPreferences())
        }
    }

    override suspend fun saveNotificationPreferences(preferences: NotificationPreferences) {
        try {
            context.dataStore.edit { prefs ->
                prefs[PreferencesKeys.NOTIF_ENABLED] = preferences.enabled
                prefs[PreferencesKeys.NOTIF_BREAKFAST_ENABLED] = preferences.breakfastEnabled
                prefs[PreferencesKeys.NOTIF_BREAKFAST_TIME] = preferences.breakfastTime
                prefs[PreferencesKeys.NOTIF_LUNCH_ENABLED] = preferences.lunchEnabled
                prefs[PreferencesKeys.NOTIF_LUNCH_TIME] = preferences.lunchTime
                prefs[PreferencesKeys.NOTIF_SNACK_ENABLED] = preferences.snackEnabled
                prefs[PreferencesKeys.NOTIF_SNACK_TIME] = preferences.snackTime
                prefs[PreferencesKeys.NOTIF_DINNER_ENABLED] = preferences.dinnerEnabled
                prefs[PreferencesKeys.NOTIF_DINNER_TIME] = preferences.dinnerTime
                prefs[PreferencesKeys.NOTIF_SUMMARY_ENABLED] = preferences.dailySummaryEnabled
                prefs[PreferencesKeys.NOTIF_SUMMARY_TIME] = preferences.dailySummaryTime
                prefs[PreferencesKeys.NOTIF_IN_APP_ENABLED] = preferences.inAppNotificationsEnabled
                prefs[PreferencesKeys.NOTIF_FOOD_LOG_ENABLED] = preferences.foodLogNotificationEnabled
                prefs[PreferencesKeys.NOTIF_CHAT_RESPONSE_ENABLED] = preferences.chatResponseNotificationEnabled
                prefs[PreferencesKeys.NOTIF_TOKEN_ALERTS_ENABLED] = preferences.tokenUsageAlertsEnabled
                prefs[PreferencesKeys.NOTIF_TOKEN_ALERT_LIMIT] = preferences.dailyTokenAlertLimit
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to save notification preferences", e)
        }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.NOTIF_ENABLED] = enabled
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to set notifications enabled", e)
        }
    }

    override fun getInAppNotificationsEnabled(): Flow<Boolean> {
        return safePreferencesFlow.map { preferences ->
            preferences[PreferencesKeys.NOTIF_IN_APP_ENABLED] ?: true
        }.distinctUntilChanged()
    }

    override suspend fun setInAppNotificationsEnabled(enabled: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.NOTIF_IN_APP_ENABLED] = enabled
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to set in-app notifications enabled", e)
        }
    }

    override suspend fun setMealReminderEnabled(mealType: String, enabled: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                when (mealType) {
                    NotificationPreferences.MEAL_BREAKFAST -> preferences[PreferencesKeys.NOTIF_BREAKFAST_ENABLED] = enabled
                    NotificationPreferences.MEAL_LUNCH -> preferences[PreferencesKeys.NOTIF_LUNCH_ENABLED] = enabled
                    NotificationPreferences.MEAL_SNACK -> preferences[PreferencesKeys.NOTIF_SNACK_ENABLED] = enabled
                    NotificationPreferences.MEAL_DINNER -> preferences[PreferencesKeys.NOTIF_DINNER_ENABLED] = enabled
                    NotificationPreferences.SUMMARY_DAILY -> preferences[PreferencesKeys.NOTIF_SUMMARY_ENABLED] = enabled
                }
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to set meal reminder enabled for $mealType", e)
        }
    }

    override suspend fun setMealReminderTime(mealType: String, time: String) {
        try {
            context.dataStore.edit { preferences ->
                when (mealType) {
                    NotificationPreferences.MEAL_BREAKFAST -> preferences[PreferencesKeys.NOTIF_BREAKFAST_TIME] = time
                    NotificationPreferences.MEAL_LUNCH -> preferences[PreferencesKeys.NOTIF_LUNCH_TIME] = time
                    NotificationPreferences.MEAL_SNACK -> preferences[PreferencesKeys.NOTIF_SNACK_TIME] = time
                    NotificationPreferences.MEAL_DINNER -> preferences[PreferencesKeys.NOTIF_DINNER_TIME] = time
                    NotificationPreferences.SUMMARY_DAILY -> preferences[PreferencesKeys.NOTIF_SUMMARY_TIME] = time
                }
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to set meal reminder time for $mealType", e)
        }
    }

    override suspend fun setFoodLogNotificationEnabled(enabled: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.NOTIF_FOOD_LOG_ENABLED] = enabled
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to set food log notification enabled", e)
        }
    }

    override suspend fun setChatResponseNotificationEnabled(enabled: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.NOTIF_CHAT_RESPONSE_ENABLED] = enabled
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to set chat response notification enabled", e)
        }
    }

    override suspend fun setTokenAlertsEnabled(enabled: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.NOTIF_TOKEN_ALERTS_ENABLED] = enabled
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to set token alerts enabled", e)
        }
    }

    override suspend fun setDailyTokenAlertLimit(limit: Int) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.NOTIF_TOKEN_ALERT_LIMIT] = limit
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to set daily token alert limit", e)
        }
    }

    override fun getStepGoal(): Flow<Int> {
        return safePreferencesFlow.map { preferences ->
            preferences[PreferencesKeys.STEP_GOAL] ?: 10_000
        }.distinctUntilChanged()
    }

    override suspend fun setStepGoal(goal: Int) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.STEP_GOAL] = goal.coerceIn(1_000, 50_000)
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to set step goal", e)
        }
    }

    override fun getGoogleHealthSyncEnabled(): Flow<Boolean> {
        return safePreferencesFlow.map { preferences ->
            preferences[PreferencesKeys.GOOGLE_HEALTH_SYNC_ENABLED] ?: true
        }.distinctUntilChanged()
    }

    override suspend fun setGoogleHealthSyncEnabled(enabled: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.GOOGLE_HEALTH_SYNC_ENABLED] = enabled
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to set Google Health sync enabled", e)
        }
    }

    override fun getContactAliases(): Flow<Map<String, String>> {
        return safePreferencesFlow.map { prefs ->
            val json = prefs[PreferencesKeys.CONTACT_ALIASES_JSON]
            if (json.isNullOrBlank()) {
                emptyMap()
            } else {
                try {
                    val type = object : TypeToken<Map<String, String>>() {}.type
                    Gson().fromJson<Map<String, String>>(json, type) ?: emptyMap()
                } catch (e: Exception) {
                    emptyMap()
                }
            }
        }.distinctUntilChanged()
    }

    override suspend fun setContactAlias(alias: String, contactTarget: String) {
        val cleanAlias = alias.trim().lowercase()
        val cleanTarget = contactTarget.trim()
        if (cleanAlias.isBlank() || cleanTarget.isBlank()) return
        try {
            context.dataStore.edit { prefs ->
                val currentJson = prefs[PreferencesKeys.CONTACT_ALIASES_JSON]
                val currentMap = if (!currentJson.isNullOrBlank()) {
                    try {
                        val type = object : TypeToken<Map<String, String>>() {}.type
                        Gson().fromJson<Map<String, String>>(currentJson, type)?.toMutableMap() ?: mutableMapOf()
                    } catch (e: Exception) {
                        mutableMapOf()
                    }
                } else mutableMapOf()
                currentMap[cleanAlias] = cleanTarget
                prefs[PreferencesKeys.CONTACT_ALIASES_JSON] = Gson().toJson(currentMap)
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to set contact alias", e)
        }
    }

    override suspend fun removeContactAlias(alias: String) {
        val cleanAlias = alias.trim().lowercase()
        try {
            context.dataStore.edit { prefs ->
                val currentJson = prefs[PreferencesKeys.CONTACT_ALIASES_JSON]
                if (!currentJson.isNullOrBlank()) {
                    val type = object : TypeToken<Map<String, String>>() {}.type
                    val currentMap = Gson().fromJson<Map<String, String>>(currentJson, type)?.toMutableMap() ?: mutableMapOf()
                    currentMap.remove(cleanAlias)
                    prefs[PreferencesKeys.CONTACT_ALIASES_JSON] = Gson().toJson(currentMap)
                }
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to remove contact alias", e)
        }
    }

    override fun getCustomModels(): Flow<List<String>> {
        return safePreferencesFlow.map { prefs ->
            val set = prefs[PreferencesKeys.CUSTOM_MODELS] ?: emptySet()
            set.toList().sorted()
        }.catch { emit(emptyList()) }
    }

    override suspend fun addCustomModel(model: String) {
        val clean = model.trim()
        if (clean.isBlank()) return
        try {
            context.dataStore.edit { prefs ->
                val current = prefs[PreferencesKeys.CUSTOM_MODELS]?.toMutableSet() ?: mutableSetOf()
                current.add(clean)
                prefs[PreferencesKeys.CUSTOM_MODELS] = current
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to add custom model", e)
        }
    }

    override suspend fun removeCustomModel(model: String) {
        val clean = model.trim()
        try {
            context.dataStore.edit { prefs ->
                val current = prefs[PreferencesKeys.CUSTOM_MODELS]?.toMutableSet() ?: mutableSetOf()
                current.remove(clean)
                prefs[PreferencesKeys.CUSTOM_MODELS] = current
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to remove custom model", e)
        }
    }

    override fun getAutoBackupEnabled(): Flow<Boolean> {
        return safePreferencesFlow.map { preferences ->
            preferences[PreferencesKeys.AUTO_BACKUP] ?: false
        }.catch { emit(false) }
    }

    override suspend fun setAutoBackupEnabled(enabled: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.AUTO_BACKUP] = enabled
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to set autoBackup", e)
        }
    }

    override fun getCustomBarcodes(): Flow<Map<String, String>> {
        return safePreferencesFlow.map { prefs ->
            val json = prefs[PreferencesKeys.CUSTOM_BARCODES_JSON]
            if (json.isNullOrBlank()) {
                emptyMap()
            } else {
                try {
                    val type = object : TypeToken<Map<String, String>>() {}.type
                    Gson().fromJson<Map<String, String>>(json, type) ?: emptyMap()
                } catch (e: Exception) {
                    emptyMap()
                }
            }
        }.distinctUntilChanged()
    }

    override suspend fun saveCustomBarcode(barcode: String, resultJson: String) {
        val cleanBarcode = barcode.trim()
        if (cleanBarcode.isBlank()) return
        try {
            context.dataStore.edit { prefs ->
                val currentJson = prefs[PreferencesKeys.CUSTOM_BARCODES_JSON]
                val currentMap = if (!currentJson.isNullOrBlank()) {
                    try {
                        val type = object : TypeToken<Map<String, String>>() {}.type
                        Gson().fromJson<Map<String, String>>(currentJson, type)?.toMutableMap() ?: mutableMapOf()
                    } catch (e: Exception) {
                        mutableMapOf()
                    }
                } else mutableMapOf()
                currentMap[cleanBarcode] = resultJson
                prefs[PreferencesKeys.CUSTOM_BARCODES_JSON] = Gson().toJson(currentMap)
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to save custom barcode", e)
        }
    }

    override suspend fun saveAllContactAliases(aliases: Map<String, String>) {
        try {
            context.dataStore.edit { prefs ->
                prefs[PreferencesKeys.CONTACT_ALIASES_JSON] = Gson().toJson(aliases)
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to save all contact aliases", e)
        }
    }

    override suspend fun saveAllCustomBarcodes(barcodes: Map<String, String>) {
        try {
            context.dataStore.edit { prefs ->
                prefs[PreferencesKeys.CUSTOM_BARCODES_JSON] = Gson().toJson(barcodes)
            }
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to save all custom barcodes", e)
        }
    }

    override suspend fun restoreApiUsage(date: String, promptTokens: Int, candidateTokens: Int, totalTokens: Int, requests: Int, externalRequests: Int) {
        try {
            val targetDate = if (date.isNotBlank()) date else AppDate.todayIso()
            context.dataStore.edit { prefs ->
                prefs[PreferencesKeys.DAILY_USAGE_DATE] = targetDate
                prefs[PreferencesKeys.DAILY_USAGE_PROMPT_TOKENS] = promptTokens.coerceAtLeast(0)
                prefs[PreferencesKeys.DAILY_USAGE_CANDIDATE_TOKENS] = candidateTokens.coerceAtLeast(0)
                prefs[PreferencesKeys.DAILY_USAGE_TOTAL_TOKENS] = totalTokens.coerceAtLeast(0)
                prefs[PreferencesKeys.DAILY_USAGE_REQUEST_COUNT] = requests.coerceAtLeast(0)
                prefs[PreferencesKeys.EXTERNAL_REQUEST_DATE] = targetDate
                prefs[PreferencesKeys.EXTERNAL_REQUEST_COUNT] = externalRequests.coerceAtLeast(0)
            }
            savePersistentQuotaBackup(targetDate, promptTokens, candidateTokens, totalTokens, requests, externalRequests)
        } catch (e: Throwable) {
            Log.e("UserPrefs", "Failed to restore API usage", e)
        }
    }

    private fun savePersistentQuotaBackup(date: String, promptTokens: Int, candidateTokens: Int, totalTokens: Int, requestCount: Int, externalRequests: Int) {
        val json = """{"date":"$date","promptTokens":$promptTokens,"candidateTokens":$candidateTokens,"totalTokens":$totalTokens,"requestCount":$requestCount,"externalRequests":$externalRequests}"""
        try {
            val dir = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS), "MacroBite")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, ".daily_api_sync.json")
            file.writeText(json)
        } catch (_: Throwable) {}
        try {
            val internalFile = File(context.filesDir, ".daily_api_sync.json")
            internalFile.writeText(json)
        } catch (_: Throwable) {}
    }

    private fun readPersistentQuotaBackup(): Map<String, Any>? {
        return try {
            val dir = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS), "MacroBite")
            val extFile = File(dir, ".daily_api_sync.json")
            val targetFile = if (extFile.exists()) extFile else File(context.filesDir, ".daily_api_sync.json")
            if (targetFile.exists()) {
                val json = targetFile.readText()
                val type = object : TypeToken<Map<String, Any>>() {}.type
                Gson().fromJson<Map<String, Any>>(json, type)
            } else null
        } catch (_: Throwable) { null }
    }
}

