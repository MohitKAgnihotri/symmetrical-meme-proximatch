package com.example.hyperlocal.ui

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hyperlocal.MatchResult
import kotlin.math.*

@Composable
fun RadarCanvas(matches: List<MatchResult>, onDotTapped: (MatchResult) -> Unit) {
    val sweepRadius = remember { Animatable(0f) }

    val pulseAnim = rememberInfiniteTransition()
    val pulseAlpha by pulseAnim.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    LaunchedEffect(Unit) {
        while (true) {
            sweepRadius.snapTo(0f)
            sweepRadius.animateTo(
                targetValue = 400f,
                animationSpec = tween(durationMillis = 1800, easing = LinearEasing)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .background(Color.Black)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)

            // Draw radar rings
            repeat(4) { i ->
                drawCircle(
                    color = Color.DarkGray,
                    radius = (i + 1) * size.minDimension / 8,
                    center = center,
                    style = Stroke(width = 2f)
                )
            }

            // Radar sweep
            drawCircle(
                color = Color(0x33FFFFFF),
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
                val color = when (match.colorCode) {
                    "Green" -> Color.Green
                    "Yellow" -> Color.Yellow
                    else -> Color.Gray
                }

                drawCircle(color = color, radius = 12f, center = Offset(x, y))

                val distance = sqrt((x - center.x).pow(2) + (y - center.y).pow(2))
                if (abs(sweepRadius.value - distance) < 15f) {
                    Log.d("RadarCanvas", "Ping! ${match.id}")
                }
            }
        }

        // Tappable dots (if any)
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

        // Fallback UI when no matches
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
