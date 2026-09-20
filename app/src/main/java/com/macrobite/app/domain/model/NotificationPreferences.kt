package com.macrobite.app.domain.model

data class NotificationPreferences(
    val enabled: Boolean = true,
    val breakfastEnabled: Boolean = true,
    val breakfastTime: String = "08:30",
    val lunchEnabled: Boolean = true,
    val lunchTime: String = "13:00",
    val snackEnabled: Boolean = true,
    val snackTime: String = "17:30",
    val dinnerEnabled: Boolean = true,
    val dinnerTime: String = "20:30",
    val dailySummaryEnabled: Boolean = true,
    val dailySummaryTime: String = "21:30",
    val inAppNotificationsEnabled: Boolean = true,
    val foodLogNotificationEnabled: Boolean = false,
    val chatResponseNotificationEnabled: Boolean = true,
    val tokenUsageAlertsEnabled: Boolean = true,
    val dailyTokenAlertLimit: Int = 100_000
) {
    companion object {
        const val MEAL_BREAKFAST = "BREAKFAST"
        const val MEAL_LUNCH = "LUNCH"
        const val MEAL_SNACK = "SNACK"
        const val MEAL_DINNER = "DINNER"
        const val SUMMARY_DAILY = "DAILY_SUMMARY"
    }

    fun isMealEnabled(mealType: String): Boolean = when (mealType) {
        MEAL_BREAKFAST -> enabled && breakfastEnabled
        MEAL_LUNCH -> enabled && lunchEnabled
        MEAL_SNACK -> enabled && snackEnabled
        MEAL_DINNER -> enabled && dinnerEnabled
        SUMMARY_DAILY -> enabled && dailySummaryEnabled
        else -> false
    }

    fun getMealTime(mealType: String): String = when (mealType) {
        MEAL_BREAKFAST -> breakfastTime
        MEAL_LUNCH -> lunchTime
        MEAL_SNACK -> snackTime
        MEAL_DINNER -> dinnerTime
        SUMMARY_DAILY -> dailySummaryTime
        else -> "12:00"
    }
}
