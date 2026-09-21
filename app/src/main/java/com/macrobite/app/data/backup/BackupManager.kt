package com.macrobite.app.data.backup

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.macrobite.app.data.local.AppDatabase
import com.macrobite.app.data.local.entity.ChatMessageEntity
import com.macrobite.app.data.local.entity.CustomFoodEntity
import com.macrobite.app.data.local.entity.MealEntity
import com.macrobite.app.data.local.entity.WeightEntity
import com.macrobite.app.domain.model.UserTargets
import com.macrobite.app.domain.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class BackupMetadata(
    val app: String = "MacroBite",
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val exportedAt: String = ""
)

data class BackupSettingsDto(
    val targets: UserTargets,
    val darkMode: String,
    val themeColor: String,
    val geminiApiKey: String,
    val geminiModel: String,
    val useGemini: Boolean = true,
    val dailyApiTokens: Int = 0,
    val dailyApiPromptTokens: Int = 0,
    val dailyApiCandidateTokens: Int = 0,
    val dailyApiRequests: Int = 0,
    val externalRequestCount: Int = 0,
    val dailyApiDate: String = "",
    val raayaName: String,
    val raayaAvatar: String,
    val raayaAvatarBase64: String = "",
    val raayaPersonality: String,
    val raayaAutoLog: Boolean,
    val raayaIncludeMicros: Boolean,
    val raayaDietaryNotes: String,
    val raayaWebSearchEnabled: Boolean,
    val voiceStyle: String,
    val replyStyle: String,
    val notificationPrefsJson: String,
    val inAppNotificationsEnabled: Boolean = true,
    val stepGoal: Int = 10000,
    val googleHealthSyncEnabled: Boolean = false,
    val contactAliases: Map<String, String> = emptyMap(),
    val customModels: List<String> = emptyList(),
    val autoBackup: Boolean = true,
    val customBarcodes: Map<String, String> = emptyMap()
)

data class BackupDataDto(
    val meals: List<MealEntity>,
    val weights: List<WeightEntity>,
    val customFoods: List<CustomFoodEntity>,
    val chatMessages: List<ChatMessageEntity>
)

data class MacroBiteBackupPayload(
    val metadata: BackupMetadata,
    val settings: BackupSettingsDto,
    val data: BackupDataDto
)

data class RestoreResult(
    val isSuccess: Boolean,
    val message: String,
    val mealsCount: Int = 0,
    val weightsCount: Int = 0,
    val chatMessagesCount: Int = 0
)

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val preferencesRepository: UserPreferencesRepository,
    private val gson: Gson
) {

    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val targets = preferencesRepository.getTargets().first()
        val darkMode = preferencesRepository.getDarkModePreference().first()
        val themeColor = preferencesRepository.getThemeColor().first()
        val geminiApiKey = preferencesRepository.getGeminiApiKey().first()
        val geminiModel = preferencesRepository.getGeminiModel().first()
        val useGemini = preferencesRepository.getUseGemini().first()
        val raayaName = preferencesRepository.getRaayaName().first()
        val raayaAvatar = preferencesRepository.getRaayaAvatar().first()
        val raayaPersonality = preferencesRepository.getRaayaPersonality().first()
        val raayaAutoLog = preferencesRepository.getRaayaAutoLog().first()
        val raayaIncludeMicros = preferencesRepository.getRaayaIncludeMicros().first()
        val raayaDietaryNotes = preferencesRepository.getRaayaDietaryNotes().first()
        val raayaWebSearchEnabled = preferencesRepository.getRaayaWebSearchEnabled().first()
        val voiceStyle = preferencesRepository.getVoiceStyle().first()
        val replyStyle = preferencesRepository.getReplyStyle().first()
        val notifPrefs = preferencesRepository.getNotificationPreferences().firstOrNull()
        val notifJson = if (notifPrefs != null) gson.toJson(notifPrefs) else ""
        val inAppNotifs = preferencesRepository.getInAppNotificationsEnabled().first()
        val stepGoal = preferencesRepository.getStepGoal().first()
        val googleHealthSync = preferencesRepository.getGoogleHealthSyncEnabled().first()
        val contactAliases = preferencesRepository.getContactAliases().first()
        val customModels = preferencesRepository.getCustomModels().first()
        val autoBackup = preferencesRepository.getAutoBackupEnabled().first()
        val customBarcodes = preferencesRepository.getCustomBarcodes().first()
        val dailyUsage = preferencesRepository.getDailyApiUsage().first()

        // Direct DAO queries to guarantee 100% full snapshot without empty Flow races
        val meals = database.mealDao().getAllMealsDirect()
        val weights = database.weightDao().getAllWeightsDirect()
        val customFoods = database.customFoodDao().getAllCustomFoodsDirect()
        val chatMessages = database.chatDao().getAllMessagesDirect()

        // Encode custom avatar image to Base64 so it can be restored on any device or fresh install
        var avatarBase64 = ""
        if (raayaAvatar.isNotBlank()) {
            try {
                val cleanPath = raayaAvatar.removePrefix("file://").removePrefix("file:")
                val file = File(cleanPath)
                if (file.exists() && file.isFile) {
                    val bytes = file.readBytes()
                    avatarBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                }
            } catch (e: Throwable) {
                Log.e("BackupManager", "Could not encode avatar image to Base64", e)
            }
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val metadata = BackupMetadata(
            exportedAt = sdf.format(Date())
        )

        val settingsDto = BackupSettingsDto(
            targets = targets,
            darkMode = darkMode,
            themeColor = themeColor,
            geminiApiKey = geminiApiKey,
            geminiModel = geminiModel,
            useGemini = useGemini,
            dailyApiTokens = dailyUsage.totalTokens,
            dailyApiPromptTokens = dailyUsage.promptTokens,
            dailyApiCandidateTokens = dailyUsage.candidateTokens,
            dailyApiRequests = dailyUsage.requestCount,
            externalRequestCount = dailyUsage.externalRequestCount,
            dailyApiDate = dailyUsage.date,
            raayaName = raayaName,
            raayaAvatar = raayaAvatar,
            raayaAvatarBase64 = avatarBase64,
            raayaPersonality = raayaPersonality,
            raayaAutoLog = raayaAutoLog,
            raayaIncludeMicros = raayaIncludeMicros,
            raayaDietaryNotes = raayaDietaryNotes,
            raayaWebSearchEnabled = raayaWebSearchEnabled,
            voiceStyle = voiceStyle,
            replyStyle = replyStyle,
            notificationPrefsJson = notifJson,
            inAppNotificationsEnabled = inAppNotifs,
            stepGoal = stepGoal,
            googleHealthSyncEnabled = googleHealthSync,
            contactAliases = contactAliases,
            customModels = customModels,
            autoBackup = autoBackup,
            customBarcodes = customBarcodes
        )

        val dataDto = BackupDataDto(
            meals = meals,
            weights = weights,
            customFoods = customFoods,
            chatMessages = chatMessages
        )

        val payload = MacroBiteBackupPayload(metadata, settingsDto, dataDto)
        GsonBuilder().setPrettyPrinting().create().toJson(payload)
    }

    suspend fun saveBackupToLocalStorage(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val json = createBackupJson()
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "MacroBite_Backup_$timeStamp.json"

            // Save to Documents/MacroBite so it permanently survives app uninstallation
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/MacroBite")
                }
                val uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(json.toByteArray())
                    }
                }
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "MacroBite")
                if (!dir.exists()) dir.mkdirs()
                File(dir, fileName).writeText(json)
            }

            // Also keep latest in internal backups folder
            val backupDir = File(context.filesDir, "backups").apply { mkdirs() }
            File(backupDir, "latest_backup.json").writeText(json)

            Pair(true, "Saved securely to Documents/MacroBite/$fileName (survives app uninstallation)")
        } catch (e: Throwable) {
            Log.e("BackupManager", "Error saving backup", e)
            Pair(false, "Failed to save backup: ${e.message}")
        }
    }

    suspend fun performAutoBackup(): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = createBackupJson()
            val fileName = "MacroBite_AutoBackup.json"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // To overwrite using MediaStore, we first need to query and delete the existing file
                val collection = MediaStore.Files.getContentUri("external")
                val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
                val selectionArgs = arrayOf(fileName, Environment.DIRECTORY_DOCUMENTS + "/MacroBite/")
                
                val cursor = context.contentResolver.query(collection, arrayOf(MediaStore.MediaColumns._ID), selection, selectionArgs, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val idColumn = it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                        val id = it.getLong(idColumn)
                        val deleteUri = android.content.ContentUris.withAppendedId(collection, id)
                        context.contentResolver.delete(deleteUri, null, null)
                    }
                }
                
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/MacroBite")
                }
                val uri = context.contentResolver.insert(collection, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(json.toByteArray())
                    }
                }
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "MacroBite")
                if (!dir.exists()) dir.mkdirs()
                File(dir, fileName).writeText(json)
            }

            val backupDir = File(context.filesDir, "backups").apply { mkdirs() }
            File(backupDir, "latest_backup.json").writeText(json)
            
            true
        } catch (e: Throwable) {
            Log.e("BackupManager", "Error performing auto backup", e)
            false
        }
    }

    suspend fun getGoogleDriveUploadIntent(): Intent = withContext(Dispatchers.IO) {
        val json = createBackupJson()
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "MacroBite_Backup.json"

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, fileName)
        file.writeText(json)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "MacroBite Backup ($timeStamp)")
            putExtra(Intent.EXTRA_TEXT, "MacroBite backup containing all settings and nutrition logs.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Check if Google Drive package is installed
        val driveInstalled = try {
            context.packageManager.getPackageInfo("com.google.android.apps.docs", 0)
            true
        } catch (_: Throwable) {
            false
        }

        if (driveInstalled) {
            intent.setPackage("com.google.android.apps.docs")
        }

        Intent.createChooser(intent, "Upload to Google Drive / Cloud")
    }

    suspend fun restoreBackup(jsonString: String): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val payload = gson.fromJson(jsonString, MacroBiteBackupPayload::class.java)
                ?: return@withContext RestoreResult(false, "Invalid backup format: unable to parse data")

            // 1. Restore Settings
            val settings = payload.settings
            preferencesRepository.saveTargets(settings.targets)
            preferencesRepository.setUseGemini(settings.useGemini)
            if (settings.themeColor.isNotBlank()) preferencesRepository.setThemeColor(settings.themeColor)
            if (settings.darkMode.isNotBlank()) preferencesRepository.setDarkModePreference(settings.darkMode)
            if (settings.geminiApiKey.isNotBlank()) preferencesRepository.setGeminiApiKey(settings.geminiApiKey)
            if (settings.geminiModel.isNotBlank()) preferencesRepository.setGeminiModel(settings.geminiModel)
            if (settings.raayaName.isNotBlank()) preferencesRepository.setRaayaName(settings.raayaName)

            // Restore custom profile photo if encoded in Base64
            if (!settings.raayaAvatarBase64.isNullOrBlank()) {
                try {
                    val dir = File(context.filesDir, "raaya_avatars")
                    if (!dir.exists()) dir.mkdirs()
                    val destFile = File(dir, "raaya_avatar_restored_${System.currentTimeMillis()}.jpg")
                    val bytes = Base64.decode(settings.raayaAvatarBase64, Base64.NO_WRAP)
                    destFile.writeBytes(bytes)
                    preferencesRepository.setRaayaAvatar(destFile.absolutePath)
                } catch (e: Throwable) {
                    Log.e("BackupManager", "Error restoring avatar from Base64", e)
                    if (settings.raayaAvatar.isNotBlank()) preferencesRepository.setRaayaAvatar(settings.raayaAvatar)
                }
            } else if (settings.raayaAvatar.isNotBlank()) {
                preferencesRepository.setRaayaAvatar(settings.raayaAvatar)
            }

            if (settings.raayaPersonality.isNotBlank()) preferencesRepository.setRaayaPersonality(settings.raayaPersonality)
            preferencesRepository.setRaayaAutoLog(settings.raayaAutoLog)
            preferencesRepository.setRaayaIncludeMicros(settings.raayaIncludeMicros)
            preferencesRepository.setRaayaDietaryNotes(settings.raayaDietaryNotes)
            preferencesRepository.setRaayaWebSearchEnabled(settings.raayaWebSearchEnabled)
            if (settings.voiceStyle.isNotBlank()) preferencesRepository.setVoiceStyle(settings.voiceStyle)
            if (settings.replyStyle.isNotBlank()) preferencesRepository.setReplyStyle(settings.replyStyle)
            preferencesRepository.setInAppNotificationsEnabled(settings.inAppNotificationsEnabled)
            preferencesRepository.setStepGoal(settings.stepGoal)
            preferencesRepository.setGoogleHealthSyncEnabled(settings.googleHealthSyncEnabled)
            preferencesRepository.setAutoBackupEnabled(settings.autoBackup)

            if (settings.notificationPrefsJson.isNotBlank()) {
                try {
                    val notifPrefs = gson.fromJson(settings.notificationPrefsJson, com.macrobite.app.domain.model.NotificationPreferences::class.java)
                    if (notifPrefs != null) preferencesRepository.saveNotificationPreferences(notifPrefs)
                } catch (_: Throwable) {}
            }
            if (!settings.customModels.isNullOrEmpty()) {
                settings.customModels.forEach { model ->
                    preferencesRepository.addCustomModel(model)
                }
            }
            if (settings.contactAliases.isNotEmpty()) {
                preferencesRepository.saveAllContactAliases(settings.contactAliases)
            }
            if (settings.customBarcodes.isNotEmpty()) {
                preferencesRepository.saveAllCustomBarcodes(settings.customBarcodes)
            }
            if (settings.dailyApiDate.isNotBlank()) {
                preferencesRepository.restoreApiUsage(
                    settings.dailyApiDate,
                    settings.dailyApiPromptTokens,
                    settings.dailyApiCandidateTokens,
                    settings.dailyApiTokens,
                    settings.dailyApiRequests,
                    settings.externalRequestCount
                )
            }

            // 2. Restore Database Entities
            val data = payload.data
            var mealsCount = 0
            if (data.meals.isNotEmpty()) {
                database.mealDao().insertMeals(data.meals)
                mealsCount = data.meals.size
            }
            var weightsCount = 0
            if (data.weights.isNotEmpty()) {
                database.weightDao().insertWeights(data.weights)
                weightsCount = data.weights.size
            }
            if (data.customFoods.isNotEmpty()) {
                database.customFoodDao().insertCustomFoods(data.customFoods)
            }
            var chatCount = 0
            if (data.chatMessages.isNotEmpty()) {
                database.chatDao().insertMessages(data.chatMessages)
                chatCount = data.chatMessages.size
            }

            RestoreResult(
                isSuccess = true,
                message = "Backup restored successfully! Restored $mealsCount meals, $chatCount chat messages, custom profile, and all settings.",
                mealsCount = mealsCount,
                weightsCount = weightsCount,
                chatMessagesCount = chatCount
            )
        } catch (e: Throwable) {
            Log.e("BackupManager", "Error restoring backup", e)
            RestoreResult(false, "Restore failed: ${e.message}")
        }
    }

    suspend fun restoreLatestBackupAuto(): RestoreResult = withContext(Dispatchers.IO) {
        try {
            data class Candidate(
                val name: String,
                val lastModified: Long,
                val read: () -> String?
            )
            val candidates = mutableListOf<Candidate>()

            // 1. Check internal backups folder
            val internalDir = File(context.filesDir, "backups")
            if (internalDir.exists()) {
                internalDir.listFiles()?.forEach { f ->
                    if (f.isFile && f.name.endsWith(".json", ignoreCase = true) && !f.name.startsWith(".")) {
                        candidates.add(Candidate(f.name, f.lastModified()) {
                            try { f.readText() } catch (_: Throwable) { null }
                        })
                    }
                }
            }

            // 2. Check Documents/MacroBite
            val docsMacroDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "MacroBite")
            if (docsMacroDir.exists()) {
                docsMacroDir.listFiles()?.forEach { f ->
                    if (f.isFile && f.name.endsWith(".json", ignoreCase = true) && !f.name.startsWith(".")) {
                        candidates.add(Candidate(f.name, f.lastModified()) {
                            try { f.readText() } catch (_: Throwable) { null }
                        })
                    }
                }
            }

            // 3. Check Downloads/MacroBite and Downloads
            val dlMacroDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "MacroBite")
            if (dlMacroDir.exists()) {
                dlMacroDir.listFiles()?.forEach { f ->
                    if (f.isFile && f.name.endsWith(".json", ignoreCase = true) && !f.name.startsWith(".")) {
                        candidates.add(Candidate(f.name, f.lastModified()) {
                            try { f.readText() } catch (_: Throwable) { null }
                        })
                    }
                }
            }

            val dlDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (dlDir.exists()) {
                dlDir.listFiles()?.forEach { f ->
                    if (f.isFile && f.name.contains("MacroBite", ignoreCase = true) && f.name.endsWith(".json", ignoreCase = true)) {
                        candidates.add(Candidate(f.name, f.lastModified()) {
                            try { f.readText() } catch (_: Throwable) { null }
                        })
                    }
                }
            }

            // 4. Check Documents root
            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (docsDir.exists()) {
                docsDir.listFiles()?.forEach { f ->
                    if (f.isFile && f.name.contains("MacroBite", ignoreCase = true) && f.name.endsWith(".json", ignoreCase = true)) {
                        candidates.add(Candidate(f.name, f.lastModified()) {
                            try { f.readText() } catch (_: Throwable) { null }
                        })
                    }
                }
            }

            // 5. Query MediaStore for any MacroBite JSON backup
            try {
                val collection = MediaStore.Files.getContentUri("external")
                val projection = arrayOf(
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.DATE_MODIFIED
                )
                val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE '%MacroBite%' OR ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE '%macrobite%'"
                val cursor = context.contentResolver.query(collection, projection, selection, null, "${MediaStore.MediaColumns.DATE_MODIFIED} DESC")
                cursor?.use { c ->
                    val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val modCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                    while (c.moveToNext()) {
                        val id = c.getLong(idCol)
                        val name = c.getString(nameCol) ?: "unknown.json"
                        val dateMod = c.getLong(modCol) * 1000L
                        if (name.endsWith(".json", ignoreCase = true) && !name.startsWith(".")) {
                            val contentUri = ContentUris.withAppendedId(collection, id)
                            candidates.add(Candidate(name, dateMod) {
                                try {
                                    context.contentResolver.openInputStream(contentUri)?.use { it.bufferedReader().readText() }
                                } catch (_: Throwable) { null }
                            })
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.e("BackupManager", "Error querying MediaStore for backups", e)
            }

            // Deduplicate and sort newest first
            val sortedCandidates = candidates
                .distinctBy { "${it.name}_${it.lastModified}" }
                .sortedByDescending { it.lastModified }

            if (sortedCandidates.isEmpty()) {
                return@withContext RestoreResult(
                    isSuccess = false,
                    message = "No MacroBite backup files found in phone storage (Documents or Downloads)."
                )
            }

            for (candidate in sortedCandidates) {
                val content = candidate.read()
                if (!content.isNullOrBlank() && (content.contains("MacroBite") || content.contains("metadata") || content.contains("settings"))) {
                    val restoreResult = restoreBackup(content)
                    if (restoreResult.isSuccess) {
                        return@withContext restoreResult.copy(
                            message = "Auto-detected backup file (${candidate.name}) and restored successfully!\n" +
                                      "Restored ${restoreResult.mealsCount} meals, ${restoreResult.chatMessagesCount} chat messages, custom profile, and all targets & settings."
                        )
                    }
                }
            }

            RestoreResult(
                isSuccess = false,
                message = "Backup files were found, but could not be parsed. Please create a new backup."
            )
        } catch (e: Throwable) {
            Log.e("BackupManager", "Error auto-restoring backup", e)
            RestoreResult(isSuccess = false, message = "Auto restore failed: ${e.message}")
        }
    }
}
