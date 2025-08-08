package com.proxilocal.ui.components

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
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
    // Make taps a bit forgiving; tune as needed during testing
    val dotRadiusPx = with(density) { theme.dotRadiusDp.toPx() * 1.4f }

    var pingingMatchId by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier.pointerInput(matches) {
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
                    val positions = DotLayout.computePositions(context, matches, w, h, dotRadiusPx)

                    // Hit-test
                    val hit = matches.firstOrNull { m ->
                        val p = positions[m.id]
                        if (p == null) false
                        else {
                            val dx = down.position.x - p.x
                            val dy = down.position.y - p.y
                            dx * dx + dy * dy <= dotRadiusPx * dotRadiusPx
                        }
                    }

                    if (hit != null) {
                        // We handled it: consume + show feedback
                        down.consume()
                        android.util.Log.d("RadarCanvas", "HIT -> ${hit.id}")
                        pingingMatchId = hit.id
                        onDotTapped(hit)
                        android.widget.Toast
                            .makeText(context, "Tapped ${hit.id.take(6)}…", android.widget.Toast.LENGTH_SHORT)
                            .show()
                        // finish the gesture sequence
                        waitForUpOrCancellation()
                    } else {
                        // Do NOT consume; let underlying UI get the tap
                        android.util.Log.d("RadarCanvas", "MISS (no dot)")
                        // Still complete the sequence so we keep the loop timing sane
                        waitForUpOrCancellation()
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
