package com.example.hyperlocal.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.hyperlocal.MatchResult
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.min

/**
 * A wrapper composable that integrates the themed RadarCanvas
 * (named ThemedRadarCanvas) from RadarThemed.kt and handles tap detection on dots.
 */
@Composable
fun RadarCanvas(
    theme: RadarTheme,
    matches: List<MatchResult>,
    onDotTapped: (MatchResult) -> Unit,
    modifier: Modifier = Modifier
) {
    // Calculate how many dots fit in the rings
    val dotCount = matches.size.coerceAtMost(theme.circleCount)
    // Pre-calc dot radius in pixels for hit testing
    val density = LocalDensity.current
    val dotRadiusPx = with(density) { theme.dotRadiusDp.toPx() * 1.2f }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp)
            // Capture taps and forward to onDotTapped when hitting a dot
            .pointerInput(matches) {
                detectTapGestures { tapOffset: Offset ->
                    val w = size.width
                    val h = size.height
                    val center = Offset(w / 2f, h / 2f)
                    val maxR = min(w, h) / 2f
                    val baseRadius = maxR / (theme.circleCount + 1)
                    matches.take(dotCount).forEachIndexed { index, match ->
                        val angleRad = Math.toRadians(index * 360.0 / dotCount)
                        val r = baseRadius * (index + 1)
                        val x = center.x + (r * cos(angleRad)).toFloat()
                        val y = center.y + (r * sin(angleRad)).toFloat()
                        val dx = tapOffset.x - x
                        val dy = tapOffset.y - y
                        if (dx * dx + dy * dy <= dotRadiusPx * dotRadiusPx) {
                            onDotTapped(match)
                        }
                    }
                }
            }
    ) {
        // Delegate to the themed RadarCanvas implementation
        ThemedRadarCanvas(
            theme = theme,
            matches = matches,
            dotCount = dotCount,
            modifier = Modifier.fillMaxSize()
        )
    }
}
