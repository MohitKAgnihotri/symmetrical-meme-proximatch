package com.proxilocal.ui.components

import androidx.compose.ui.geometry.Offset
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Small helper that remembers last known positions/radii per peer id
 * and applies hysteresis + gentle interpolation to suppress jitter.
 */
object PositionMemory {

    private data class Entry(
        var pos: Offset,
        var radiusPx: Float
    )

    private val map = ConcurrentHashMap<String, Entry>()

    // Hysteresis: ignore changes smaller than this many pixels
    private const val RADIUS_DEADBAND_PX = 6f

    // When we do accept an update, glide part-way toward it (0..1)
    private const val LERP_FACTOR = 0.25f

    fun resolve(id: String, proposedPos: Offset, proposedRadiusPx: Float): Offset {
        val e = map[id]
        if (e == null) {
            map[id] = Entry(proposedPos, proposedRadiusPx)
            return proposedPos
        }
        val deltaR = proposedRadiusPx - e.radiusPx
        val accept = kotlin.math.abs(deltaR) > RADIUS_DEADBAND_PX

        if (accept) {
            // glide radius toward the new one
            val newR = e.radiusPx + deltaR * LERP_FACTOR
            // move position proportionally (straight-line toward proposed)
            val dx = proposedPos.x - e.pos.x
            val dy = proposedPos.y - e.pos.y
            val newPos = Offset(e.pos.x + dx * LERP_FACTOR, e.pos.y + dy * LERP_FACTOR)
            e.radiusPx = newR
            e.pos = newPos
        }
        // If not accepted, keep the old entry as-is
        return e.pos
    }

    /** Optional: call occasionally to trim memory */
    fun prune(idsToKeep: Set<String>) {
        val iter = map.keys.iterator()
        while (iter.hasNext()) {
            val k = iter.next()
            if (k !in idsToKeep) iter.remove()
        }
    }

    fun clear() { map.clear() }
}
