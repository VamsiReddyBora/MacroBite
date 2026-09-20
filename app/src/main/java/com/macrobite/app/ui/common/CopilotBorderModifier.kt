package com.macrobite.app.ui.common

import android.graphics.Matrix
import android.graphics.SweepGradient
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * VS Code GitHub Copilot-Style Rotating Border
 * Smoothly orbits an accent light beam around the perimeter of the rounded text box.
 * Matches the user-selected theme color.
 */
fun Modifier.copilotRotatingBorder(
    baseBorderColor: Color,
    accentColor: Color,
    cornerRadius: Dp = 26.dp,
    borderWidth: Dp = 1.3.dp
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "copilot_rotation")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "copilot_angle"
    )

    this.drawWithContent {
        drawContent()

        val strokePx = borderWidth.toPx()
        val radiusPx = cornerRadius.toPx()
        val w = size.width
        val h = size.height

        if (w > 0f && h > 0f) {
            // 1. Base subtle border
            drawRoundRect(
                color = baseBorderColor,
                topLeft = Offset(strokePx / 2f, strokePx / 2f),
                size = Size(w - strokePx, h - strokePx),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
                style = Stroke(width = strokePx)
            )

            // 2. Animated sweep gradient matrix
            val matrix = Matrix()
            matrix.postRotate(angle, w / 2f, h / 2f)

            val sweepShader = SweepGradient(
                w / 2f,
                h / 2f,
                intArrayOf(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                    accentColor.copy(alpha = 0.2f).toArgb(),
                    accentColor.copy(alpha = 0.85f).toArgb(),
                    accentColor.toArgb(),
                    accentColor.copy(alpha = 0.85f).toArgb(),
                    accentColor.copy(alpha = 0.2f).toArgb(),
                    android.graphics.Color.TRANSPARENT
                ),
                floatArrayOf(0.0f, 0.65f, 0.78f, 0.88f, 0.92f, 0.96f, 0.99f, 1.0f)
            )
            sweepShader.setLocalMatrix(matrix)

            // 3. Rotating glowing accent border
            drawRoundRect(
                brush = ShaderBrush(sweepShader),
                topLeft = Offset(strokePx / 2f, strokePx / 2f),
                size = Size(w - strokePx, h - strokePx),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
                style = Stroke(width = strokePx)
            )
        }
    }
}
