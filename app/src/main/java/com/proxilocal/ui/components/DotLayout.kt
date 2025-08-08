package com.proxilocal.ui.components

import android.content.Context
import android.util.Log
import androidx.compose.ui.geometry.Offset
import com.proxilocal.hyperlocal.MatchResult
import com.proxilocal.hyperlocal.DotPositionStore
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private const val TAG = "DotLayout"

/**
 * Computes positions for radar dots inside a w x h canvas.
 * Now distance-aware: radius is derived from RSSI.
 * Angle is stable per peer via DotPositionStore.
 * Jitter is reduced with PositionMemory.
 */
object DotLayout {

    // Tunables for RSSI → radius mapping (dBm)
    // Adjust to your environment if needed.
    private const val RSSI_CLOSE = -45  // very close
    private const val RSSI_FAR   = -95  // far edge
    private const val INNER_PAD_SCALE = 1.8f // keep some breathing room near center

    /**
     * @param w canvas width in px
     * @param h canvas height in px
     * @param dotRadiusPx visual radius for hit testing / spacing
     */
    fun computePositions(
        context: Context,
        matches: List<MatchResult>,
        w: Float,
        h: Float,
        dotRadiusPx: Float
    ): Map<String, Offset> {
        val width = w.coerceAtLeast(1f)
        val height = h.coerceAtLeast(1f)

        // inset so we don't draw under edges
        val inset = max(1f, dotRadiusPx)

        val cx = width / 2f
        val cy = height / 2f
        val maxRadius = min(width, height) / 2f - inset

        if (maxRadius <= 0f) {
            if (matches.isNotEmpty()) {
                Log.w(TAG, "Drawable area too small (w=$width, h=$height, inset=$inset). Parking ${matches.size} dots at center.")
            }
            return matches.associate { it.id to Offset(cx, cy) }
        }

        // Keep a small inner dead-zone so dots don't stack at exact center
        val innerRadius = (dotRadiusPx * INNER_PAD_SCALE).coerceAtMost(maxRadius * 0.35f)

        val positions = LinkedHashMap<String, Offset>(matches.size)

        // One pass: resolve angle (stable), radius (from RSSI), then smooth with PositionMemory
        matches.forEachIndexed { index, m ->
            // 1) Stable angle per id (persisted). If missing, derive from hash, then save.
            val storedDeg = DotPositionStore.getAngle(context, m.id)
            val angleDeg = storedDeg ?: run {
                val derived = ((m.id.hashCode() and 0x7FFFFFFF) % 360).toFloat()
                DotPositionStore.putAngle(context, m.id, derived)
                derived
            }
            val theta = angleDeg * (PI / 180f)

            // 2) Distance-aware radius from RSSI
            val rssi = m.distanceRssi.coerceIn(RSSI_FAR, RSSI_CLOSE)
            val t = (rssi - RSSI_CLOSE).toFloat() / (RSSI_FAR - RSSI_CLOSE).toFloat() // 0 (close) .. 1 (far)
            val targetRadius = innerRadius + t * (maxRadius - innerRadius)

            // 3) Convert polar → cartesian
            var x = cx + (targetRadius * cos(theta)).toFloat()
            var y = cy + (targetRadius * sin(theta)).toFloat()

            // Edge safety
            x = safeClamp(x, inset, width - inset)
            y = safeClamp(y, inset, height - inset)

            // 4) Jitter smoothing (optional but nice)
            val smoothed = PositionMemory.resolve(
                id = m.id,
                proposedPos = Offset(x, y),
                proposedRadiusPx = targetRadius
            )

            positions[m.id] = smoothed
        }

        // Optional housekeeping so memory doesn’t grow forever
        PositionMemory.prune(matches.map { it.id }.toSet())

        return positions
    }

    private fun safeClamp(value: Float, minV: Float, maxV: Float): Float {
        return if (minV <= maxV) value.coerceIn(minV, maxV) else value.coerceIn(maxV, minV)
    }
}
