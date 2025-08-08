package com.proxilocal.ui.components

import android.content.Context
import androidx.compose.ui.geometry.Offset
import com.proxilocal.hyperlocal.DotPositionStore
import com.proxilocal.hyperlocal.MatchResult
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Stable, first-placement-only positions:
 *  - Use cached XY if present
 *  - Otherwise compute once from persisted angle + initial RSSI (with collision-aware radial nudges)
 *  - Save to PositionCache; later frames reuse without recalculation
 */
object DotLayout {

    private const val EMERGENCY_ANGLE_NUDGE_DEG = 7.5f

    fun rssiToNorm(rssi: Int): Float {
        val minRssi = -90f
        val maxRssi = -30f
        return ((rssi - minRssi) / (maxRssi - minRssi)).coerceIn(0f, 1f)
    }

    fun computePositions(
        context: Context,
        matches: List<MatchResult>,
        width: Float,
        height: Float,
        dotRadiusPx: Float
    ): Map<String, Offset> {
        if (matches.isEmpty()) return emptyMap()

        // Scope cache to this canvas size
        PositionCache.setCanvasSize(width.toInt(), height.toInt())

        val center = Offset(width / 2f, height / 2f)
        val maxRadius = min(width, height) / 2f
        val minSep = dotRadiusPx * 2.6f
        val placed = LinkedHashMap<String, Offset>(matches.size)

        // Use a stable order to avoid chain reactions on first layout
        val ordered = matches.sortedWith(compareBy<MatchResult> {
            DotPositionStore.getAngle(context, it.id) ?: ((it.id.hashCode() and 0x7fffffff) % 360).toFloat()
        }.thenBy { it.id })

// imports if needed:
// import kotlin.math.cos
// import kotlin.math.sin

        for (m in ordered) {
            // 0) Use cached XY if we already placed this id
            val cached = PositionCache.get(m.id)
            if (cached != null) {
                placed[m.id] = cached
                continue
            }

            // 1) Fixed (persisted) angle; fall back to id hash once, then persist
            val persisted = DotPositionStore.getAngle(context, m.id)
            val angleDeg = persisted ?: ((m.id.hashCode() and 0x7fffffff) % 360).toFloat()

            // 2) Base radius from (first-seen) RSSI
            val baseRadius = maxRadius * (1f - rssiToNorm(m.distanceRssi))

            // 3) Radial collision resolution at this fixed angle (one-time)
            val delta = dotRadiusPx * 1.25f
            val attempts = ArrayList<Float>(1 + 6 + 6).apply {
                add(0f)
                for (i in 1..6) add(i * delta)    // outward
                for (i in 1..6) add(-i * delta)   // inward
            }

            var pos: Offset? = null
            val rad = Math.toRadians(angleDeg.toDouble()).toFloat()
            for (dr in attempts) {
                val r = (baseRadius + dr).coerceIn(dotRadiusPx * 3, maxRadius - dotRadiusPx * 3)
                val candidate = Offset(
                    x = center.x + r * cos(rad),
                    y = center.y + r * sin(rad)
                )
                if (isClear(candidate, placed.values, minSep)) {
                    pos = candidate
                    break
                }
            }

            if (pos == null) {
                // Last resort: tiny angle nudge
                val rad1 = Math.toRadians((angleDeg + EMERGENCY_ANGLE_NUDGE_DEG).toDouble()).toFloat()
                val r = baseRadius
                pos = Offset(
                    x = center.x + r * cos(rad1),
                    y = center.y + r * sin(rad1)
                )
                // Do NOT persist this emergency change
            } else if (persisted == null) {
                // Persist the chosen fixed angle for future runs
                DotPositionStore.putAngle(context, m.id, angleDeg)
            }

            // 4) Cache final position and reuse forever (until size changes)
            PositionCache.put(m.id, pos!!)
            placed[m.id] = pos
        }

        // Optional cache cleanup
        // PositionCache.prune(placed.keys)

        return placed
    }

    private fun isClear(candidate: Offset, existing: Collection<Offset>, minSep: Float): Boolean {
        for (e in existing) {
            val dx = candidate.x - e.x
            val dy = candidate.y - e.y
            if (dx * dx + dy * dy < (minSep * minSep)) return false
        }
        return true
    }
}
