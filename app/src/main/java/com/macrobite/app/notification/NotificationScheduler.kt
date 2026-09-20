package com.macrobite.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.macrobite.app.domain.model.NotificationPreferences
import java.util.Calendar

object NotificationScheduler {

    private const val TAG = "NotificationScheduler"
    const val ACTION_MEAL_REMINDER = "com.macrobite.app.ACTION_MEAL_REMINDER"
    const val EXTRA_MEAL_TYPE = "extra_meal_type"
    const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"

    private const val RC_BREAKFAST = 101
    private const val RC_LUNCH = 102
    private const val RC_SNACK = 103
    private const val RC_DINNER = 104
    private const val RC_DAILY_SUMMARY = 105

    private fun getRequestCode(mealType: String): Int = when (mealType) {
        NotificationPreferences.MEAL_BREAKFAST -> RC_BREAKFAST
        NotificationPreferences.MEAL_LUNCH -> RC_LUNCH
        NotificationPreferences.MEAL_SNACK -> RC_SNACK
        NotificationPreferences.MEAL_DINNER -> RC_DINNER
        NotificationPreferences.SUMMARY_DAILY -> RC_DAILY_SUMMARY
        else -> 100
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: false
        } else {
            true
        }
    }

    /**
     * Schedules or cancels all meal reminders and daily summaries based on the user preferences.
     */
    fun scheduleAll(context: Context, prefs: NotificationPreferences) {
        NotificationHelper.createNotificationChannels(context)

        if (!prefs.enabled) {
            Log.d(TAG, "Master notifications disabled. Cancelling all scheduled reminders.")
            cancelAll(context)
            return
        }

        scheduleMeal(context, NotificationPreferences.MEAL_BREAKFAST, prefs.breakfastEnabled, prefs.breakfastTime)
        scheduleMeal(context, NotificationPreferences.MEAL_LUNCH, prefs.lunchEnabled, prefs.lunchTime)
        scheduleMeal(context, NotificationPreferences.MEAL_SNACK, prefs.snackEnabled, prefs.snackTime)
        scheduleMeal(context, NotificationPreferences.MEAL_DINNER, prefs.dinnerEnabled, prefs.dinnerTime)
        scheduleMeal(context, NotificationPreferences.SUMMARY_DAILY, prefs.dailySummaryEnabled, prefs.dailySummaryTime)
    }

    /**
     * Schedules an exact alarm for a specific meal reminder.
     */
    fun scheduleMeal(context: Context, mealType: String, isEnabled: Boolean, timeStr: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val requestCode = getRequestCode(mealType)

        val intent = Intent(context, MealReminderReceiver::class.java).apply {
            action = ACTION_MEAL_REMINDER
            putExtra(EXTRA_MEAL_TYPE, mealType)
            putExtra(EXTRA_SCHEDULED_TIME, timeStr)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!isEnabled) {
            Log.d(TAG, "Meal $mealType is disabled. Cancelling alarm RC=$requestCode.")
            try {
                alarmManager.cancel(pendingIntent)
            } catch (e: Throwable) {
                Log.e(TAG, "Error cancelling alarm for $mealType", e)
            }
            return
        }

        val triggerAtMillis = calculateNextTriggerMillis(timeStr)
        setExactAlarm(alarmManager, triggerAtMillis, pendingIntent)
        Log.d(TAG, "Scheduled exact alarm for $mealType at $timeStr (millis=$triggerAtMillis, RC=$requestCode)")
    }

    /**
     * Schedules the next day's exact alarm after today's reminder has fired.
     */
    fun scheduleNextDailyAlarm(context: Context, mealType: String, timeStr: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val requestCode = getRequestCode(mealType)

        val intent = Intent(context, MealReminderReceiver::class.java).apply {
            action = ACTION_MEAL_REMINDER
            putExtra(EXTRA_MEAL_TYPE, mealType)
            putExtra(EXTRA_SCHEDULED_TIME, timeStr)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = calculateNextTriggerMillis(timeStr, forceTomorrow = true)
        setExactAlarm(alarmManager, triggerAtMillis, pendingIntent)
        Log.d(TAG, "Rescheduled next day's exact alarm for $mealType at $timeStr")
    }

    /**
     * Cancels all scheduled reminder alarms.
     */
    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val allMealTypes = listOf(
            NotificationPreferences.MEAL_BREAKFAST,
            NotificationPreferences.MEAL_LUNCH,
            NotificationPreferences.MEAL_SNACK,
            NotificationPreferences.MEAL_DINNER,
            NotificationPreferences.SUMMARY_DAILY
        )

        for (mealType in allMealTypes) {
            val requestCode = getRequestCode(mealType)
            val intent = Intent(context, MealReminderReceiver::class.java).apply {
                action = ACTION_MEAL_REMINDER
                putExtra(EXTRA_MEAL_TYPE, mealType)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                try {
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                } catch (e: Throwable) {
                    Log.e(TAG, "Failed cancelling alarm for $mealType", e)
                }
            }
        }
    }

    private fun setExactAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    // Graceful fallback if exact alarm permission was revoked by user in system settings
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (se: SecurityException) {
            Log.w(TAG, "Exact alarm permission not granted, falling back to setAndAllowWhileIdle: ${se.message}")
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to set fallback alarm", t)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Unexpected error setting exact alarm", e)
        }
    }

    fun calculateNextTriggerMillis(timeStr: String, forceTomorrow: Boolean = false): Long {
        val parts = timeStr.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 12
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val now = System.currentTimeMillis()
        if (forceTomorrow || calendar.timeInMillis <= now) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return calendar.timeInMillis
    }
}
