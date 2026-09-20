package com.macrobite.app.domain.model

import java.text.NumberFormat
import java.util.Locale

enum class GoogleHealthStatus {
    AVAILABLE,
    NEEDS_APP_INSTALL,
    UNSUPPORTED
}

data class DailyActivityData(
    val date: String = AppDate.todayIso(),
    val steps: Long = 0L,
    val caloriesBurned: Int = 0,
    val distanceKm: Float = 0f,
    val stepGoal: Int = 10_000,
    val isConnected: Boolean = false,
    val hasPermission: Boolean = false,
    val sdkStatus: GoogleHealthStatus = GoogleHealthStatus.AVAILABLE,
    val dataSource: String = "Google Health",
    val lastSyncTime: Long = 0L,
    val isSyncing: Boolean = false
) {
    val stepProgress: Float
        get() = (steps.toFloat() / stepGoal.coerceAtLeast(1)).coerceIn(0f, 1f)

    val formattedSteps: String
        get() = NumberFormat.getNumberInstance(Locale.US).format(steps)

    val formattedGoal: String
        get() = NumberFormat.getNumberInstance(Locale.US).format(stepGoal)

    val formattedCalories: String
        get() = "$caloriesBurned kcal"

    val formattedDistance: String
        get() = if (distanceKm >= 1.0f) String.format(Locale.US, "%.1f km", distanceKm) else String.format(Locale.US, "%.0f m", distanceKm * 1000f)
}
