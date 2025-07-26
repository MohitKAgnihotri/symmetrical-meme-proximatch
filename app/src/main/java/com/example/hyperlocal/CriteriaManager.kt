package com.example.hyperlocal

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlin.experimental.or

object CriteriaManager {

    private const val PREFS_NAME = "ProxiMatchPrefs"
    private const val KEY_PREFERENCES = "criteria_prefs"

    /**
     * Converts selected criteria into a compact byte array (bitmask).
     * Example: 32 booleans -> 4 bytes.
     */
    fun encodeCriteria(selectedIndices: List<Int>, size: Int = 32): ByteArray {
        val bits = ByteArray(size / 8)
        selectedIndices.forEach { index ->
            val byteIndex = index / 8
            val bitPosition = index % 8
            bits[byteIndex] = bits[byteIndex] or (1 shl bitPosition).toByte()
        }
        return bits
    }

    fun decodeCriteria(data: ByteArray): List<Boolean> {
        val result = mutableListOf<Boolean>()
        for (byte in data) {
            for (i in 0 until 8) {
                result.add((byte.toInt() shr i) and 1 == 1)
            }
        }
        return result.take(32) // truncate to expected size
    }

    fun saveUserPreferences(context: Context, selectedIndices: List<Int>) {
        val encoded = encodeCriteria(selectedIndices)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_PREFERENCES, encoded.joinToString(",") { it.toString() })
        }
    }

    fun getUserPreferences(context: Context): List<Boolean> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PREFERENCES, null)
            ?: return List(32) { false } // default: none selected
        val byteArray = raw.split(",").map { it.toByte() }.toByteArray()
        return decodeCriteria(byteArray)
    }

    fun calculateMatchPercentage(
        userPrefs: List<Boolean>,
        otherPrefs: List<Boolean>
    ): Int {
        val matches = userPrefs.zip(otherPrefs).count { it.first && it.second }
        val total = userPrefs.count { it }
        return if (total == 0) 0 else (matches * 100 / total)
    }

    fun getEncodedCriteria(context: Context): ByteArray {
        val prefs = getUserPreferences(context)
        val selectedIndices = prefs.mapIndexedNotNull { index, selected ->
            if (selected) index else null
        }
        return encodeCriteria(selectedIndices)
    }

}


