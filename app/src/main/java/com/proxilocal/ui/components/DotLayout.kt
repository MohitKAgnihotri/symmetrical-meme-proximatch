package com.proxilocal.ui.components

import android.content.Context
import android.util.Log
import androidx.compose.ui.geometry.Offset
import com.proxilocal.hyperlocal.MatchResult
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private const val TAG = "DotLayout"

/**
 * Computes stable positions for radar dots inside a w x h canvas.
 * All math is defensive: never throws even if the box is tiny or padding exceeds size.
 */
object DotLayout {

    /**
     * @param w canvas width in px
     * @param h canvas height in px
     * @param dotRadiusPx visual (approx) radius of a dot for hit testing / spacing
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

        // Keep at least a dot radius of inset so we don't draw under edges
        val inset = max(1f, dotRadiusPx)

        val cx = width / 2f
        val cy = height / 2f
        val maxRadius = min(width, height) / 2f - inset

        // If we have no drawable radius (too small), park dots at center and bail
        if (maxRadius <= 0f) {
            if (matches.isNotEmpty()) {
                Log.w(TAG, "Drawable area too small (w=$width, h=$height, inset=$inset). Parking ${matches.size} dots at center.")
            }
            return matches.associate { it.id to Offset(cx, cy) }
        }

        // Arrange around concentric rings; bucket by index to spread evenly
        val ringCount = max(1, when {
            matches.size <= 6 -> 1
            matches.size <= 18 -> 2
            else -> 3
        })

        // Small inner padding between rings
        val ringPadding = (dotRadiusPx * 1.5f).coerceAtMost(maxRadius / ringCount)
        val ringStep = max((maxRadius - ringPadding) / ringCount, 1f)

        val positions = LinkedHashMap<String, Offset>(matches.size)
        matches.forEachIndexed { idx, m ->
            val ring = idx % ringCount
            val r = max(1f, ringPadding + ringStep * (ring + 1))

            // Distribute around the circle
            val perRing = max(1, (matches.size + ringCount - 1) / ringCount)
            val theta = (idx / ringCount) * (2 * PI / perRing)

            var x = cx + (r * cos(theta)).toFloat()
            var y = cy + (r * sin(theta)).toFloat()

            // Safe clamp: if min > max (tiny layout), swap to avoid IllegalArgumentException
            x = safeClamp(x, inset, width - inset)
            y = safeClamp(y, inset, height - inset)

            positions[m.id] = Offset(x, y)
        }
        return positions
    }

    private fun safeClamp(value: Float, minV: Float, maxV: Float): Float {
        return if (minV <= maxV) {
            value.coerceIn(minV, maxV)
        } else {
            // Swap the bounds to avoid "empty range" — layout is smaller than insets.
            value.coerceIn(maxV, minV)
        }
    }
}
