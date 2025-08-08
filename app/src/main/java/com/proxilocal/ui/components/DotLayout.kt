package com.proxilocal.ui.components

import android.content.Context
import androidx.compose.ui.geometry.Offset
import com.proxilocal.hyperlocal.DotPositionStore
import com.proxilocal.hyperlocal.MatchResult
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Stable, collision-aware positions:
 *  - FIX: stable placement order (by stored angle then id)
 *  - FIX: angle is fixed per id; collisions resolved radially
 *  - FIX: radius jitter suppressed via PositionMemory hysteresis
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

        val center = Offset(width / 2f, height / 2f)
        val maxRadius = min(width, height) / 2f
        val minSep = dotRadiusPx * 2.6f
        val placed = LinkedHashMap<String, Offset>(matches.size)

        // FIX 1: use a stable order: by stored angle (or hash fallback), then by id
        val ordered = matches.sortedWith(compareBy<MatchResult> {
            DotPositionStore.getAngle(context, it.id) ?: ((it.id.hashCode() and 0x7fffffff) % 360).toFloat()
        }.thenBy { it.id })

        for (m in ordered) {
            // Fixed, persisted angle
            val persisted = DotPositionStore.getAngle(context, m.id)
            var angleDeg = persisted ?: ((m.id.hashCode() and 0x7fffffff) % 360).toFloat()

            // Base radius from (smoothed) RSSI
            val baseRadius = maxRadius * (1f - rssiToNorm(m.distanceRssi))

            // Try slight radial nudges (±N * delta) keeping angle constant
            val delta = dotRadiusPx * 1.25f
            val attempts = ArrayList<Float>(1 + 6 + 6).apply {
                add(0f)
                for (i in 1..6) add(i * delta)    // outward
                for (i in 1..6) add(-i * delta)   // inward
            }

            var pos: Offset? = null
            var chosenR = baseRadius
            for (dr in attempts) {
                val r = (baseRadius + dr).coerceIn(dotRadiusPx * 3, maxRadius - dotRadiusPx * 3)
                val rad = Math.toRadians(angleDeg.toDouble()).toFloat()
                val candidate = Offset(
                    x = center.x + r * cos(rad),
                    y = center.y + r * sin(rad)
                )
                if (isClear(candidate, placed.values, minSep)) {
                    pos = candidate
                    chosenR = r
                    break
                }
            }

            // Last resort: tiny angle nudge if everything collided
            if (pos == null) {
                val rad1 = Math.toRadians((angleDeg + EMERGENCY_ANGLE_NUDGE_DEG).toDouble()).toFloat()
                val r = baseRadius
                pos = Offset(
                    x = center.x + r * cos(rad1),
                    y = center.y + r * sin(rad1)
                )
                // do NOT persist angle changes on emergency fallback
            } else if (persisted == null) {
                // Persist the fixed angle the first time we resolve it
                DotPositionStore.putAngle(context, m.id, angleDeg)
            }

            // FIX 2: apply hysteresis/glide to suppress visual jitter on radius changes
            val stablePos = PositionMemory.resolve(m.id, pos!!, chosenR)
            placed[m.id] = stablePos
        }

        // Optional cleanup
        // PositionMemory.prune(placed.keys)  // call occasionally if you want
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
