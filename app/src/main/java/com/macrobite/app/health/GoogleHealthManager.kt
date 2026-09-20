package com.macrobite.app.health

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.macrobite.app.domain.model.AppDate
import com.macrobite.app.domain.model.DailyActivityData
import com.macrobite.app.domain.model.GoogleHealthStatus
import java.time.LocalDate
import java.time.ZoneId
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class GoogleHealthManager @Inject constructor() {

    companion object {
        const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"

        val REQUIRED_PERMISSIONS: Set<String> = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class)
        )
    }

    /**
     * Checks whether Google Health Connect is available on this Android device.
     */
    fun getSdkStatus(context: Context): GoogleHealthStatus {
        return try {
            when (HealthConnectClient.getSdkStatus(context)) {
                HealthConnectClient.SDK_AVAILABLE -> GoogleHealthStatus.AVAILABLE
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> GoogleHealthStatus.NEEDS_APP_INSTALL
                else -> GoogleHealthStatus.UNSUPPORTED
            }
        } catch (t: Throwable) {
            GoogleHealthStatus.UNSUPPORTED
        }
    }

    /**
     * Checks if the required read permissions for footsteps and calorie burn have been granted.
     */
    suspend fun hasPermissions(context: Context): Boolean {
        if (getSdkStatus(context) != GoogleHealthStatus.AVAILABLE) return false
        return try {
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            // As long as steps read permission is granted, we have core integration
            granted.contains(HealthPermission.getReadPermission(StepsRecord::class))
        } catch (t: Throwable) {
            false
        }
    }

    /**
     * Reads footsteps and calories burned for the given date (YYYY-MM-DD) from Google Health Connect.
     */
    suspend fun readDailyActivity(
        context: Context,
        dateIso: String,
        userWeightKg: Float = 70f,
        stepGoal: Int = 10_000
    ): DailyActivityData {
        val sdkStatus = getSdkStatus(context)
        if (sdkStatus != GoogleHealthStatus.AVAILABLE) {
            return DailyActivityData(
                date = dateIso,
                stepGoal = stepGoal,
                sdkStatus = sdkStatus,
                isConnected = false,
                hasPermission = false
            )
        }

        val hasPerms = hasPermissions(context)
        if (!hasPerms) {
            return DailyActivityData(
                date = dateIso,
                stepGoal = stepGoal,
                sdkStatus = sdkStatus,
                isConnected = true,
                hasPermission = false
            )
        }

        return try {
            val client = HealthConnectClient.getOrCreate(context)
            val zoneId = ZoneId.systemDefault()
            val localDate = try {
                LocalDate.parse(dateIso)
            } catch (_: Throwable) {
                LocalDate.now()
            }

            val startTime = localDate.atStartOfDay(zoneId).toInstant()
            val isToday = (dateIso == AppDate.todayIso())
            val endTime = if (isToday) {
                Instant.now()
            } else {
                localDate.atTime(23, 59, 59, 999_999_999).atZone(zoneId).toInstant()
            }

            // Ensure start time is strictly before end time
            val validEndTime = if (endTime.isAfter(startTime)) endTime else startTime.plusSeconds(1)

            val response = client.aggregate(
                AggregateRequest(
                    metrics = setOf(
                        StepsRecord.COUNT_TOTAL,
                        TotalCaloriesBurnedRecord.ENERGY_TOTAL,
                        ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                        DistanceRecord.DISTANCE_TOTAL
                    ),
                    timeRangeFilter = TimeRangeFilter.between(startTime, validEndTime)
                )
            )

            val steps = response[StepsRecord.COUNT_TOTAL] ?: 0L
            val activeKcal = response[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories?.roundToInt() ?: 0
            val totalKcal = response[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories?.roundToInt() ?: 0
            val distanceKm = (response[DistanceRecord.DISTANCE_TOTAL]?.inKilometers ?: 0.0).toFloat()

            // Calculate calories burned:
            // 1. If active calories burned was recorded by the tracking app, use it directly.
            // 2. Otherwise calculate personalized active calorie burn based on footsteps & weight.
            val caloriesBurned = when {
                activeKcal > 0 -> activeKcal
                steps > 0 -> {
                    val weightFactor = (userWeightKg.coerceIn(40f, 200f)) / 70.0f
                    (steps * 0.04f * weightFactor).roundToInt()
                }
                totalKcal > 0 -> totalKcal
                else -> 0
            }

            // Estimated distance fallback if distance record was not provided by the app
            val effectiveDistanceKm = if (distanceKm > 0f) {
                distanceKm
            } else if (steps > 0) {
                // Average stride length is ~0.762m
                ((steps * 0.762f) / 1000f)
            } else {
                0f
            }

            DailyActivityData(
                date = dateIso,
                steps = steps,
                caloriesBurned = caloriesBurned,
                distanceKm = effectiveDistanceKm,
                stepGoal = stepGoal,
                isConnected = true,
                hasPermission = true,
                sdkStatus = sdkStatus,
                dataSource = "Google Health",
                lastSyncTime = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            DailyActivityData(
                date = dateIso,
                stepGoal = stepGoal,
                sdkStatus = sdkStatus,
                isConnected = true,
                hasPermission = hasPerms
            )
        }
    }

    /**
     * Prompts the user to install or update Google Health Connect via the Play Store.
     */
    fun openPlayStoreForHealthConnect(context: Context) {
        val playStoreUri = Uri.parse("market://details?id=$HEALTH_CONNECT_PACKAGE")
        val intent = Intent(Intent.ACTION_VIEW, playStoreUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$HEALTH_CONNECT_PACKAGE")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        }
    }

    /**
     * Direct shortcut to open the Health Connect settings / permissions screen.
     */
    fun openHealthConnectSettings(context: Context) {
        val intent = Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            openPlayStoreForHealthConnect(context)
        }
    }
}
