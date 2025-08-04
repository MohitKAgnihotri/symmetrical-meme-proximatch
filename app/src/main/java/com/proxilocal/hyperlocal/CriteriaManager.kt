package com.proxilocal.hyperlocal

import android.content.Context
import androidx.core.content.edit

object CriteriaManager {

    private const val PREFS_NAME = "ProxiMatchPrefs"
    private const val KEY_GENDER = "user_gender"
    private const val KEY_MY_CRITERIA = "my_criteria"
    private const val KEY_THEIR_CRITERIA = "their_criteria"

    /**
     * Saves the entire UserProfile object to SharedPreferences.
     */
    fun saveUserProfile(context: Context, profile: UserProfile) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(KEY_GENDER, profile.gender.name)
            // Convert boolean lists to comma-separated strings for efficient storage
            putString(KEY_MY_CRITERIA, profile.myCriteria.joinToString(","))
            putString(KEY_THEIR_CRITERIA, profile.theirCriteria.joinToString(","))
        }
    }

    /**
     * Loads the UserProfile from SharedPreferences. Returns null if no profile is found.
     */
    fun getUserProfile(context: Context): UserProfile? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val genderStr = prefs.getString(KEY_GENDER, null) ?: return null
        val myCriteriaStr = prefs.getString(KEY_MY_CRITERIA, null) ?: return null
        val theirCriteriaStr = prefs.getString(KEY_THEIR_CRITERIA, null) ?: return null

        return UserProfile(
            gender = Gender.valueOf(genderStr),
            myCriteria = myCriteriaStr.split(',').map { it.toBoolean() },
            theirCriteria = theirCriteriaStr.split(',').map { it.toBoolean() }
        )
    }

    /**
     * Converts selected criteria (a list of booleans) into a compact byte array (bitmask).
     * Supports up to 64 criteria, resulting in an 8-byte array.
     */
    fun encodeCriteria(selectedCriteria: List<Boolean>): ByteArray {
        val bits = ByteArray(8) // 64 bits = 8 bytes
        selectedCriteria.forEachIndexed { index, isSelected ->
            if (isSelected) {
                val byteIndex = index / 8
                val bitPosition = index % 8
                bits[byteIndex] = (bits[byteIndex].toInt() or (1 shl bitPosition)).toByte()
            }
        }
        return bits
    }

    /**
     * Decodes a byte array (bitmask) back into a list of 64 booleans.
     */
    fun decodeCriteria(data: ByteArray): List<Boolean> {
        val result = mutableListOf<Boolean>()
        for (byte in data) {
            for (i in 0 until 8) {
                result.add((byte.toInt() shr i) and 1 == 1)
            }
        }
        return result.take(64) // Ensure the list is exactly 64 items long
    }

    /**
     * Calculates the match percentage between two users' preferences.
     */
    fun calculateMatchPercentage(
        userPrefs: List<Boolean>,
        otherPrefs: List<Boolean>
    ): Int {
        val matches = userPrefs.zip(otherPrefs).count { it.first && it.second }
        val total = userPrefs.count { it }
        return if (total == 0) 0 else (matches * 100 / total)
    }
}