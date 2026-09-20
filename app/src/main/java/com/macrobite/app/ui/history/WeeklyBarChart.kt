package com.macrobite.app.ui.history

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.macrobite.app.domain.model.AppDate
import com.macrobite.app.ui.theme.CarbsGreen

data class DayCalorieData(
    val date: AppDate,
    val calories: Int,
    val targetCalories: Int
)

@Composable
fun WeeklyBarChart(
    daysData: List<DayCalorieData>,
    selectedDate: AppDate,
    onSelectDay: (AppDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

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
            val targetCal = daysData.firstOrNull()?.targetCalories ?: 3000
            val maxCal = (daysData.maxOfOrNull { it.calories } ?: targetCal).coerceAtLeast(targetCal + 400)
            val nonZeroDays = daysData.filter { it.calories > 0 }
            val avgCal = if (nonZeroDays.isNotEmpty()) nonZeroDays.sumOf { it.calories } / nonZeroDays.size else 0

            // Header with Target & Average
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Weekly Calorie Intake",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Target: %,d kcal/day (dashed line)".format(targetCal),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(primaryColor.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "Avg: %,d kcal".format(avgCal),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = primaryColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Chart Canvas
            val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
            val outlineColor = MaterialTheme.colorScheme.outlineVariant

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val barCount = daysData.size.coerceAtLeast(1)
                    val barSpacing = width / barCount
                    val barWidth = barSpacing * 0.42f

                    // Target Dashed Line
                    val targetY = height - (targetCal.toFloat() / maxCal * height)
                    drawLine(
                        color = outlineColor,
                        start = Offset(0f, targetY),
                        end = Offset(width, targetY),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )

                    // Draw each day bar
                    daysData.forEachIndexed { index, dayData ->
                        val barHeight = (dayData.calories.toFloat() / maxCal * height).coerceAtLeast(4f)
                        val left = index * barSpacing + (barSpacing - barWidth) / 2
                        val top = height - barHeight
                        val isSelected = dayData.date == selectedDate
                        val cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())

                        // Background pillar track (highlighted with primary tint if selected)
                        val trackColor = if (isSelected) {
                            primaryColor.copy(alpha = 0.22f)
                        } else {
                            surfaceVariant.copy(alpha = 0.45f)
                        }

                        drawRoundRect(
                            color = trackColor,
                            topLeft = Offset(left, 0f),
                            size = Size(barWidth, height),
                            cornerRadius = cornerRadius
                        )

                        // Calorie bar fill
                        val barColor = when {
                            isSelected -> primaryColor
                            dayData.calories >= targetCal -> CarbsGreen
                            dayData.calories > 0 -> primaryColor.copy(alpha = 0.55f)
                            else -> Color.Transparent
                        }

                        if (dayData.calories > 0) {
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(left, top),
                                size = Size(barWidth, barHeight),
                                cornerRadius = cornerRadius
                            )
                        } else if (isSelected) {
                            // Baseline indicator for selected zero-calorie day
                            drawRoundRect(
                                color = primaryColor.copy(alpha = 0.65f),
                                topLeft = Offset(left, height - 4.dp.toPx()),
                                size = Size(barWidth, 4.dp.toPx()),
                                cornerRadius = cornerRadius
                            )
                        }

                        // Luminous border highlight around the selected pillar
                        if (isSelected) {
                            drawRoundRect(
                                color = primaryColor,
                                topLeft = Offset(left - 1.5.dp.toPx(), -1.5.dp.toPx()),
                                size = Size(barWidth + 3.dp.toPx(), height + 3.dp.toPx()),
                                cornerRadius = CornerRadius(7.5.dp.toPx(), 7.5.dp.toPx()),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }

                // Interactive tap overlay for each day bar
                Row(modifier = Modifier.fillMaxSize()) {
                    daysData.forEach { dayData ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onSelectDay(dayData.date)
                                }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Day Labels Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                daysData.forEach { dayData ->
                    val isSelected = dayData.date == selectedDate
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) primaryColor.copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSelectDay(dayData.date) }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = dayData.date.toDayOfWeekShort().take(2).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${dayData.date.dayOfMonth}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(primaryColor)
                            )
                        } else {
                            Spacer(modifier = Modifier.size(4.dp))
                        }
                    }
                }
            }
        }
    }
}
