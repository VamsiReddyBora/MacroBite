package com.macrobite.app.ui.settings

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.macrobite.app.R
import com.macrobite.app.domain.model.DailyApiUsage
import com.macrobite.app.ui.theme.CarbsGreen
import com.macrobite.app.ui.theme.FatsCoral
import com.macrobite.app.ui.theme.ProteinBlue
import java.text.NumberFormat
import java.util.Locale


@Composable
fun GeminiApiUsageCard(
    usage: DailyApiUsage,
    activeModel: String = "gemini-3.5-flash-lite",
    onRefreshQuota: () -> Unit = {},
    liveQuotaStatus: SettingsViewModel.LiveQuotaStatus = SettingsViewModel.LiveQuotaStatus(),
    isScreenActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val numberFmt = remember { NumberFormat.getNumberInstance(Locale.US) }
    val uriHandler = LocalUriHandler.current

    val isLiteModel = usage.dailyLimit >= 500

    // Auto-collapse when navigating away from settings
    LaunchedEffect(isScreenActive) {
        if (!isScreenActive) {
            isExpanded = false
        }
    }

    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "usage_arrow_rotation"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Clickable Dropdown Header
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
                            painter = painterResource(id = R.drawable.ic_bolt),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Gemini API Quota & Usage",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                               ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isLiteModel) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CarbsGreen.copy(alpha = 0.15f))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "500 RPD",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CarbsGreen
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${usage.totalRequestCount} / ${usage.dailyLimit} requests (${usage.requestCount} app${if (usage.externalRequestCount > 0) " + ${usage.externalRequestCount} external" else ""}) • ${numberFmt.format(usage.totalTokens)} tokens",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
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

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Daily usage (${if (usage.date.isNotBlank()) usage.date else "Today"}) tracking:",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isLiteModel) CarbsGreen.copy(alpha = 0.12f) else FatsCoral.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isLiteModel) CarbsGreen else FatsCoral)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isLiteModel) "⭐ 500 RPD Recommended" else "⚠️ 20 RPD Cap",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = if (isLiteModel) CarbsGreen else FatsCoral
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Two main stat boxes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Stat 1: Total Tokens Today
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "TOKENS TODAY",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.sp,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = numberFmt.format(usage.totalTokens),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 22.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Prompt + Response",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Stat 2: Daily Requests Made (Total = Internal + External)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "DAILY REQUESTS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.sp,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${usage.totalRequestCount} / ${usage.dailyLimit}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 22.sp
                                    ),
                                    color = if (usage.estimatedRemainingRequests == 0) FatsCoral else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (usage.estimatedRemainingRequests > 0) {
                                        "${numberFmt.format(usage.estimatedRemainingRequests)} remaining"
                                    } else {
                                        "Limit reached (0 left)"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.sp
                                    ),
                                    color = if (usage.estimatedRemainingRequests > 0) CarbsGreen else FatsCoral
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Progress Bar for Free-tier daily limit
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            progress = { usage.usagePercentage },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (usage.usagePercentage >= 0.85f) FatsCoral else CarbsGreen,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${String.format(Locale.US, "%.1f", usage.usagePercentage * 100)}% of daily quota used",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Daily Limit: ${usage.dailyLimit} RPD",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // SECTION 1: Live API Status & Quota Check
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                when {
                                    liveQuotaStatus.isRateLimited -> FatsCoral.copy(alpha = 0.08f)
                                    liveQuotaStatus.isApiAlive == true -> CarbsGreen.copy(alpha = 0.08f)
                                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                }
                            )
                            .border(
                                BorderStroke(
                                    1.dp,
                                    when {
                                        liveQuotaStatus.isRateLimited -> FatsCoral.copy(alpha = 0.3f)
                                        liveQuotaStatus.isApiAlive == true -> CarbsGreen.copy(alpha = 0.3f)
                                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    }
                                ),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    liveQuotaStatus.isProbing -> MaterialTheme.colorScheme.primary
                                                    liveQuotaStatus.isRateLimited -> FatsCoral
                                                    liveQuotaStatus.isApiAlive == true -> CarbsGreen
                                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Live API Status",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (liveQuotaStatus.lastProbeTime != null) {
                                        Text(
                                            text = "Last: ${liveQuotaStatus.lastProbeTime}",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                when {
                                                    liveQuotaStatus.isProbing -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                    liveQuotaStatus.isRateLimited -> FatsCoral.copy(alpha = 0.15f)
                                                    liveQuotaStatus.isApiAlive == true -> CarbsGreen.copy(alpha = 0.15f)
                                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                                }
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                liveQuotaStatus.isProbing -> "Checking..."
                                                liveQuotaStatus.isRateLimited -> "❌ Quota Exhausted"
                                                liveQuotaStatus.isApiAlive == true -> "✅ API Active"
                                                liveQuotaStatus.errorMessage != null -> "⚠️ Error"
                                                else -> "Unknown"
                                            },
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                liveQuotaStatus.isProbing -> MaterialTheme.colorScheme.primary
                                                liveQuotaStatus.isRateLimited -> FatsCoral
                                                liveQuotaStatus.isApiAlive == true -> CarbsGreen
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Status message
                            Text(
                                text = when {
                                    liveQuotaStatus.isProbing -> "Probing Google Gemini API to verify quota availability..."
                                    liveQuotaStatus.isRateLimited -> "Your daily RPD quota is exhausted. The API will reject new requests until midnight Pacific Time reset."
                                    liveQuotaStatus.isApiAlive == true -> "Your API key is active and accepting requests. Quota has not been exhausted."
                                    liveQuotaStatus.errorMessage != null -> "Error: ${liveQuotaStatus.errorMessage}"
                                    else -> "Tap 'Check Now' to probe the API and verify your current quota status."
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 14.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Breakdown Pill: App requests from today
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "MacroBite Today: ${usage.requestCount} request${if (usage.requestCount != 1) "s" else ""}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${usage.requestCount} / ${usage.dailyLimit} RPD",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (usage.estimatedRemainingRequests == 0) FatsCoral else CarbsGreen
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Check Now & Open Dashboard buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = onRefreshQuota,
                                    enabled = !liveQuotaStatus.isProbing,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_speed),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (liveQuotaStatus.isProbing) "Checking..." else "Check Now",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(
                                    onClick = {
                                        uriHandler.openUri("https://aistudio.google.com/rate-limit")
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Open AI Studio ↗",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

        }
    }
}
}
}
