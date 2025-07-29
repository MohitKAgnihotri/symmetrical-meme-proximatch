package com.example.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import com.example.hyperlocal.MatchResult
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun RadarCanvas(
    theme: RadarTheme,
    matches: List<MatchResult>,
    isSweeping: Boolean,
    onDotTapped: (MatchResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val dotRadiusPx = with(density) { theme.dotRadiusDp.toPx() * 1.2f }

    // --- NEW: State to track which dot is currently "pinging" ---
    var pingingMatchId by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .pointerInput(matches) {
                detectTapGestures { tapOffset: Offset ->
                    val w = size.width
                    val h = size.height
                    val center = Offset(w / 2f, h / 2f)
                    val maxRadius = min(w, h) / 2f

                    matches.forEach { match ->
                        val normalizedDistance = ((match.distanceRssi - -90f) / (-30f - -90f)).coerceIn(0f, 1f)
                        val radius = maxRadius * (1 - normalizedDistance)
                        val angleRad = (match.id.hashCode() % 360) * (Math.PI / 180)
                        val x = center.x + (radius * cos(angleRad)).toFloat()
                        val y = center.y + (radius * sin(angleRad)).toFloat()

                        val dx = tapOffset.x - x
                        val dy = tapOffset.y - y
                        if (dx * dx + dy * dy <= dotRadiusPx * dotRadiusPx) {
                            // --- NEW: Trigger the ping and then show the dialog ---
                            pingingMatchId = match.id
                            onDotTapped(match)
                        }
                    }
                }
            }
    ) {
        ThemedRadarCanvas(
            theme = theme,
            matches = matches,
            isSweeping = isSweeping,
            pingingMatchId = pingingMatchId, // Pass the pinging ID down
            onPingCompleted = { pingingMatchId = null }, // Callback to reset the ping
            modifier = Modifier.fillMaxSize()
        )
    }
}