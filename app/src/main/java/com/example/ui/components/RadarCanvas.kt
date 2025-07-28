package com.example.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.hyperlocal.MatchResult
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun RadarCanvas(
    theme: RadarTheme,
    matches: List<MatchResult>,
    onDotTapped: (MatchResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val dotRadiusPx = with(density) { theme.dotRadiusDp.toPx() * 1.2f } // For hit testing

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp)
            .pointerInput(matches) {
                detectTapGestures { tapOffset: Offset ->
                    val w = size.width
                    val h = size.height
                    val center = Offset(w / 2f, h / 2f)
                    val maxRadius = min(w, h) / 2f

                    matches.forEach { match ->
                        // Re-calculate dot position for hit-testing
                        val normalizedDistance = ((match.distanceRssi - -90f) / (-30f - -90f)).coerceIn(0f, 1f)
                        val radius = maxRadius * (1 - normalizedDistance)
                        val angleRad = (match.id.hashCode() % 360) * (Math.PI / 180)
                        val x = center.x + (radius * cos(angleRad)).toFloat()
                        val y = center.y + (radius * sin(angleRad)).toFloat()

                        val dx = tapOffset.x - x
                        val dy = tapOffset.y - y
                        if (dx * dx + dy * dy <= dotRadiusPx * dotRadiusPx) {
                            onDotTapped(match)
                        }
                    }
                }
            }
    ) {
        // --- FIX 2: REMOVED dotCount PARAMETER ---
        ThemedRadarCanvas(
            theme = theme,
            matches = matches,
            modifier = Modifier.fillMaxSize()
        )
    }
}