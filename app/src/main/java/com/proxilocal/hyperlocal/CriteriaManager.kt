package com.proxilocal.hyperlocal

import android.content.Context
import android.util.Log
import androidx.core.content.edit

object CriteriaManager {

    private const val PREFS_NAME = "ProxiMatchPrefs"
    private const val KEY_GENDER = "user_gender"
    private const val KEY_MY_CRITERIA = "my_criteria"
    private const val KEY_THEIR_CRITERIA = "their_criteria"
    private const val TAG = "CriteriaManager"
    private const val EXPECTED_CRITERIA_COUNT = 64

    /**
     * Saves the entire UserProfile object to SharedPreferences.
     *
     * @param context The application context.
     * @param profile The UserProfile containing gender and criteria to save.
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
     * Loads the UserProfile from SharedPreferences. Returns a default UserProfile if data is missing or invalid.
     *
     * @param context The application context.
     * @return A UserProfile instance, or a default profile if data is unavailable or corrupted.
     */
    fun getUserProfile(context: Context): UserProfile {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val genderStr = prefs.getString(KEY_GENDER, null)
        val myCriteriaStr = prefs.getString(KEY_MY_CRITERIA, null)
        val theirCriteriaStr = prefs.getString(KEY_THEIR_CRITERIA, null)

        // Return default profile if any data is missing
        if (genderStr == null || myCriteriaStr == null || theirCriteriaStr == null) {
            Log.w(TAG, "Missing profile data in SharedPreferences, returning default profile")
            return createDefaultProfile()
        }

        return try {
            val gender = Gender.valueOf(genderStr)
            val myCriteria = parseCriteria(myCriteriaStr)
            val theirCriteria = parseCriteria(theirCriteriaStr)
            UserProfile(gender, myCriteria, theirCriteria)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing UserProfile from SharedPreferences", e)
            createDefaultProfile()
        }
    }

    /**
     * Converts selected criteria (a list of booleans) into a compact byte array (bitmask).
     * Supports up to 64 criteria, resulting in an 8-byte array.
     *
     * @param selectedCriteria A list of booleans representing user criteria (true = selected).
     * @return An 8-byte array encoding the criteria as a bitmask.
     * @throws IllegalArgumentException if the input list size is not 64.
     */
    fun encodeCriteria(selectedCriteria: List<Boolean>): ByteArray {
        if (selectedCriteria.size != EXPECTED_CRITERIA_COUNT) {
            throw IllegalArgumentException("Criteria list must contain exactly $EXPECTED_CRITERIA_COUNT elements, got ${selectedCriteria.size}")
        }
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
     *
     * @param data An 8-byte array containing the encoded criteria.
     * @return A list of 64 booleans, or a default list if the input is invalid.
     */
    fun decodeCriteria(data: ByteArray): List<Boolean> {
        if (data.size != 8) {
            Log.e(TAG, "Invalid byte array size for decoding: ${data.size} bytes, expected 8")
            return List(EXPECTED_CRITERIA_COUNT) { false }
        }
        val result = mutableListOf<Boolean>()
        for (byte in data) {
            for (i in 0 until 8) {
                result.add((byte.toInt() shr i) and 1 == 1)
            }
        }
        return result.take(EXPECTED_CRITERIA_COUNT) // Ensure exactly 64 items
    }

    /**
     * Calculates the match percentage between two users' preferences.
     *
     * @param userPrefs The user's preferred criteria for others (list of booleans).
     * @param otherPrefs The other user's criteria (list of booleans).
     * @return The match percentage (0-100), or 0 if no preferences are selected.
     * @throws IllegalArgumentException if the input lists have different sizes or are not 64 elements.
     */
    fun calculateMatchPercentage(userPrefs: List<Boolean>, otherPrefs: List<Boolean>): Int {
        if (userPrefs.size != otherPrefs.size || userPrefs.size != EXPECTED_CRITERIA_COUNT) {
            throw IllegalArgumentException(
                "Criteria lists must both contain exactly $EXPECTED_CRITERIA_COUNT elements, " +
                        "got ${userPrefs.size} and ${otherPrefs.size}"
            )
        }
        val matches = userPrefs.zip(otherPrefs).count { it.first && it.second }
        val total = userPrefs.count { it }
        return if (total == 0) 0 else (matches * 100 / total)
    }

    /**
     * Parses a comma-separated string of booleans into a list of booleans.
     *
     * @param criteriaStr The comma-separated string (e.g., "true,false,true").
     * @return A list of booleans, or a default list if parsing fails.
     */
    private fun parseCriteria(criteriaStr: String): List<Boolean> {
        return try {
            val criteria = criteriaStr.split(',').map { it.toBoolean() }
            if (criteria.size != EXPECTED_CRITERIA_COUNT) {
                Log.e(TAG, "Invalid criteria size: ${criteria.size}, expected $EXPECTED_CRITERIA_COUNT")
                List(EXPECTED_CRITERIA_COUNT) { false }
            } else {
                criteria
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing criteria string: $criteriaStr", e)
            List(EXPECTED_CRITERIA_COUNT) { false }
        }
    }

    /**
     * Creates a default UserProfile for fallback scenarios.
     *
     * @return A UserProfile with default values (Gender.PRIVATE, empty criteria).
     */
    private fun createDefaultProfile(): UserProfile {
        return UserProfile(
            gender = Gender.PRIVATE,
            myCriteria = List(EXPECTED_CRITERIA_COUNT) { false },
            theirCriteria = List(EXPECTED_CRITERIA_COUNT) { false }
        )
    }
}