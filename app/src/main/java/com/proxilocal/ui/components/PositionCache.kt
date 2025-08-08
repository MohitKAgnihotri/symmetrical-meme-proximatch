package com.proxilocal.ui.components

import androidx.compose.ui.geometry.Offset
import java.util.concurrent.ConcurrentHashMap

/**
 * Remembers a fixed on-screen position for each peer id, scoped to the
 * current canvas size. If the size changes, entries are invalidated.
 */
object PositionCache {

    private data class Entry(
        val width: Int,
        val height: Int,
        val pos: Offset
    )

    private val map = ConcurrentHashMap<String, Entry>()
    private var lastWidth: Int = -1
    private var lastHeight: Int = -1

    /** Must be called once per layout pass so we can invalidate when size changes. */
    fun setCanvasSize(width: Int, height: Int) {
        if (width != lastWidth || height != lastHeight) {
            // Invalidate all cached positions if the canvas size changed
            map.clear()
            lastWidth = width
            lastHeight = height
        }
    }

    fun get(id: String): Offset? = map[id]?.pos

    fun put(id: String, pos: Offset) {
        if (lastWidth <= 0 || lastHeight <= 0) return
        map[id] = Entry(lastWidth, lastHeight, pos)
    }

    /** Optional: prune entries for ids no longer present. */
    fun prune(idsToKeep: Set<String>) {
        val it = map.keys.iterator()
        while (it.hasNext()) {
            val k = it.next()
            if (k !in idsToKeep) it.remove()
        }
    }

    fun clear() = map.clear()
}
