package com.example.hyperlocal.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.hyperlocal.MatchResult
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun RadarCanvas(
    matches: List<MatchResult>,
    onDotTapped: (MatchResult) -> Unit
) {
    // Infinite animations
    val sweepTransition = rememberInfiniteTransition()
    val sweepAngle by sweepTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val pulseTransition = rememberInfiniteTransition()
    val pulseFactor by pulseTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val loadingAlpha by pulseTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(matches) {
                    detectTapGestures { tapOffset ->
                        val width = size.width
                        val height = size.height
                        val center = Offset(width / 2f, height / 2f)
                        val maxRadius = min(width, height) / 2f
                        val dotRadius = 12.dp.toPx() * pulseFactor
                        matches.forEachIndexed { index, match ->
                            val angleRad = Math.toRadians(index * 360.0 / matches.size)
                            val spacing = maxRadius / (matches.size + 1)
                            val r = spacing * (index + 1)
                            val x = center.x + (r * cos(angleRad)).toFloat()
                            val y = center.y + (r * sin(angleRad)).toFloat()
                            val dist = sqrt((tapOffset.x - x).let { it * it } + (tapOffset.y - y).let { it * it })
                            if (dist <= dotRadius) onDotTapped(match)
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val center = Offset(width / 2f, height / 2f)
            val maxRadius = min(width, height) / 2f

            // Background radial gradient
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF2B2D42), Color.Black),
                    center = center,
                    radius = maxRadius,
                    tileMode = TileMode.Clamp
                ),
                size = size
            )

            // Concentric rings
            repeat(4) { i ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.2f),
                    center = center,
                    radius = maxRadius * (i + 1) / 4f,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Rotating sweep beam
            drawArc(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x44A5D6FF), Color.Transparent),
                    center = center,
                    radius = maxRadius,
                    tileMode = TileMode.Clamp
                ),
                startAngle = sweepAngle,
                sweepAngle = 30f,
                useCenter = true
            )

            // Pulsing match dots
            matches.forEachIndexed { index, match ->
                val angleRad = Math.toRadians(index * 360.0 / matches.size)
                val spacing = maxRadius / (matches.size + 1)
                val r = spacing * (index + 1)
                val x = center.x + (r * cos(angleRad)).toFloat()
                val y = center.y + (r * sin(angleRad)).toFloat()
                val color = when (match.colorCode) {
                    "Green" -> Color.Green
                    "Yellow" -> Color.Yellow
                    else -> Color.Gray
                }
                drawCircle(
                    color = color,
                    center = Offset(x, y),
                    radius = 12.dp.toPx() * pulseFactor
                )
            }
        }

        if (matches.isEmpty()) {
            Text(
                text = "Searching for nearby matches...",
                color = Color.White.copy(alpha = loadingAlpha),
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
