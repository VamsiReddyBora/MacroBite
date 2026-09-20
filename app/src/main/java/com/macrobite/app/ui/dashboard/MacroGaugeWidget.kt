package com.macrobite.app.ui.dashboard

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.macrobite.app.R
import com.macrobite.app.domain.model.DailyActivityData
import com.macrobite.app.domain.model.DailyMacros
import com.macrobite.app.domain.model.formatMacroOneDecimal
import com.macrobite.app.ui.theme.CarbsGreen
import com.macrobite.app.ui.theme.FatsCoral
import com.macrobite.app.ui.theme.ProteinBlue

@Composable
fun MacroGaugeWidget(
    dailyMacros: DailyMacros,
    dailyActivity: DailyActivityData = DailyActivityData(),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Main Calorie Ring + Quick Overview
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Calorie Circular Ring
                val animatedCalorieProgress by animateFloatAsState(
                    targetValue = dailyMacros.calorieProgress,
                    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                    label = "calorie_progress"
                )

                val animatedTotalCalories by animateIntAsState(
                    targetValue = dailyMacros.totalCalories,
                    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                    label = "total_calories_counter"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(118.dp)
                ) {
                    val trackColor = MaterialTheme.colorScheme.surfaceVariant
                    val progressColor = MaterialTheme.colorScheme.primary

                    Canvas(modifier = Modifier.size(108.dp)) {
                        val strokeWidth = 11.dp.toPx()
                        // Background track
                        drawCircle(
                            color = trackColor,
                            radius = (size.minDimension - strokeWidth) / 2,
                            style = Stroke(width = strokeWidth)
                        )
                        // Progress arc with rounded cap
                        if (animatedCalorieProgress > 0f) {
                            drawArc(
                                color = progressColor,
                                startAngle = -90f,
                                sweepAngle = (360f * animatedCalorieProgress).coerceAtMost(360f),
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "%,d".format(animatedTotalCalories),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                                letterSpacing = (-0.5).sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "🔥 KCAL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Calorie Target & Remaining Metrics
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DAILY TARGET",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "%,d kcal".format(dailyMacros.targetCalories),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val rem = dailyMacros.remainingCalories
                    val isOver = rem < 0
                    val animatedRem by animateIntAsState(
                        targetValue = rem,
                        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                        label = "remaining_calories_counter"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isOver) "SURPLUS" else "REMAINING",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isOver) "+%,d kcal".format(-animatedRem) else "%,d kcal".format(animatedRem),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isOver) CarbsGreen else MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Clean status pill
                    val percent = (dailyMacros.calorieProgress * 100).toInt()
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (isOver) {
                                "Goal reached! Surplus achieved 🔥"
                            } else {
                                "$percent% of daily calorie goal"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = if (isOver) CarbsGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Dynamic Net Calories & Footstep Burn Integration
            if (dailyActivity.caloriesBurned > 0) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Eaten
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "EATEN",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${dailyMacros.totalCalories} kcal",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(text = "−", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    // Burned
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "BURNED",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${dailyActivity.caloriesBurned} kcal",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFF97316)
                        )
                    }

                    Text(text = "=", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    // Net Intake
                    val netKcal = (dailyMacros.totalCalories - dailyActivity.caloriesBurned).coerceAtLeast(0)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "NET INTAKE",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "$netKcal kcal",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_footsteps),
                        contentDescription = null,
                        tint = Color(0xFFF97316),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "+${dailyActivity.caloriesBurned} kcal allowance earned from ${dailyActivity.formattedSteps} steps",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                        color = Color(0xFFF97316)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3 Modern Macro Stat Cards (Protein, Carbs, Fats)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MacroCardItem(
                    emoji = "🥩",
                    label = "PROTEIN",
                    current = dailyMacros.totalProtein,
                    target = dailyMacros.targetProtein,
                    progress = dailyMacros.proteinProgress,
                    color = ProteinBlue,
                    modifier = Modifier.weight(1f)
                )

                MacroCardItem(
                    emoji = "🌾",
                    label = "CARBS",
                    current = dailyMacros.totalCarbs,
                    target = dailyMacros.targetCarbs,
                    progress = dailyMacros.carbsProgress,
                    color = CarbsGreen,
                    modifier = Modifier.weight(1f)
                )

                MacroCardItem(
                    emoji = "🥑",
                    label = "FATS",
                    current = dailyMacros.totalFats,
                    target = dailyMacros.targetFats,
                    progress = dailyMacros.fatsProgress,
                    color = FatsCoral,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MacroCardItem(
    emoji: String,
    label: String,
    current: Float,
    target: Int,
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "${label}_progress"
    )

    val animatedCurrent by animateFloatAsState(
        targetValue = current,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "${label}_counter"
    )

    val percent = (progress * 100).toInt()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = emoji,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = color
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${animatedCurrent.formatMacroOneDecimal()}g",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = " / ${target}g",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 1.dp, start = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { animatedProgress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
        }
    }
}
