package com.macrobite.app.data.backup

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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
    val raayaName: String,
    val raayaAvatar: String,
    val raayaPersonality: String,
    val raayaAutoLog: Boolean,
    val raayaIncludeMicros: Boolean,
    val raayaDietaryNotes: String,
    val raayaWebSearchEnabled: Boolean,
    val voiceStyle: String,
    val replyStyle: String,
    val notificationPrefsJson: String,
    val customModels: List<String> = emptyList()
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
        val customModels = preferencesRepository.getCustomModels().first()

        val meals = database.mealDao().getAllMeals().first()
        val weights = database.weightDao().getAllWeights().first()
        val customFoods = database.customFoodDao().getAllCustomFoods().first()
        val chatMessages = database.chatDao().getAllMessages().first()

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
            raayaName = raayaName,
            raayaAvatar = raayaAvatar,
            raayaPersonality = raayaPersonality,
            raayaAutoLog = raayaAutoLog,
            raayaIncludeMicros = raayaIncludeMicros,
            raayaDietaryNotes = raayaDietaryNotes,
            raayaWebSearchEnabled = raayaWebSearchEnabled,
            voiceStyle = voiceStyle,
            replyStyle = replyStyle,
            notificationPrefsJson = notifJson,
            customModels = customModels
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
            if (settings.themeColor.isNotBlank()) preferencesRepository.setThemeColor(settings.themeColor)
            if (settings.darkMode.isNotBlank()) preferencesRepository.setDarkModePreference(settings.darkMode)
            if (settings.geminiApiKey.isNotBlank()) preferencesRepository.setGeminiApiKey(settings.geminiApiKey)
            if (settings.geminiModel.isNotBlank()) preferencesRepository.setGeminiModel(settings.geminiModel)
            if (settings.raayaName.isNotBlank()) preferencesRepository.setRaayaName(settings.raayaName)
            if (settings.raayaAvatar.isNotBlank()) preferencesRepository.setRaayaAvatar(settings.raayaAvatar)
            if (settings.raayaPersonality.isNotBlank()) preferencesRepository.setRaayaPersonality(settings.raayaPersonality)
            preferencesRepository.setRaayaAutoLog(settings.raayaAutoLog)
            preferencesRepository.setRaayaIncludeMicros(settings.raayaIncludeMicros)
            preferencesRepository.setRaayaDietaryNotes(settings.raayaDietaryNotes)
            preferencesRepository.setRaayaWebSearchEnabled(settings.raayaWebSearchEnabled)
            if (settings.voiceStyle.isNotBlank()) preferencesRepository.setVoiceStyle(settings.voiceStyle)
            if (settings.replyStyle.isNotBlank()) preferencesRepository.setReplyStyle(settings.replyStyle)
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
                message = "Backup restored successfully! Restored $mealsCount meals, $chatCount chat messages, and all preferences.",
                mealsCount = mealsCount,
                weightsCount = weightsCount,
                chatMessagesCount = chatCount
            )
        } catch (e: Throwable) {
            Log.e("BackupManager", "Error restoring backup", e)
            RestoreResult(false, "Restore failed: ${e.message}")
        }
    }
}
