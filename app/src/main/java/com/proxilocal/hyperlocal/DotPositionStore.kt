package com.proxilocal.hyperlocal

import android.content.Context
import android.util.Log
import kotlin.math.abs

/**
 * Persists a stable polar angle (in degrees) per peer ID so their dot
 * stays in a consistent spot across sessions.
 */
object DotPositionStore {
    private const val PREFS = "DotPositions"
    private const val TAG = "DotPositionStore"

    /** Returns a stored angle for id in degrees, or null if none. */
    fun getAngle(context: Context, id: String): Float? {
        return try {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (!prefs.contains(id)) null else prefs.getFloat(id, Float.NaN).takeIf { !it.isNaN() }
        } catch (e: Exception) {
            Log.w(TAG, "getAngle failed for $id", e); null
        }
    }

    /**
     * Save angle in degrees for id. We only write if it changed meaningfully
     * to reduce disk churn.
     */
    fun putAngle(context: Context, id: String, angleDeg: Float) {
        try {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val old = prefs.getFloat(id, Float.NaN)
            if (old.isNaN() || abs(normalizeDeg(old) - normalizeDeg(angleDeg)) > 0.5f) {
                prefs.edit().putFloat(id, normalizeDeg(angleDeg)).apply()
            }
        } catch (e: Exception) {
            Log.w(TAG, "putAngle failed for $id", e)
        }
    }

    /** Remove angles no longer needed (optional housekeeping). */
    fun pruneExcept(context: Context, idsToKeep: Set<String>) {
        try {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val all = prefs.all.keys
            val toRemove = all.filter { it !in idsToKeep }
            if (toRemove.isNotEmpty()) {
                val edit = prefs.edit()
                toRemove.forEach { edit.remove(it) }
                edit.apply()
            }
        } catch (e: Exception) {
            Log.w(TAG, "pruneExcept failed", e)
        }
    }

    private fun normalizeDeg(deg: Float): Float {
        var d = deg % 360f
        if (d < 0f) d += 360f
        return d
    }
}
