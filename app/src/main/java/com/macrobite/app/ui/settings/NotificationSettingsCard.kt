package com.macrobite.app.ui.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.macrobite.app.R
import com.macrobite.app.domain.model.NotificationPreferences
import com.macrobite.app.notification.NotificationScheduler

@Composable
fun NotificationSettingsCard(
    prefs: NotificationPreferences,
    onToggleMaster: (Boolean) -> Unit,
    onToggleMeal: (String, Boolean) -> Unit,
    onSetMealTime: (String, String) -> Unit,
    onToggleInAppNotifications: (Boolean) -> Unit = {},
    onToggleFoodLogNotification: (Boolean) -> Unit,
    onToggleChatResponseNotification: (Boolean) -> Unit,
    onToggleTokenAlerts: (Boolean) -> Unit,
    onSetDailyTokenLimit: (Int) -> Unit,
    onSendTestNotification: () -> Unit,
    onSendTestFoodLogNotification: () -> Unit,
    onSendTestChatNotification: () -> Unit,
    onSendTestTokenNotification: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "notif_arrow_rotation"
    )

    var hasPostNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPostNotificationPermission = isGranted
        if (isGranted) {
            onToggleMaster(true)
        }
    }

    val canExactAlarm = remember {
        NotificationScheduler.canScheduleExactAlarms(context)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Dropdown Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_notification_bell),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Notifications & Reminders",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (prefs.enabled) "Active • Precision meals, logs, chat & token quota" else "Paused • Tap to configure",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = if (prefs.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_keyboard_arrow_down),
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.rotate(arrowRotation)
                    )
                }
            }

            // Dropdown Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .padding(bottom = 18.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 14.dp),
                        thickness = 0.6.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Master Toggle Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable System Notifications",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Master switch for reminders, activity & quota alerts",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = prefs.enabled,
                            onCheckedChange = { isChecked ->
                                if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPostNotificationPermission) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onToggleMaster(isChecked)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    // Android 13+ Permission Warning Banner
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPostNotificationPermission) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                                .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Permission Required",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Allow MacroBite to post notifications in your status bar.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Exact Alarm Status Chip
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (canExactAlarm) Color(0xFF4CAF50) else Color(0xFFFFB74D))
                        )
                        Text(
                            text = if (canExactAlarm) "Exact Alarm Engine: Active (Pinpoint Precision)" else "Standard Alarm Engine: Active",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // SECTION 1: Meal & Logging Reminders
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "AI WELL-WISHER & MEAL SCHEDULE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Intelligent Coach",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "If you forget to log or face trouble eating, reminders dynamically suggest practical high-calorie replacements based on your remaining deficit.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    MealTimeSettingRow(
                        label = "Breakfast Reminder",
                        emoji = "🍳",
                        timeStr = prefs.breakfastTime,
                        isEnabled = prefs.breakfastEnabled,
                        isMasterEnabled = prefs.enabled,
                        onToggle = { onToggleMeal(NotificationPreferences.MEAL_BREAKFAST, it) },
                        onTimeClick = {
                            showTimePickerDialog(context, prefs.breakfastTime) { newTime ->
                                onSetMealTime(NotificationPreferences.MEAL_BREAKFAST, newTime)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    MealTimeSettingRow(
                        label = "Lunch Reminder",
                        emoji = "🥗",
                        timeStr = prefs.lunchTime,
                        isEnabled = prefs.lunchEnabled,
                        isMasterEnabled = prefs.enabled,
                        onToggle = { onToggleMeal(NotificationPreferences.MEAL_LUNCH, it) },
                        onTimeClick = {
                            showTimePickerDialog(context, prefs.lunchTime) { newTime ->
                                onSetMealTime(NotificationPreferences.MEAL_LUNCH, newTime)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    MealTimeSettingRow(
                        label = "Pre/Post-Workout Snack",
                        emoji = "🍎",
                        timeStr = prefs.snackTime,
                        isEnabled = prefs.snackEnabled,
                        isMasterEnabled = prefs.enabled,
                        onToggle = { onToggleMeal(NotificationPreferences.MEAL_SNACK, it) },
                        onTimeClick = {
                            showTimePickerDialog(context, prefs.snackTime) { newTime ->
                                onSetMealTime(NotificationPreferences.MEAL_SNACK, newTime)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    MealTimeSettingRow(
                        label = "Dinner Reminder",
                        emoji = "🍲",
                        timeStr = prefs.dinnerTime,
                        isEnabled = prefs.dinnerEnabled,
                        isMasterEnabled = prefs.enabled,
                        onToggle = { onToggleMeal(NotificationPreferences.MEAL_DINNER, it) },
                        onTimeClick = {
                            showTimePickerDialog(context, prefs.dinnerTime) { newTime ->
                                onSetMealTime(NotificationPreferences.MEAL_DINNER, newTime)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    MealTimeSettingRow(
                        label = "Daily Macro Review",
                        emoji = "📊",
                        timeStr = prefs.dailySummaryTime,
                        isEnabled = prefs.dailySummaryEnabled,
                        isMasterEnabled = prefs.enabled,
                        onToggle = { onToggleMeal(NotificationPreferences.SUMMARY_DAILY, it) },
                        onTimeClick = {
                            showTimePickerDialog(context, prefs.dailySummaryTime) { newTime ->
                                onSetMealTime(NotificationPreferences.SUMMARY_DAILY, newTime)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // SECTION 2: Smart Activity Confirmations
                    Text(
                        text = "SMART ACTIVITY NOTIFICATIONS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // In-App Floating Notifications Toggle
                    SwitchSettingRow(
                        title = "In-App Floating Notifications",
                        subtitle = "Show animated floating popups for food logs, restored items, and quick alerts",
                        emoji = "🔔",
                        checked = prefs.inAppNotificationsEnabled,
                        enabled = true,
                        onCheckedChange = onToggleInAppNotifications
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Food Logged Notification Toggle
                    SwitchSettingRow(
                        title = "Food Logged Status Bar Alerts",
                        subtitle = "Receive status bar alert on food log (in-app animated popup is active by default)",
                        emoji = "🍽️",
                        checked = prefs.foodLogNotificationEnabled,
                        enabled = prefs.enabled,
                        onCheckedChange = onToggleFoodLogNotification
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Chat Background Response Notification Toggle
                    SwitchSettingRow(
                        title = "AI Chat Background Replies",
                        subtitle = "Notify when AI responds if you navigate away or background chat",
                        emoji = "✨",
                        checked = prefs.chatResponseNotificationEnabled,
                        enabled = prefs.enabled,
                        onCheckedChange = onToggleChatResponseNotification
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // SECTION 3: Token Quota & API Limit Alerts
                    Text(
                        text = "API QUOTA & TOKEN ALERTS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Token Threshold Alert Toggle
                    SwitchSettingRow(
                        title = "Token Usage Alerts (50%, 75%, 90%)",
                        subtitle = "Alert once per day when token usage crosses 50%, 75%, and 90%",
                        emoji = "⚡",
                        checked = prefs.tokenUsageAlertsEnabled,
                        enabled = prefs.enabled,
                        onCheckedChange = onToggleTokenAlerts
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Daily Token Budget Threshold Selector Chips
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Daily Token Alert Budget Basis:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val tokenBudgetOptions = listOf(
                            50_000 to "50K",
                            100_000 to "100K",
                            250_000 to "250K",
                            500_000 to "500K",
                            1_000_000 to "1M"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            tokenBudgetOptions.forEach { (limitValue, label) ->
                                val isSelected = prefs.dailyTokenAlertLimit == limitValue
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable(enabled = prefs.enabled && prefs.tokenUsageAlertsEnabled) {
                                            onSetDailyTokenLimit(limitValue)
                                        }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 11.sp
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Active basis: %,d tokens/day. Alerts fire at 50%%, 75%%, and 90%%.".format(prefs.dailyTokenAlertLimit),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // SECTION 4: Instant Test Controls
                    Text(
                        text = "TEST NOTIFICATION PREVIEWS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // 2x2 Grid of Test Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPostNotificationPermission) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onSendTestNotification()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test AI Advice", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPostNotificationPermission) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onSendTestFoodLogNotification()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test Food Log", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPostNotificationPermission) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onSendTestChatNotification()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test AI Reply", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPostNotificationPermission) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onSendTestTokenNotification()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test Quota 75%", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchSettingRow(
    title: String,
    subtitle: String,
    emoji: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val active = enabled && checked
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f) else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(text = emoji, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun MealTimeSettingRow(
    label: String,
    emoji: String,
    timeStr: String,
    isEnabled: Boolean,
    isMasterEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onTimeClick: () -> Unit
) {
    val active = isMasterEnabled && isEnabled
    val displayTime = formatDisplayTime(timeStr)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f) else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(text = emoji, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Clickable Time Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                    .border(
                        width = 0.8.dp,
                        color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable(enabled = active) { onTimeClick() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayTime,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            Switch(
                checked = isEnabled,
                enabled = isMasterEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

private fun formatDisplayTime(timeStr: String): String {
    val parts = timeStr.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 12
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val isPm = hour >= 12
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val amPm = if (isPm) "PM" else "AM"
    return "%02d:%02d %s".format(displayHour, minute, amPm)
}

private fun showTimePickerDialog(
    context: Context,
    currentTimeStr: String,
    onTimeSelected: (String) -> Unit
) {
    val parts = currentTimeStr.split(":")
    val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 12
    val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            val formatted = "%02d:%02d".format(hourOfDay, minute)
            onTimeSelected(formatted)
        },
        initialHour,
        initialMinute,
        DateFormat.is24HourFormat(context)
    )
    timePickerDialog.show()
}
