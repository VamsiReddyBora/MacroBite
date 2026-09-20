package com.macrobite.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.macrobite.app.domain.model.NotificationPreferences
import com.macrobite.app.domain.repository.UserPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MealReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var aiWellWisherCoach: AiWellWisherCoach

    override fun onReceive(context: Context, intent: Intent) {
        val mealType = intent.getStringExtra(NotificationScheduler.EXTRA_MEAL_TYPE) ?: return
        val scheduledTime = intent.getStringExtra(NotificationScheduler.EXTRA_SCHEDULED_TIME) ?: "12:00"

        Log.d("MealReminderReceiver", "Alarm triggered for meal: $mealType at $scheduledTime")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = userPreferencesRepository.getNotificationPreferences().firstOrNull()
                    ?: NotificationPreferences()
                val aiName = try {
                    userPreferencesRepository.getRaayaName().firstOrNull()?.trim()?.ifBlank { "AI Assistant" } ?: "AI Assistant"
                } catch (e: Throwable) {
                    "AI Assistant"
                }

                if (prefs.enabled && prefs.isMealEnabled(mealType)) {
                    val currentTime = prefs.getMealTime(mealType)

                    // Generate intelligent, empathetic well-wisher advice based on actual database food logs & deficits
                    val advice = try {
                        aiWellWisherCoach.generateWellWisherAdvice(mealType = mealType, aiName = aiName)
                    } catch (e: Throwable) {
                        Log.e("MealReminderReceiver", "Error generating well-wisher advice, falling back to default", e)
                        null
                    }

                    NotificationHelper.showMealReminder(
                        context = context,
                        mealType = mealType,
                        customTitle = advice?.title,
                        customBody = advice?.body,
                        aiName = aiName,
                        replacementSuggestion = advice?.recommendedActionFood
                    )

                    // Schedule next occurrence for tomorrow to ensure exact 24-hour cycle precision
                    NotificationScheduler.scheduleNextDailyAlarm(context, mealType, currentTime)
                } else {
                    Log.d("MealReminderReceiver", "Meal $mealType or notifications disabled in preferences. Suppressing.")
                }
            } catch (e: Throwable) {
                Log.e("MealReminderReceiver", "Error processing reminder for $mealType", e)
                // Fallback: show reminder anyway
                NotificationHelper.showMealReminder(context, mealType)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
