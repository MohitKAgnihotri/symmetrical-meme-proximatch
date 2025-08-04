package com.proxilocal.hyperlocal

import android.content.Context
import java.util.*

object UserIdManager {
    private const val PREFS_NAME = "ProxiUserId"
    private const val KEY_ID = "anon_id"

    fun getOrGenerateId(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_ID, null)
        val id = existing ?: UUID.randomUUID().toString().take(8).also {
            prefs.edit().putString(KEY_ID, it).apply()
        }
        return id.toByteArray()
    }
}
