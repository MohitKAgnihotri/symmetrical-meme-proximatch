package com.proxilocal.ui.components

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.proxilocal.hyperlocal.MatchUiState

/**
 * Radar canvas with hit-testing for:
 *  - Dot taps (existing)
 *  - Ring taps (Phase 6): when canTapRings(id) is true, expand hit-box and call onRingsTap(id)
 */
@Composable
fun RadarCanvas(
    theme: RadarTheme,
    matches: List<MatchUiState>,                  // UI state (used by renderer and hit-testing)
    isSweeping: Boolean,
    onDotTapped: (MatchUiState) -> Unit,          // existing behavior: open like dialog
    // NEW (Phase 6): enable “tap the rings to connect” when true for this id
    canTapRings: (id: String) -> Boolean,
    onRingsTap: (id: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // Keep a "forgiving" dot radius for hit-testing
    val baseDotR = with(density) { theme.dotRadiusDp.toPx() }
    val dotHitR = baseDotR * 1.4f

    // Corona for ring taps (roughly matches the visuals in ThemedRadarCanvas)
    val ringInnerR = baseDotR * 1.6f      // just outside the dot
    val ringOuterR = baseDotR * 7.0f      // around the outer animated ring

    var pingingMatchId by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier.pointerInput(matches, canTapRings) {
            awaitPointerEventScope {
                while (true) {
                    // Read at the Initial pass so children don't steal it first
                    val down = awaitFirstDown(pass = PointerEventPass.Initial)
                    android.util.Log.d(
                        "RadarCanvas",
                        "tap down at (${down.position.x.toInt()}, ${down.position.y.toInt()}) size=(${size.width}, ${size.height}) matches=${matches.size}"
                    )

                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val positions = DotLayout.computePositions(
                        context = context,
                        matches = matches.map { it.match }, // layout uses raw MatchResult
                        w = w,
                        h = h,
                        dotRadiusPx = baseDotR
                    )

                    // 1) Dot hit-test first (highest priority)
                    val dotHit = matches.firstOrNull { ui ->
                        val p = positions[ui.match.id] ?: return@firstOrNull false
                        val dx = down.position.x - p.x
                        val dy = down.position.y - p.y
                        dx * dx + dy * dy <= dotHitR * dotHitR
                    }

                    if (dotHit != null) {
                        down.consume()
                        android.util.Log.d("RadarCanvas", "HIT dot -> ${dotHit.match.id}")
                        pingingMatchId = dotHit.match.id
                        onDotTapped(dotHit)
                        android.widget.Toast
                            .makeText(context, "Tapped ${dotHit.match.id.take(6)}…", android.widget.Toast.LENGTH_SHORT)
                            .show()
                        waitForUpOrCancellation()
                        continue
                    }

                    // 2) Phase 6: Rings corona hit-test (only if canTapRings says it's tappable)
                    val ringsHit = matches.firstOrNull { ui ->
                        if (!canTapRings(ui.match.id)) return@firstOrNull false
                        val p = positions[ui.match.id] ?: return@firstOrNull false
                        val dx = down.position.x - p.x
                        val dy = down.position.y - p.y
                        val d2 = dx * dx + dy * dy
                        d2 >= (ringInnerR * ringInnerR) && d2 <= (ringOuterR * ringOuterR)
                    }

                    if (ringsHit != null) {
                        down.consume()
                        android.util.Log.d("RadarCanvas", "HIT rings -> ${ringsHit.match.id}")
                        onRingsTap(ringsHit.match.id)
                        waitForUpOrCancellation()
                    } else {
                        android.util.Log.d("RadarCanvas", "MISS (no dot/rings)")
                        // Don’t consume; just finish the gesture so the loop continues cleanly
                        waitForUpOrCancellation()
                    }
                }
            }
        }
    ) {
        ThemedRadarCanvas(
            theme = theme,
            matches = matches,                  // pass UI list through to renderer
            isSweeping = isSweeping,
            pingingMatchId = pingingMatchId,
            onPingCompleted = { pingingMatchId = null },
            modifier = Modifier.fillMaxSize()
        )
    }
}
