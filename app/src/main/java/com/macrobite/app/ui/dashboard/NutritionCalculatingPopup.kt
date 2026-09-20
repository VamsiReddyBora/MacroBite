package com.macrobite.app.ui.dashboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.macrobite.app.ui.theme.CarbsGreen
import com.macrobite.app.ui.theme.ProteinBlue
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun NutritionCalculatingPopup(
    message: String = "Calculating nutrition...",
    foodQuery: String? = null,
    onCancel: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "notepad_calc_transition")

    // Cycle progress runs for exactly 3000ms (3 seconds) to allow full, clear viewing
    val cycleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cycle_progress"
    )

    // Shimmer bar offset
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -200f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    Dialog(
        onDismissRequest = { onCancel?.invoke() },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Realistic Animated Scene: Notepad with dynamic food query, fountain pen & realistic ink
                NotepadCalculationScene(
                    progress = cycleProgress,
                    foodQuery = foodQuery,
                    isDark = MaterialTheme.colorScheme.background.red < 0.5f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(188.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Title (Clean, no AI/Gemini mention)
                Text(
                    text = if (message.isNotBlank()) message else "Calculating Nutrition",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Dynamic contextual subtitle synchronized to the 3-second cycle
                val cleanQuery = foodQuery?.trim()?.take(22)
                val phaseCaption = when {
                    cycleProgress < 0.30f -> if (!cleanQuery.isNullOrBlank()) "Searching database for \"$cleanQuery\"..." else "Searching food item database..."
                    cycleProgress < 0.54f -> "Analyzing portion size & weight..."
                    cycleProgress < 0.74f -> "Calculating calorie density..."
                    cycleProgress < 0.88f -> "Tallying proteins, carbs & healthy fats..."
                    else -> "Finalizing nutrition table..."
                }

                Text(
                    text = phaseCaption,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Minimal Slim Shimmer Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        MaterialTheme.colorScheme.primary,
                                        ProteinBlue,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    ),
                                    start = Offset(shimmerOffset - 200f, 0f),
                                    end = Offset(shimmerOffset + 200f, 0f)
                                )
                            )
                    )
                }

                if (onCancel != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotepadCalculationScene(
    progress: Float,
    foodQuery: String?,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(width = 240.dp, height = 184.dp)) {
            val padWidth = size.width
            val padHeight = size.height

            // Notepad Paper Colors
            val paperBg = if (isDark) Color(0xFF1B222E) else Color(0xFFFCFDF9)
            val paperBorder = if (isDark) Color(0xFF2E384D) else Color(0xFFE2E8F0)
            val lineBlue = if (isDark) Color(0xFF263248) else Color(0xFFE2E8F0)
            val marginRed = if (isDark) Color(0x66F87171) else Color(0x55EF4444)
            val ringColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
            val inkColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
            val accentGold = primaryColor
            val greenCheck = Color(0xFF22C55E)

            // 0. Soft ambient paper shadow for realistic physical depth
            drawRoundRect(
                color = Color.Black.copy(alpha = if (isDark) 0.35f else 0.08f),
                topLeft = Offset(3.dp.toPx(), 13.dp.toPx()),
                size = Size(padWidth - 2.dp.toPx(), padHeight - 11f),
                cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
            )

            // 1. Draw Notepad Paper Body
            drawRoundRect(
                color = paperBg,
                topLeft = Offset(0f, 10f),
                size = Size(padWidth, padHeight - 10f),
                cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
            )
            drawRoundRect(
                color = paperBorder,
                topLeft = Offset(0f, 10f),
                size = Size(padWidth, padHeight - 10f),
                cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 2. Draw Top Dual-Wire Spiral Binder Loops (Realistic notebook binding)
            val numRings = 7
            val ringSpacing = (padWidth - 44.dp.toPx()) / (numRings - 1)
            for (i in 0 until numRings) {
                val rx = 22.dp.toPx() + i * ringSpacing
                // Shadow under ring
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.2f),
                    topLeft = Offset(rx - 2.dp.toPx(), 4.dp.toPx()),
                    size = Size(5.dp.toPx(), 14.dp.toPx()),
                    cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
                )
                // Metallic ring
                drawRoundRect(
                    color = ringColor,
                    topLeft = Offset(rx - 3.dp.toPx(), 2.dp.toPx()),
                    size = Size(6.dp.toPx(), 16.dp.toPx()),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
            }

            // 3. Draw Vertical Margin Line (Double red accounting margin)
            val marginX = 40.dp.toPx()
            drawLine(
                color = marginRed,
                start = Offset(marginX, 20.dp.toPx()),
                end = Offset(marginX, padHeight - 8.dp.toPx()),
                strokeWidth = 1.2.dp.toPx()
            )
            drawLine(
                color = marginRed.copy(alpha = 0.4f),
                start = Offset(marginX + 2.5.dp.toPx(), 20.dp.toPx()),
                end = Offset(marginX + 2.5.dp.toPx(), padHeight - 8.dp.toPx()),
                strokeWidth = 0.8.dp.toPx()
            )

            // 4. Draw Horizontal Ruled Notebook Lines
            val lineY1 = 66.dp.toPx()
            val lineY2 = 98.dp.toPx()
            val lineY3 = 130.dp.toPx()
            val lineY4 = 158.dp.toPx()

            val lines = listOf(lineY1, lineY2, lineY3, lineY4)
            for (ly in lines) {
                drawLine(
                    color = lineBlue,
                    start = Offset(12.dp.toPx(), ly),
                    end = Offset(padWidth - 12.dp.toPx(), ly),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // 5. Draw Top Search Bar on Notepad
            val searchBarY = 22.dp.toPx()
            val searchBarHeight = 20.dp.toPx()
            val searchBarWidth = padWidth - marginX - 18.dp.toPx()

            drawRoundRect(
                color = if (isDark) Color(0xFF263043) else Color(0xFFF1F5F9),
                topLeft = Offset(marginX + 8.dp.toPx(), searchBarY),
                size = Size(searchBarWidth, searchBarHeight),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )

            val searchStyle = TextStyle(
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium,
                color = inkColor.copy(alpha = 0.8f)
            )

            // Determine dynamic food query text
            val userText = foodQuery?.trim()?.take(22)
            val fullSearchText = if (!userText.isNullOrBlank()) "Searching: $userText" else "Searching: Food items..."

            // PHASE 1: Searching food item (0f .. 0.30f)
            if (progress < 0.30f) {
                val sProg = (progress / 0.22f).coerceIn(0f, 1f)
                val searchChars = (fullSearchText.length * sProg).toInt().coerceIn(0, fullSearchText.length)
                val displayedSearch = fullSearchText.take(searchChars)
                if (displayedSearch.isNotEmpty()) {
                    drawText(
                        textMeasurer = textMeasurer,
                        text = displayedSearch,
                        topLeft = Offset(marginX + 28.dp.toPx(), searchBarY + 3.dp.toPx()),
                        style = searchStyle
                    )
                }

                // Magnifying glass sweeps smoothly over the search bar
                val searchPhase = progress / 0.30f
                val scanMinX = marginX + 18.dp.toPx()
                val scanMaxX = padWidth - 30.dp.toPx()
                val scanX = scanMinX + (scanMaxX - scanMinX) * (0.5f + 0.46f * sin(searchPhase * 2f * Math.PI.toFloat()))
                val scanY = searchBarY + searchBarHeight / 2f

                drawMagnifyingGlass(
                    center = Offset(scanX, scanY),
                    color = accentGold,
                    radius = 6.dp.toPx()
                )
            } else {
                // Docked matched food indicator
                val matchStyle = TextStyle(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CarbsGreen
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = if (!userText.isNullOrBlank()) "$userText: Found ✓" else "Food Database: Match OK ✓",
                    topLeft = Offset(marginX + 24.dp.toPx(), searchBarY + 3.dp.toPx()),
                    style = matchStyle
                )
                drawMagnifyingGlass(
                    center = Offset(marginX + 15.dp.toPx(), searchBarY + searchBarHeight / 2f),
                    color = CarbsGreen.copy(alpha = 0.8f),
                    radius = 4.dp.toPx()
                )
            }

            // Text Styles for Real Handwritten Words
            val textStartX = marginX + 10.dp.toPx()
            val line1Style = TextStyle(
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = inkColor
            )
            val line2Style = TextStyle(
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
            val line3Style = TextStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = ProteinBlue
            )

            val line1Full = if (!userText.isNullOrBlank()) "Portion: $userText" else "Portion: 250g / 1 serving"
            val line2Full = "Calories: 385 kcal"
            val line3Full = "Protein: 28g  Carbs: 45g  Fat: 12g"

            // Line 1: Portion Size (written during progress 0.32f..0.52f)
            var currentL1Width = 0f
            if (progress >= 0.32f) {
                val l1Prog = ((progress - 0.32f) / 0.20f).coerceIn(0f, 1f)
                val chars1 = (line1Full.length * l1Prog).toInt().coerceIn(0, line1Full.length)
                val text1 = line1Full.take(chars1)

                if (text1.isNotEmpty()) {
                    val layout = textMeasurer.measure(text1, line1Style)
                    currentL1Width = layout.size.width.toFloat()
                    drawText(
                        textMeasurer = textMeasurer,
                        text = text1,
                        topLeft = Offset(textStartX, lineY1 - 16.dp.toPx()),
                        style = line1Style
                    )
                }

                // Primary bullet dot
                drawCircle(
                    color = primaryColor.copy(alpha = 0.8f * l1Prog),
                    radius = 3.dp.toPx(),
                    center = Offset(marginX - 10.dp.toPx(), lineY1 - 8.dp.toPx())
                )
            }

            // Line 2: Calories (written during progress 0.54f..0.72f)
            var currentL2Width = 0f
            if (progress >= 0.54f) {
                val l2Prog = ((progress - 0.54f) / 0.18f).coerceIn(0f, 1f)
                val chars2 = (line2Full.length * l2Prog).toInt().coerceIn(0, line2Full.length)
                val text2 = line2Full.take(chars2)

                if (text2.isNotEmpty()) {
                    val layout = textMeasurer.measure(text2, line2Style)
                    currentL2Width = layout.size.width.toFloat()
                    drawText(
                        textMeasurer = textMeasurer,
                        text = text2,
                        topLeft = Offset(textStartX, lineY2 - 16.dp.toPx()),
                        style = line2Style
                    )
                }

                drawCircle(
                    color = primaryColor,
                    radius = 3.dp.toPx(),
                    center = Offset(marginX - 10.dp.toPx(), lineY2 - 8.dp.toPx())
                )
            }

            // Line 3: Protein, Carbs, Fat (written during progress 0.74f..0.88f)
            var currentL3Width = 0f
            if (progress >= 0.74f) {
                val l3Prog = ((progress - 0.74f) / 0.14f).coerceIn(0f, 1f)
                val chars3 = (line3Full.length * l3Prog).toInt().coerceIn(0, line3Full.length)
                val text3 = line3Full.take(chars3)

                if (text3.isNotEmpty()) {
                    val layout = textMeasurer.measure(text3, line3Style)
                    currentL3Width = layout.size.width.toFloat()
                    drawText(
                        textMeasurer = textMeasurer,
                        text = text3,
                        topLeft = Offset(textStartX, lineY3 - 16.dp.toPx()),
                        style = line3Style
                    )
                }

                drawCircle(
                    color = ProteinBlue,
                    radius = 3.dp.toPx(),
                    center = Offset(marginX - 10.dp.toPx(), lineY3 - 8.dp.toPx())
                )
            }

            // Double underline & checkmark (progress >= 0.88f)
            if (progress >= 0.88f) {
                val doneProg = ((progress - 0.88f) / 0.12f).coerceIn(0f, 1f)
                val underY = lineY4 - 16.dp.toPx()
                val totalUnderlineWidth = textMeasurer.measure(line3Full, line3Style).size.width.toFloat()
                val underEnd = textStartX + totalUnderlineWidth * doneProg

                drawLine(
                    color = inkColor.copy(alpha = 0.65f * doneProg),
                    start = Offset(textStartX, underY),
                    end = Offset(underEnd, underY),
                    strokeWidth = 1.2.dp.toPx()
                )
                drawLine(
                    color = inkColor.copy(alpha = 0.65f * doneProg),
                    start = Offset(textStartX, underY + 2.5.dp.toPx()),
                    end = Offset(underEnd, underY + 2.5.dp.toPx()),
                    strokeWidth = 1.2.dp.toPx()
                )

                // Green checkmark
                if (doneProg > 0.4f) {
                    val checkCenter = Offset(padWidth - 26.dp.toPx(), lineY3 - 8.dp.toPx())
                    drawCheckmark(center = checkCenter, color = greenCheck)
                }
            }

            // 6. Draw Realistic Fountain Pen actively writing on paper
            if (progress >= 0.30f) {
                val fullL1W = textMeasurer.measure(line1Full, line1Style).size.width.toFloat()
                val fullL2W = textMeasurer.measure(line2Full, line2Style).size.width.toFloat()
                val fullL3W = textMeasurer.measure(line3Full, line3Style).size.width.toFloat()

                val (penX, penY) = when {
                    // Pen descends to Line 1
                    progress in 0.30f..0.32f -> {
                        val p = (progress - 0.30f) / 0.02f
                        val startX = textStartX + 40.dp.toPx()
                        val startY = lineY1 - 25.dp.toPx()
                        val targetX = textStartX
                        val targetY = lineY1 - 8.dp.toPx()
                        Pair(startX + (targetX - startX) * p, startY + (targetY - startY) * p)
                    }
                    // Writing Line 1
                    progress in 0.32f..0.52f -> {
                        val p = (progress - 0.32f) / 0.20f
                        val x = textStartX + currentL1Width
                        val y = lineY1 - 8.dp.toPx() + sin(p * 32f) * 1.5f
                        Pair(x, y)
                    }
                    // Transition to Line 2 (lifting pen in smooth 3D arc)
                    progress in 0.52f..0.54f -> {
                        val p = (progress - 0.52f) / 0.02f
                        val startX = textStartX + fullL1W
                        val startY = lineY1 - 8.dp.toPx()
                        val targetX = textStartX
                        val targetY = lineY2 - 8.dp.toPx()
                        val x = startX + (targetX - startX) * p
                        val y = startY + (targetY - startY) * p - 8.dp.toPx() * sin(p * Math.PI.toFloat())
                        Pair(x, y)
                    }
                    // Writing Line 2
                    progress in 0.54f..0.72f -> {
                        val p = (progress - 0.54f) / 0.18f
                        val x = textStartX + currentL2Width
                        val y = lineY2 - 8.dp.toPx() + sin(p * 32f) * 1.5f
                        Pair(x, y)
                    }
                    // Transition to Line 3 (lifting pen in smooth 3D arc)
                    progress in 0.72f..0.74f -> {
                        val p = (progress - 0.72f) / 0.02f
                        val startX = textStartX + fullL2W
                        val startY = lineY2 - 8.dp.toPx()
                        val targetX = textStartX
                        val targetY = lineY3 - 8.dp.toPx()
                        val x = startX + (targetX - startX) * p
                        val y = startY + (targetY - startY) * p - 8.dp.toPx() * sin(p * Math.PI.toFloat())
                        Pair(x, y)
                    }
                    // Writing Line 3
                    progress in 0.74f..0.88f -> {
                        val p = (progress - 0.74f) / 0.14f
                        val x = textStartX + currentL3Width
                        val y = lineY3 - 8.dp.toPx() + sin(p * 32f) * 1.5f
                        Pair(x, y)
                    }
                    // Lifted up after calculation done
                    else -> {
                        Pair(textStartX + fullL3W + 12.dp.toPx(), lineY3 - 18.dp.toPx())
                    }
                }

                val isLifted = progress >= 0.88f || progress in 0.30f..0.32f || progress in 0.52f..0.54f || progress in 0.72f..0.74f

                drawRealisticPen(
                    tip = Offset(penX, penY),
                    penColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B),
                    accentColor = accentGold,
                    isLifted = isLifted
                )
            }
        }
    }
}

private fun DrawScope.drawMagnifyingGlass(
    center: Offset,
    color: Color,
    radius: Float = 7.dp.toPx()
) {
    // Glass reflection arc
    drawArc(
        color = color.copy(alpha = 0.4f),
        startAngle = 180f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = center - Offset(radius * 0.75f, radius * 0.75f),
        size = Size(radius * 1.5f, radius * 1.5f),
        style = Stroke(width = 1.2.dp.toPx())
    )
    // Lens circle
    drawCircle(
        color = color,
        radius = radius,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )
    // Handle at 45 degrees
    val handleStart = center + Offset(radius * 0.707f, radius * 0.707f)
    val handleEnd = handleStart + Offset(6.dp.toPx(), 6.dp.toPx())
    drawLine(
        color = color,
        start = handleStart,
        end = handleEnd,
        strokeWidth = 2.5.dp.toPx(),
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawRealisticPen(
    tip: Offset,
    penColor: Color,
    accentColor: Color,
    isLifted: Boolean
) {
    val liftOffset = if (isLifted) Offset(4.dp.toPx(), -7.dp.toPx()) else Offset.Zero
    val actualTip = tip + liftOffset

    // 1. Dynamic Contact Shadow on Paper below the nib
    val shadowOffset = if (isLifted) Offset(5.dp.toPx(), 8.dp.toPx()) else Offset(1.5.dp.toPx(), 2.dp.toPx())
    val shadowAlpha = if (isLifted) 0.12f else 0.35f
    val shadowRadius = if (isLifted) 4.5.dp.toPx() else 2.5.dp.toPx()
    drawCircle(
        color = Color.Black.copy(alpha = shadowAlpha),
        radius = shadowRadius,
        center = tip + shadowOffset
    )

    // Pen angled at 45 degrees
    val angleRad = Math.toRadians(45.0).toFloat()
    val cosA = cos(angleRad)
    val sinA = sin(angleRad)

    // Nib (Realistic Metallic Fountain Pen Nib with center ink slit)
    val nibLength = 7.dp.toPx()
    val nibBase = actualTip + Offset(nibLength * cosA, -nibLength * sinA)

    val pathNib = Path().apply {
        moveTo(actualTip.x, actualTip.y)
        lineTo(nibBase.x - 2.5.dp.toPx() * sinA, nibBase.y - 2.5.dp.toPx() * cosA)
        lineTo(nibBase.x + 2.5.dp.toPx() * sinA, nibBase.y + 2.5.dp.toPx() * cosA)
        close()
    }
    drawPath(path = pathNib, color = accentColor, style = Fill)

    // Nib Center Ink Slit
    drawLine(
        color = penColor.copy(alpha = 0.7f),
        start = actualTip,
        end = actualTip + Offset(4.dp.toPx() * cosA, -4.dp.toPx() * sinA),
        strokeWidth = 0.8.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Gold grip band
    val bandEnd = nibBase + Offset(3.5.dp.toPx() * cosA, -3.5.dp.toPx() * sinA)
    drawLine(
        color = accentColor,
        start = nibBase,
        end = bandEnd,
        strokeWidth = 4.5.dp.toPx(),
        cap = StrokeCap.Square
    )

    // Pen Body / Barrel
    val bodyEnd = bandEnd + Offset(24.dp.toPx() * cosA, -24.dp.toPx() * sinA)
    drawLine(
        color = penColor,
        start = bandEnd,
        end = bodyEnd,
        strokeWidth = 5.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Metallic clip on pen barrel
    val clipStart = bodyEnd - Offset(6.dp.toPx() * cosA, -6.dp.toPx() * sinA)
    val clipEnd = clipStart + Offset(3.dp.toPx() * sinA, 3.dp.toPx() * cosA)
    drawLine(
        color = accentColor,
        start = clipStart,
        end = clipEnd,
        strokeWidth = 1.8.dp.toPx(),
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawCheckmark(
    center: Offset,
    color: Color
) {
    val checkPath = Path().apply {
        moveTo(center.x - 5.dp.toPx(), center.y)
        lineTo(center.x - 1.5.dp.toPx(), center.y + 4.dp.toPx())
        lineTo(center.x + 6.dp.toPx(), center.y - 5.dp.toPx())
    }
    drawPath(
        path = checkPath,
        color = color,
        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
    )
}
