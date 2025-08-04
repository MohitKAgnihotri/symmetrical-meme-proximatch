package com.proxilocal.hyperlocal

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import java.util.concurrent.TimeUnit

/**
 * Manages user interactions (interests) sent to other users, stored locally in SharedPreferences.
 * Interests are stored with timestamps and can be cleaned up after expiration.
 */
object InterestManager {
    private const val PREFS_NAME = "ProxiInterestPrefs"
    private const val KEY_SENT_INTERESTS = "sent_interests"
    private const val TAG = "InterestManager"
    private const val EXPIRATION_DAYS = 7L // Interests expire after 7 days

    /**
     * Saves an interest sent to another user, including a timestamp for expiration.
     *
     * @param context The application context.
     * @param userId The ID of the user to whom the interest was sent.
     */
    fun saveInterest(context: Context, userId: String) {
        if (userId.isBlank()) {
            Log.w(TAG, "Attempted to save empty or blank userId")
            return
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentInterests = getSentInterests(context).toMutableSet()
        val timestamp = System.currentTimeMillis()
        currentInterests.add("$userId:$timestamp")
        cleanupExpiredInterests(currentInterests)
        prefs.edit {
            putStringSet(KEY_SENT_INTERESTS, currentInterests)
        }
        Log.d(TAG, "Saved interest for user: $userId")
    }

    /**
     * Checks if an interest has been sent to a specific user.
     *
     * @param context The application context.
     * @param userId The ID of the user to check.
     * @return True if an interest was sent and is still valid, false otherwise.
     */
    fun hasSentInterest(context: Context, userId: String): Boolean {
        if (userId.isBlank()) {
            Log.w(TAG, "Checked empty or blank userId")
            return false
        }
        val interests = getSentInterests(context)
        return interests.any { it.startsWith("$userId:") }
    }

    /**
     * Retrieves all valid (non-expired) sent interests.
     *
     * @param context The application context.
     * @return A set of user IDs for whom interests were sent and are still valid.
     */
    fun getSentInterests(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val interests = prefs.getStringSet(KEY_SENT_INTERESTS, emptySet())?.toMutableSet() ?: mutableSetOf()
        cleanupExpiredInterests(interests)
        prefs.edit {
            putStringSet(KEY_SENT_INTERESTS, interests)
        }
        return interests.mapNotNull { it.split(":").firstOrNull() }.toSet()
    }

    /**
     * Removes expired interests from the given set based on their timestamps.
     *
     * @param interests The mutable set of interest entries (format: "userId:timestamp").
     */
    private fun cleanupExpiredInterests(interests: MutableSet<String>) {
        val expirationThreshold = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(EXPIRATION_DAYS)
        interests.removeAll { entry ->
            val parts = entry.split(":")
            if (parts.size != 2) {
                Log.w(TAG, "Invalid interest entry format: $entry")
                return@removeAll true
            }
            try {
                val timestamp = parts[1].toLong()
                timestamp < expirationThreshold
            } catch (e: NumberFormatException) {
                Log.e(TAG, "Invalid timestamp in interest entry: $entry", e)
                true
            }
        }
    }
}