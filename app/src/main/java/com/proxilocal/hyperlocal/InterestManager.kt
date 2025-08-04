package com.proxilocal.hyperlocal

import android.content.Context
import androidx.core.content.edit

object InterestManager {
    private const val PREFS_NAME = "ProxiInterests"
    private const val SENT_KEY = "sent_interest_set"

    fun saveInterest(context: Context, id: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(SENT_KEY, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        current.add(id)
        prefs.edit {
            putStringSet(SENT_KEY, current)
        }
    }

    fun hasSentInterest(context: Context, id: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(SENT_KEY, emptySet())?.contains(id) == true
    }

    fun getSentInterests(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(SENT_KEY, emptySet()) ?: emptySet()
    }
}
