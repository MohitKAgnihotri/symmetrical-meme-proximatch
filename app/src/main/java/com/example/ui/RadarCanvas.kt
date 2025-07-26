package com.example.hyperlocal

import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

@Composable
fun RadarCanvas(matches: List<MatchResult>, onDotTapped: (MatchResult) -> Unit) {
    val sweepRadius = remember { androidx.compose.animation.core.Animatable(0f) }

    val pulseAnim = androidx.compose.animation.core.rememberInfiniteTransition()
    val pulseAlpha by pulseAnim.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        )
    )

    LaunchedEffect(Unit) {
        while (true) {
            sweepRadius.snapTo(0f)
            sweepRadius.animateTo(
                targetValue = 400f,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 1800, easing = androidx.compose.animation.core.LinearEasing)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF2B2D42), Color(0xFF000000)),
                    center = Offset(200f, 200f),
                    radius = 600f
                )
            )

    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)

            // Draw radar rings with solid low-alpha white stroke
            repeat(4) { i ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.2f),
                    radius = (i + 1) * size.minDimension / 5.5f,
                    center = center,
                    style = Stroke(width = 2f)
                )
            }


            // Radar sweep with glow
            drawCircle(
                color = Color(0x44A5D6FF),
                radius = sweepRadius.value,
                center = center
            )

            // Plot matches using RSSI if available
            matches.forEachIndexed { index, match ->
                val angle = Math.toRadians((index * 360.0 / matches.size))
                val rssiNormalized = ((127 + (match.rssi ?: -100)).coerceIn(20, 100)) / 100f
                val radius = rssiNormalized * (size.minDimension / 2.2f)
                val x = center.x + radius * cos(angle).toFloat()
                val y = center.y + radius * sin(angle).toFloat()
                val color = try { match.colorCode as Color } catch (e: Exception) { Color.Gray }

                drawCircle(
                    color = color,
                    radius = 14f,
                    center = Offset(x, y)
                )
                drawCircle(
                    color = color.copy(alpha = 0.3f),
                    radius = 24f,
                    center = Offset(x, y),
                    style = Stroke(width = 2f)
                )
            }
        }

        // Tappable dots overlay
        matches.forEachIndexed { index, match ->
            val angle = Math.toRadians((index * 360.0 / matches.size))
            val rssiNormalized = ((127 + (match.rssi ?: -100)).coerceIn(20, 100)) / 100f
            val radius = rssiNormalized * 180f
            val x = radius * cos(angle).toFloat()
            val y = radius * sin(angle).toFloat()
            Box(
                modifier = Modifier
                    .offset { IntOffset((x + 200).toInt(), (y + 200).toInt()) }
                    .size(40.dp)
                    .clickable { onDotTapped(match) }
            )
        }

        if (matches.isEmpty()) {
            Text(
                text = "Searching for nearby matches...",
                color = Color.White.copy(alpha = pulseAlpha),
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
