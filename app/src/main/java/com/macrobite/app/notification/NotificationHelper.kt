package com.macrobite.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.macrobite.app.MainActivity
import com.macrobite.app.R
import com.macrobite.app.domain.model.AppDate
import com.macrobite.app.domain.model.MealCategory
import com.macrobite.app.domain.model.MealEntry
import com.macrobite.app.domain.model.NotificationPreferences
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

object NotificationHelper {

    const val CHANNEL_MEAL_REMINDERS = "meal_reminders_v2"
    const val CHANNEL_MACRO_CHECKIN = "macro_checkin_v2"
    const val CHANNEL_FOOD_LOGS = "food_logs_v1"
    const val CHANNEL_CHAT_RESPONSES = "chat_responses_v1"
    const val CHANNEL_TOKEN_ALERTS = "token_alerts_v1"

    private const val NOTIFICATION_ID_BASE = 2000
    const val NOTIFICATION_ID_TEST_MEAL = 2099
    const val NOTIFICATION_ID_TEST_FOOD_LOG = 2098
    const val NOTIFICATION_ID_TEST_CHAT = 2097
    const val NOTIFICATION_ID_TEST_TOKEN = 2096

    const val NOTIFICATION_ID_FOOD_LOG = 3001
    const val NOTIFICATION_ID_CHAT_RESPONSE = 3002
    const val NOTIFICATION_ID_TOKEN_50 = 3050
    const val NOTIFICATION_ID_TOKEN_75 = 3075
    const val NOTIFICATION_ID_TOKEN_90 = 3090

    // Signature App Icon Colors: Black background (#121212) with Amber-Yellow icon (#FFC107)
    val NOTIFICATION_ICON_COLOR: Int = android.graphics.Color.parseColor("#FFC107")
    val NOTIFICATION_BG_COLOR: Int = android.graphics.Color.parseColor("#121212")

    @Suppress("UNUSED_PARAMETER")
    fun updateThemeColor(colorId: String) {
        // Maintained for caller compatibility; notification icon color is strictly locked to the app icon
    }

    private var cachedAppIconBitmap: Bitmap? = null

    /**
     * Generates a crisp, high-resolution bitmap matching the official MacroBite app icon:
     * A sleek dark circular background (#121212) with the vibrant yellow (#FFC107) fork & knife emblem.
     */
    fun getAppIconBitmap(context: Context): Bitmap {
        cachedAppIconBitmap?.let { if (!it.isRecycled) return it }

        val size = (context.resources.displayMetrics.density * 64).toInt().coerceAtLeast(128)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Circular black background matching app icon
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = NOTIFICATION_BG_COLOR
            style = Paint.Style.FILL
        }
        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, bgPaint)

        // 2. Yellow fork & knife emblem
        val foregroundDrawable = ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)
        if (foregroundDrawable != null) {
            foregroundDrawable.setBounds(0, 0, size, size)
            foregroundDrawable.draw(canvas)
        }

        cachedAppIconBitmap = bitmap
        return bitmap
    }

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            // 1. Meal Reminders Channel
            val mealChannel = NotificationChannel(
                CHANNEL_MEAL_REMINDERS,
                "Meal & Logging Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Timely alerts to log your Breakfast, Lunch, Snacks, and Dinner."
                enableLights(true)
                lightColor = NOTIFICATION_ICON_COLOR
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 180, 80, 180)
                setSound(defaultSoundUri, audioAttributes)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 2. Macro Goals & Evening Review Channel
            val summaryChannel = NotificationChannel(
                CHANNEL_MACRO_CHECKIN,
                "Macro Goals & Daily Review",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Evening check-ins to review your daily macros, calories, and targets."
                enableLights(true)
                lightColor = NOTIFICATION_ICON_COLOR
                enableVibration(true)
                setSound(defaultSoundUri, audioAttributes)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 3. Food Logging Confirmations Channel (Minimal)
            val foodLogChannel = NotificationChannel(
                CHANNEL_FOOD_LOGS,
                "Food Log Confirmations",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Minimal status bar alerts when meals and foods are logged."
                enableLights(true)
                lightColor = NOTIFICATION_ICON_COLOR
                setShowBadge(true)
            }

            // 4. AI Chat Responses Channel
            val chatResponseChannel = NotificationChannel(
                CHANNEL_CHAT_RESPONSES,
                "AI Chat Responses",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when your AI nutritionist finishes replying while you are away from the chat."
                enableLights(true)
                lightColor = NOTIFICATION_ICON_COLOR
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 150, 100, 150)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 5. Token & API Quota Alerts Channel
            val tokenAlertChannel = NotificationChannel(
                CHANNEL_TOKEN_ALERTS,
                "API Quota & Token Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when reaching 50%, 75%, and 90% of daily API token limits."
                enableLights(true)
                lightColor = NOTIFICATION_ICON_COLOR
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannel(mealChannel)
            notificationManager.createNotificationChannel(summaryChannel)
            notificationManager.createNotificationChannel(foodLogChannel)
            notificationManager.createNotificationChannel(chatResponseChannel)
            notificationManager.createNotificationChannel(tokenAlertChannel)
        }
    }

    /**
     * Minimal Notification: Confirms food logged (Breakfast, Lunch, Dinner, Snack).
     * Header displays only the clean app name and time (MacroBite • now).
     */
    fun showFoodLoggedNotification(context: Context, meal: MealEntry) {
        createNotificationChannels(context)

        val categoryTitle = when (meal.category) {
            MealCategory.BREAKFAST -> "Breakfast Logged"
            MealCategory.LUNCH -> "Lunch Logged"
            MealCategory.DINNER -> "Dinner Logged"
            MealCategory.SNACKS -> "Snack Logged"
        }

        val macroSummary = "${meal.foodName} • ${meal.calories} kcal (${meal.protein.roundToInt()}g P • ${meal.carbs.roundToInt()}g C • ${meal.fats.roundToInt()}g F)"
        val shortContent = "${meal.foodName} • ${meal.calories} kcal"

        val openDashboardPendingIntent = createPendingIntent(context, targetPage = 0, requestCode = NOTIFICATION_ID_FOOD_LOG)
        val openChatPendingIntent = createPendingIntent(context, targetPage = 1, requestCode = NOTIFICATION_ID_FOOD_LOG + 10)
        val builder = NotificationCompat.Builder(context, CHANNEL_FOOD_LOGS)
            .setSmallIcon(R.drawable.ic_notification_app_icon)
            .setColor(NOTIFICATION_BG_COLOR)
            .setContentTitle(categoryTitle)
            .setContentText(shortContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(macroSummary))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(openDashboardPendingIntent)
            .addAction(R.drawable.ic_nav_restaurant, "View Log", openDashboardPendingIntent)
            .addAction(R.drawable.ic_auto_awesome, "Chat AI", openChatPendingIntent)

        safelyNotify(context, NOTIFICATION_ID_FOOD_LOG, builder)
    }

    /**
     * Minimal Notification: Confirms multiple items logged at once (e.g. from photo scan).
     */
    fun showMultipleFoodsLoggedNotification(context: Context, meals: List<MealEntry>) {
        if (meals.isEmpty()) return
        createNotificationChannels(context)

        val category = meals.first().category
        val categoryTitle = when (category) {
            MealCategory.BREAKFAST -> "Breakfast Logged"
            MealCategory.LUNCH -> "Lunch Logged"
            MealCategory.DINNER -> "Dinner Logged"
            MealCategory.SNACKS -> "Snacks Logged"
        }

        val totalCalories = meals.sumOf { it.calories }
        val itemsSummary = meals.joinToString(", ") { it.foodName }
        val shortContent = "${meals.size} items logged • $totalCalories kcal"
        val fullContent = "$itemsSummary • $totalCalories kcal"

        val openDashboardPendingIntent = createPendingIntent(context, targetPage = 0, requestCode = NOTIFICATION_ID_FOOD_LOG)
        val builder = NotificationCompat.Builder(context, CHANNEL_FOOD_LOGS)
            .setSmallIcon(R.drawable.ic_notification_app_icon)
            .setColor(NOTIFICATION_BG_COLOR)
            .setContentTitle(categoryTitle)
            .setContentText(shortContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(fullContent))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(openDashboardPendingIntent)
            .addAction(R.drawable.ic_nav_restaurant, "View Log", openDashboardPendingIntent)

        safelyNotify(context, NOTIFICATION_ID_FOOD_LOG, builder)
    }

    /**
     * Minimal Notification: Sent when AI generates a response and user is away from chat.
     */
    fun showChatResponseNotification(
        context: Context,
        aiName: String = "AI Assistant",
        responseSnippet: String
    ) {
        createNotificationChannels(context)

        val openChatPendingIntent = createPendingIntent(context, targetPage = 1, requestCode = NOTIFICATION_ID_CHAT_RESPONSE)
        val cleanSnippet = responseSnippet.trim().ifBlank { "Your nutrition breakdown is ready. Tap to view." }
        val builder = NotificationCompat.Builder(context, CHANNEL_CHAT_RESPONSES)
            .setSmallIcon(R.drawable.ic_notification_app_icon)
            .setColor(NOTIFICATION_BG_COLOR)
            .setContentTitle("Response from $aiName")
            .setContentText(cleanSnippet)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cleanSnippet))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(openChatPendingIntent)
            .addAction(R.drawable.ic_auto_awesome, "Open Chat", openChatPendingIntent)

        safelyNotify(context, NOTIFICATION_ID_CHAT_RESPONSE, builder)
    }

    /**
     * Minimal Notification: Sent when token quota reaches 50%, 75%, or 90%.
     */
    fun showTokenUsageAlert(
        context: Context,
        thresholdPercent: Int,
        totalTokens: Int,
        dailyLimit: Int,
        requestCount: Int = 0
    ) {
        createNotificationChannels(context)

        val numberFmt = NumberFormat.getNumberInstance(Locale.US)
        val title = "Token Usage: $thresholdPercent% Reached"
        val requestInfo = if (requestCount > 0) " ($requestCount requests)" else ""
        val body = if (thresholdPercent >= 90) {
            "Approaching daily quota cap: ${numberFmt.format(totalTokens)} of ${numberFmt.format(dailyLimit)} tokens used today$requestInfo."
        } else {
            "${numberFmt.format(totalTokens)} of ${numberFmt.format(dailyLimit)} daily tokens used ($thresholdPercent%$requestInfo). Resets at midnight."
        }

        val openSettingsPendingIntent = createPendingIntent(context, targetPage = 3, requestCode = NOTIFICATION_ID_TOKEN_50 + thresholdPercent)
        val notifId = when (thresholdPercent) {
            90 -> NOTIFICATION_ID_TOKEN_90
            75 -> NOTIFICATION_ID_TOKEN_75
            else -> NOTIFICATION_ID_TOKEN_50
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_TOKEN_ALERTS)
            .setSmallIcon(R.drawable.ic_notification_app_icon)
            .setColor(NOTIFICATION_BG_COLOR)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(openSettingsPendingIntent)
            .addAction(R.drawable.ic_tune, "View Quota", openSettingsPendingIntent)

        safelyNotify(context, notifId, builder)
    }

    /**
     * Minimal Scheduled Meal Reminders
     */
    fun showMealReminder(
        context: Context,
        mealType: String,
        customTitle: String? = null,
        customBody: String? = null,
        aiName: String = "AI Assistant",
        replacementSuggestion: String? = null
    ) {
        createNotificationChannels(context)

        val (title, body, notifId, channelId) = when (mealType) {
            NotificationPreferences.MEAL_BREAKFAST -> Quadruple(
                customTitle ?: "Breakfast Reminder",
                customBody ?: "Time to fuel up for the day. Tap to log breakfast.",
                NOTIFICATION_ID_BASE + 1,
                CHANNEL_MEAL_REMINDERS
            )
            NotificationPreferences.MEAL_LUNCH -> Quadruple(
                customTitle ?: "Lunch Reminder",
                customBody ?: "Keep your energy surging. Quick log your midday meal.",
                NOTIFICATION_ID_BASE + 2,
                CHANNEL_MEAL_REMINDERS
            )
            NotificationPreferences.MEAL_SNACK -> Quadruple(
                customTitle ?: "Snack Reminder",
                customBody ?: "Time for a healthy bite or protein snack to hit your macros.",
                NOTIFICATION_ID_BASE + 3,
                CHANNEL_MEAL_REMINDERS
            )
            NotificationPreferences.MEAL_DINNER -> Quadruple(
                customTitle ?: "Dinner Reminder",
                customBody ?: "Finish strong! Log dinner to complete today's nutrition targets.",
                NOTIFICATION_ID_BASE + 4,
                CHANNEL_MEAL_REMINDERS
            )
            NotificationPreferences.SUMMARY_DAILY -> Quadruple(
                customTitle ?: "Daily Macro Review",
                customBody ?: "Check your calorie and protein totals for today.",
                NOTIFICATION_ID_BASE + 5,
                CHANNEL_MACRO_CHECKIN
            )
            else -> Quadruple(
                customTitle ?: "MacroBite Reminder",
                customBody ?: "Don't forget to track your nutrition today.",
                NOTIFICATION_ID_BASE,
                CHANNEL_MEAL_REMINDERS
            )
        }

        val fullBody = if (!replacementSuggestion.isNullOrBlank() && !body.contains(replacementSuggestion, ignoreCase = true)) {
            "$body\n💡 Suggested: $replacementSuggestion"
        } else {
            body
        }

        val logPendingIntent = createPendingIntent(context, targetPage = 0, requestCode = notifId + 100, focusQuickLog = true)
        val chatPendingIntent = createPendingIntent(context, targetPage = 1, requestCode = notifId + 200)
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_app_icon)
            .setColor(NOTIFICATION_BG_COLOR)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(fullBody))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(logPendingIntent)
            .addAction(R.drawable.ic_nav_restaurant, "Quick Log", logPendingIntent)
            .addAction(R.drawable.ic_auto_awesome, "Ask $aiName", chatPendingIntent)

        safelyNotify(context, notifId, builder)
    }

    /**
     * Test notification triggers
     */
    fun showTestNotification(context: Context, aiName: String = "AI Assistant") {
        createNotificationChannels(context)

        val openIntent = createPendingIntent(context, targetPage = 0, requestCode = NOTIFICATION_ID_TEST_MEAL)
        val chatIntent = createPendingIntent(context, targetPage = 1, requestCode = NOTIFICATION_ID_TEST_MEAL + 10)
        val builder = NotificationCompat.Builder(context, CHANNEL_MEAL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification_app_icon)
            .setColor(NOTIFICATION_BG_COLOR)
            .setContentTitle("MacroBite Notifications Active")
            .setContentText("Precision reminders are configured and active.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("All notifications and reminders are active and ready."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .addAction(R.drawable.ic_nav_restaurant, "Open Log", openIntent)
            .addAction(R.drawable.ic_auto_awesome, "Chat $aiName", chatIntent)

        safelyNotify(context, NOTIFICATION_ID_TEST_MEAL, builder)
    }

    fun showTestFoodLogNotification(context: Context) {
        showFoodLoggedNotification(
            context = context,
            meal = MealEntry(
                date = AppDate.todayIso(),
                category = MealCategory.LUNCH,
                foodName = "Grilled Chicken & Brown Rice",
                portion = "1 bowl",
                calories = 480,
                protein = 42f,
                carbs = 48f,
                fats = 12f
            )
        )
    }

    fun showTestChatResponseNotification(context: Context, aiName: String = "AI Assistant") {
        showChatResponseNotification(
            context = context,
            aiName = aiName,
            responseSnippet = "Here's your high-protein post-workout recommendation: 30g whey isolate with 1 banana (280 kcal, 27g protein)."
        )
    }

    fun showTestTokenAlertNotification(context: Context) {
        showTokenUsageAlert(
            context = context,
            thresholdPercent = 75,
            totalTokens = 75_000,
            dailyLimit = 100_000,
            requestCount = 28
        )
    }

    private fun createPendingIntent(
        context: Context,
        targetPage: Int,
        requestCode: Int,
        focusQuickLog: Boolean = false
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("target_page", targetPage)
            if (focusQuickLog) putExtra("focus_quick_log", true)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun safelyNotify(context: Context, id: Int, builder: NotificationCompat.Builder) {
        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(id, builder.build())
        } catch (e: SecurityException) {
            // Permission not granted
        } catch (t: Throwable) {
            // Log or ignore
        }
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
