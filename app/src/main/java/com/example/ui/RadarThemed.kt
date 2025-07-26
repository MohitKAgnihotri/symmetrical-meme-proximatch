package com.example.hyperlocal.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.hyperlocal.MatchResult
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// 1. Data classes / enums for theming

enum class RingStyle { SOLID, DASHED, GRADIENT }

data class RadarTheme(
    val bgGradient: Pair<Color, Color>,
    val ringStyle: RingStyle,
    val ringPrimary: Color,
    val ringAccent: Color,
    val sweepColor: Color,
    val sweepWidth: Float,
    val dotPalette: List<Color>,
    val labelTextStyle: TextStyle,
    val circleCount: Int = 4,
    // radius of each dot before scaling (in dp)
    val dotRadiusDp: Dp = 8.dp
)

// 2. ThemeProvider with Neon Tech and Corporate Pulse

object ThemeProvider {
    val NeonTech = RadarTheme(
        bgGradient = Color(0xFF1B1F3B) to Color(0xFF101229),
        ringStyle = RingStyle.GRADIENT,
        ringPrimary = Color.Cyan,
        ringAccent = Color.Green,
        sweepColor = Color.Cyan.copy(alpha = 0.5f),
        sweepWidth = 20f,
        dotPalette = listOf(Color(0xFF00FF00), Color(0xFF00FFAA), Color(0xFF00EE00)),
        labelTextStyle = TextStyle(color = Color.White),
        circleCount = 4,
        dotRadiusDp = 8.dp
    )

    val CorporatePulse = RadarTheme(
        bgGradient = Color.White to Color(0xFFEEEEEE),
        ringStyle = RingStyle.GRADIENT,
        ringPrimary = Color(0xFF333333),
        ringAccent = Color(0xFF7E57C2),
        sweepColor = Color(0xFF7E57C2).copy(alpha = 0.5f),
        sweepWidth = 30f,
        dotPalette = listOf(Color(0xFF4CAF50), Color(0xFFFFC107), Color(0xFFF44336)),
        labelTextStyle = TextStyle(color = Color.DarkGray),
        circleCount = 4,
        dotRadiusDp = 8.dp
    )
}

// 3. Interpolation helper

private fun interpolateColors(
    start: Color,
    end: Color,
    steps: Int
): List<Color> = List(steps) { i ->
    val t = i.toFloat() / (steps - 1)
    lerp(start, end, t)
}

// 4. Themed Radar Canvas composable

@Composable
fun ThemedRadarCanvas(
    theme: RadarTheme,
    matches: List<MatchResult>,
    dotCount: Int,
    modifier: Modifier = Modifier
) {
    // Animate sweep
    val transition = rememberInfiniteTransition()
    val sweepAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val density = LocalDensity.current
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2, h / 2)
        val maxR = min(w, h) / 2

        // Background gradient
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(theme.bgGradient.first, theme.bgGradient.second),
                center = center,
                radius = maxR
            ),
            size = size
        )

        // Compute ring colors
        val ringColors = if (theme.ringStyle == RingStyle.GRADIENT) {
            interpolateColors(theme.ringPrimary, theme.ringAccent, theme.circleCount)
        } else List(theme.circleCount) { theme.ringPrimary }

        // Draw rings
        ringColors.forEachIndexed { i, color ->
            val r = maxR * (i + 1) / (theme.circleCount + 1)
            drawCircle(
                color = color,
                center = center,
                radius = r,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Sweep beam
        drawArc(
            brush = Brush.radialGradient(
                colors = listOf(theme.sweepColor, Color.Transparent),
                center = center,
                radius = maxR
            ),
            startAngle = sweepAngle,
            sweepAngle = theme.sweepWidth,
            useCenter = true
        )

        // Draw dots
        matches.take(dotCount).forEachIndexed { idx, match ->
            val angleRad = Math.toRadians(idx * 360.0 / dotCount)
            val r = maxR * (idx + 1) / (theme.circleCount + 1)
            val x = center.x + r * cos(angleRad).toFloat()
            val y = center.y + r * sin(angleRad).toFloat()
            val color = theme.dotPalette[idx % theme.dotPalette.size]
            drawCircle(
                color = color,
                center = Offset(x, y),
                radius = theme.dotRadiusDp.toPx()
            )
            // Optional label
            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    "${match.matchPercentage}%",
                    x + 4.dp.toPx(),
                    y - 4.dp.toPx(),
                    android.graphics.Paint().apply {
                        textSize = 12.dp.toPx()
                    }
                )
            }
        }
    }
}
