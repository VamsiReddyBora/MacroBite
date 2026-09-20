package com.macrobite.app.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.macrobite.app.data.preferences.dataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import com.macrobite.app.R
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Universal Modern Floating In-App Notification Host with:
 * - Fluid spring bounce bottom entry & exit animation (matching the beloved delete undo HUD)
 * - Animated circular countdown progress ring
 * - Sleek dark capsule with glowing theme border
 * - Contextual badge title & icon detection (Food Logged, Health, AI, Settings, etc.)
 * - Tactile action pill button & swipe-to-dismiss gesture
 */
@Composable
fun ModernFloatingSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    defaultDurationMillis: Long = 4000L,
    enabled: Boolean? = null
) {
    val context = LocalContext.current
    val inAppNotificationsEnabled by remember(context, enabled) {
        if (enabled != null) {
            flowOf(enabled)
        } else {
            context.dataStore.data
                .map { prefs -> prefs[booleanPreferencesKey("notif_in_app_enabled")] ?: true }
                .catch { emit(true) }
        }
    }.collectAsState(initial = enabled ?: true)

    val currentData = hostState.currentSnackbarData
    var displayedData by remember { mutableStateOf<SnackbarData?>(null) }

    LaunchedEffect(currentData, inAppNotificationsEnabled) {
        if (!inAppNotificationsEnabled && currentData != null) {
            currentData.dismiss()
            displayedData = null
        } else if (currentData != null) {
            displayedData = currentData
        }
    }

    AnimatedVisibility(
        visible = inAppNotificationsEnabled && currentData != null,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.88f),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(200)
        ) + fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.88f),
        modifier = modifier
    ) {
        displayedData?.let { data ->
            val durationMillis = when (data.visuals.duration) {
                SnackbarDuration.Short -> defaultDurationMillis
                SnackbarDuration.Long -> 7000L
                SnackbarDuration.Indefinite -> Long.MAX_VALUE
            }

            ModernFloatingNotificationCapsule(
                data = data,
                durationMillis = durationMillis,
                onDismiss = { data.dismiss() }
            )
        }
    }
}

@Composable
fun ModernFloatingNotificationCapsule(
    data: SnackbarData,
    durationMillis: Long,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val progress = remember { Animatable(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(data) {
        offsetX = 0f
        if (durationMillis < Long.MAX_VALUE) {
            progress.snapTo(1f)
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = durationMillis.toInt(),
                    easing = LinearEasing
                )
            )
            onDismiss()
        }
    }

    val lowerMsg = data.visuals.message.lowercase()
    val (badgeText, iconRes, imageVector) = remember(lowerMsg) {
        when {
            "meal" in lowerMsg || "food" in lowerMsg || "logged" in lowerMsg || "kcal" in lowerMsg || "item" in lowerMsg || "top-up" in lowerMsg ->
                Triple("FOOD LOGGED", R.drawable.ic_nav_restaurant, null)
            "restored" in lowerMsg || "undo" in lowerMsg ->
                Triple("RESTORED", R.drawable.ic_undo, null)
            "step" in lowerMsg || "footstep" in lowerMsg || "health" in lowerMsg || "calorie" in lowerMsg || "burned" in lowerMsg ->
                Triple("GOOGLE HEALTH", R.drawable.ic_footsteps, null)
            "chat" in lowerMsg || "copied" in lowerMsg || "ai" in lowerMsg || "raaya" in lowerMsg || "response" in lowerMsg ->
                Triple("MACROBITE AI", R.drawable.ic_nav_chat, null)
            "export" in lowerMsg || "csv" in lowerMsg ->
                Triple("DATA EXPORT", R.drawable.ic_file_download, null)
            "setting" in lowerMsg || "saved" in lowerMsg || "theme" in lowerMsg || "goal" in lowerMsg || "preset" in lowerMsg ->
                Triple("SETTINGS", R.drawable.ic_tune, null)
            "error" in lowerMsg || "fail" in lowerMsg || "could not" in lowerMsg || "limit" in lowerMsg ->
                Triple("NOTICE", null, Icons.Default.Info)
            else ->
                Triple("MACROBITE", null, Icons.Default.Check)
        }
    }

    val badgeSubtitle = remember(data.visuals.message, lowerMsg) {
        val kcalMatch = Regex("""\+(\d+)\s*kcal""").find(data.visuals.message)
        when {
            kcalMatch != null -> "+${kcalMatch.groupValues[1]} kcal"
            "restored" in lowerMsg -> "SUCCESS"
            "saved" in lowerMsg -> "SAVED"
            "copied" in lowerMsg -> "CLIPBOARD"
            "export" in lowerMsg -> "CSV READY"
            "error" in lowerMsg || "fail" in lowerMsg -> "ALERT"
            else -> "SUCCESS"
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .offset { IntOffset(offsetX.roundToInt(), 0) }
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    offsetX += delta
                },
                onDragStopped = {
                    if (kotlin.math.abs(offsetX) > 160f) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        scope.launch {
                            onDismiss()
                        }
                    } else {
                        offsetX = 0f
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF191A20),
            tonalElevation = 8.dp,
            shadowElevation = 14.dp,
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .shadow(
                    16.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = Color.Black,
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Animated Circular Countdown Ring with Contextual Icon
                Box(
                    modifier = Modifier.size(38.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(38.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        strokeWidth = 2.8.dp,
                        trackColor = Color.Transparent
                    )
                    CircularProgressIndicator(
                        progress = { progress.value },
                        modifier = Modifier.size(38.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.8.dp,
                        trackColor = Color.Transparent
                    )
                    if (iconRes != null) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(17.dp)
                        )
                    } else if (imageVector != null) {
                        Icon(
                            imageVector = imageVector,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Middle: Context Badge & Notification Message
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp
                            ),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                        )
                        Text(
                            text = "• $badgeSubtitle",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = Color(0xFFAAAAAA)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = data.visuals.message,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        ),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Optional Action Button (e.g. Undo, View, etc.)
                data.visuals.actionLabel?.let { actionLabel ->
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                data.performAction()
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = actionLabel,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                // Far Right: Quick Dismiss 'X' Button
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDismiss()
                    },
                    modifier = Modifier
                        .size(28.dp)
                        .padding(start = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color(0xFF888894),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
