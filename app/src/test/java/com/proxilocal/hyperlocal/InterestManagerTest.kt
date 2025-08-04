package com.proxilocal.hyperlocal

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.concurrent.TimeUnit

class InterestManagerTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        prefs = mock(SharedPreferences::class.java)
        editor = mock(SharedPreferences.Editor::class.java)
        `when`(context.getSharedPreferences(anyString(), anyInt())).thenReturn(prefs)
        `when`(prefs.edit()).thenReturn(editor)
        `when`(editor.putStringSet(anyString(), any())).thenReturn(editor)
        `when`(editor.apply()).then { /* No-op */ }
    }

    @Test
    fun `saveInterest stores valid userId with timestamp`() {
        val userId = "user123"
        val interests = mutableSetOf<String>()
        `when`(prefs.getStringSet(eq("sent_interests"), any())).thenReturn(interests)

        InterestManager.saveInterest(context, userId)

        assertTrue(interests.any { it.startsWith("$userId:") })
        verify(editor).putStringSet(eq("sent_interests"), any())
        verify(editor).apply()
    }

    @Test
    fun `saveInterest ignores blank userId`() {
        val interests = mutableSetOf<String>()
        `when`(prefs.getStringSet(eq("sent_interests"), any())).thenReturn(interests)

        InterestManager.saveInterest(context, "")
        assertTrue(interests.isEmpty())
        verify(editor, org.mockito.Mockito.never()).putStringSet(anyString(), any())
    }

    @Test
    fun `hasSentInterest returns true for existing interest`() {
        val userId = "user123"
        val timestamp = System.currentTimeMillis()
        `when`(prefs.getStringSet(eq("sent_interests"), any())).thenReturn(setOf("$userId:$timestamp"))

        assertTrue(InterestManager.hasSentInterest(context, userId))
    }

    @Test
    fun `hasSentInterest returns false for non-existing interest`() {
        `when`(prefs.getStringSet(eq("sent_interests"), any())).thenReturn(emptySet())

        assertFalse(InterestManager.hasSentInterest(context, "user123"))
    }

    @Test
    fun `hasSentInterest returns false for blank userId`() {
        `when`(prefs.getStringSet(eq("sent_interests"), any())).thenReturn(setOf("user123:1234567890"))

        assertFalse(InterestManager.hasSentInterest(context, ""))
    }

    @Test
    fun `getSentInterests returns user IDs for valid interests`() {
        val userId1 = "user123"
        val userId2 = "user456"
        val timestamp = System.currentTimeMillis()
        `when`(prefs.getStringSet(eq("sent_interests"), any())).thenReturn(setOf("$userId1:$timestamp", "$userId2:$timestamp"))

        val result = InterestManager.getSentInterests(context)
        assertEquals(setOf(userId1, userId2), result)
    }

    @Test
    fun `getSentInterests removes expired interests`() {
        val userId = "user123"
        val expiredTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(8)
        val validTimestamp = System.currentTimeMillis()
        val interests = mutableSetOf("$userId:$expiredTimestamp", "user456:$validTimestamp")
        `when`(prefs.getStringSet(eq("sent_interests"), any())).thenReturn(interests)

        val result = InterestManager.getSentInterests(context)
        assertEquals(setOf("user456"), result)
        verify(editor).putStringSet(eq("sent_interests"), any())
    }

    @Test
    fun `getSentInterests handles invalid entries`() {
        val userId = "user123"
        val timestamp = System.currentTimeMillis()
        val interests = mutableSetOf("$userId:$timestamp", "invalid_entry")
        `when`(prefs.getStringSet(eq("sent_interests"), any())).thenReturn(interests)

        val result = InterestManager.getSentInterests(context)
        assertEquals(setOf(userId), result)
        verify(editor).putStringSet(eq("sent_interests"), any())
    }
}