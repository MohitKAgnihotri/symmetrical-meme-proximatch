package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.hyperlocal.Gender
import com.example.hyperlocal.MatchResult
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class RingStyle { SOLID, DASHED, GRADIENT }

data class RadarTheme(
    val bgGradient: Pair<Color, Color>,
    val ringStyle: RingStyle,
    val ringPrimary: Color,
    val ringAccent: Color,
    val sweepColor: Color,
    val sweepWidth: Float,
    val labelTextStyle: TextStyle,
    val circleCount: Int = 4,
    val dotRadiusDp: Dp = 8.dp,
    val maleColor: Color = Color(0xFF03A9F4),
    val femaleColor: Color = Color(0xFFE91E63),
    val privateColor: Color = Color.Gray,
    val rainbowColors: List<Color> = listOf(Color.Red, Color.Yellow, Color.Green, Color.Blue, Color.Magenta)
)

object ThemeProvider {
    val NeonTech = RadarTheme(
        bgGradient = Color(0xFF1B1F3B) to Color(0xFF101229),
        ringStyle = RingStyle.GRADIENT,
        ringPrimary = Color.Cyan,
        ringAccent = Color.Green,
        sweepColor = Color.Cyan.copy(alpha = 0.5f),
        sweepWidth = 20f,
        labelTextStyle = TextStyle(color = Color.White)
    )
    val CorporatePulse = RadarTheme(
        bgGradient = Color.White to Color(0xFFEEEEEE),
        ringStyle = RingStyle.GRADIENT,
        ringPrimary = Color(0xFF333333),
        ringAccent = Color(0xFF7E57C2),
        sweepColor = Color(0xFF7E57C2).copy(alpha = 0.5f),
        sweepWidth = 30f,
        labelTextStyle = TextStyle(color = Color.DarkGray)
    )
}

@Composable
fun ThemedRadarCanvas(
    theme: RadarTheme,
    matches: List<MatchResult>,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val center = Offset(w / 2, h / 2)
        val maxRadius = min(w, h) / 2f

        val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
        val sweepAngle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ), label = "SweepAngle"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(colors = listOf(theme.bgGradient.first, theme.bgGradient.second), center = center, radius = maxRadius),
                size = size
            )
            (1..theme.circleCount).forEach { i ->
                drawCircle(
                    color = theme.ringPrimary.copy(alpha = 0.3f),
                    center = center,
                    radius = maxRadius * i / theme.circleCount,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(Color.Transparent, theme.sweepColor.copy(alpha = 0.4f)),
                    center = center
                ),
                startAngle = sweepAngle,
                sweepAngle = theme.sweepWidth,
                useCenter = true,
            )
        }

        Layout(
            content = {
                matches.forEach { match ->
                    MatchDot(theme = theme, match = match)
                }
            }
        ) { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            layout(constraints.maxWidth, constraints.maxHeight) {
                placeables.forEachIndexed { index, placeable ->
                    val match = matches[index]
                    val normalizedDistance = ((match.distanceRssi - -90f) / (-30f - -90f)).coerceIn(0f, 1f)
                    val radius = maxRadius * (1 - normalizedDistance)
                    val angleRad = (match.id.hashCode() % 360) * (Math.PI / 180)
                    val x = center.x + (radius * cos(angleRad)).toFloat()
                    val y = center.y + (radius * sin(angleRad)).toFloat()
                    placeable.placeRelative(
                        x = (x - placeable.width / 2).toInt(),
                        y = (y - placeable.height / 2).toInt()
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchDot(
    theme: RadarTheme,
    match: MatchResult
) {
    val dotRadiusPx = with(LocalDensity.current) { theme.dotRadiusDp.toPx() }

    val infiniteTransition = rememberInfiniteTransition(label = "DotPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "PulseAlpha"
    )

    Canvas(modifier = Modifier.size(theme.dotRadiusDp * 4)) {
        val dotCenter = center

        if (match.matchPercentage > 85) {
            val baseGlowColor = when (match.gender) {
                Gender.MALE -> theme.maleColor
                Gender.FEMALE -> theme.femaleColor
                Gender.PRIVATE -> theme.privateColor
                Gender.LGBTQ_PLUS -> theme.rainbowColors.first()
            }
            val glowColor = lerp(baseGlowColor.copy(alpha = 0.4f), baseGlowColor, match.matchPercentage / 100f)
            drawCircle(
                color = glowColor.copy(alpha = pulseAlpha),
                center = dotCenter,
                radius = dotRadiusPx * 2.0f
            )
        }

        val baseDotColor = when (match.gender) {
            Gender.MALE -> theme.maleColor
            Gender.FEMALE -> theme.femaleColor
            Gender.PRIVATE -> theme.privateColor
            Gender.LGBTQ_PLUS -> Color.Transparent
        }
        val paleColor = baseDotColor.copy(alpha = 0.4f)
        val matchColor = lerp(paleColor, baseDotColor, match.matchPercentage / 100f)

        if (match.gender == Gender.LGBTQ_PLUS) {
            drawCircle(
                brush = Brush.sweepGradient(theme.rainbowColors, center = dotCenter),
                center = dotCenter,
                radius = dotRadiusPx
            )
        } else {
            drawCircle(
                color = matchColor,
                center = dotCenter,
                radius = dotRadiusPx
            )
        }
    }
}