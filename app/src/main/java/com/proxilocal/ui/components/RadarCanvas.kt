package com.proxilocal.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.proxilocal.hyperlocal.MatchResult

@Composable
fun RadarCanvas(
    theme: RadarTheme,
    matches: List<MatchResult>,
    isSweeping: Boolean,
    onDotTapped: (MatchResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    // Slightly bigger than visual dot so taps are forgiving
    val dotRadiusPx = with(density) { theme.dotRadiusDp.toPx() * 1.2f }

    var pingingMatchId by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier.pointerInput(matches) {
            // Only reacts when a dot is actually hit; otherwise lets events pass through
            detectTapGestures { tapOffset: Offset ->
                val w = size.width.toFloat()
                val h = size.height.toFloat()
                val positions = DotLayout.computePositions(context, matches, w, h, dotRadiusPx)
                // Hit test using the same positions used for drawing
                matches.forEach { match ->
                    val pos = positions[match.id] ?: return@forEach
                    val dx = tapOffset.x - pos.x
                    val dy = tapOffset.y - pos.y
                    if (dx * dx + dy * dy <= dotRadiusPx * dotRadiusPx) {
                        pingingMatchId = match.id
                        onDotTapped(match)
                        return@detectTapGestures
                    }
                }
            }
        }
    ) {
        ThemedRadarCanvas(
            theme = theme,
            matches = matches,
            isSweeping = isSweeping,
            pingingMatchId = pingingMatchId,
            onPingCompleted = { pingingMatchId = null },
            modifier = Modifier.fillMaxSize()
        )
    }
}
