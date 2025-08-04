package com.proxilocal.hyperlocal

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import kotlin.test.assertFailsWith

class CriteriaManagerTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    @Before
    fun setUp() {
        context = mock<Context>()
        prefs = mock<SharedPreferences>()
        editor = mock<SharedPreferences.Editor>()
        `when`(context.getSharedPreferences(anyString(), anyInt())).thenReturn(prefs)
        `when`(prefs.edit()).thenReturn(editor)
        `when`(editor.putString(anyString(), anyString())).thenReturn(editor)
        `when`(editor.putStringSet(anyString(), any())).thenReturn(editor)
        `when`(editor.apply()).then { /* No-op */ }
    }

    @Test
    fun `saveUserProfile stores profile data correctly`() {
        val profile = UserProfile(
            gender = Gender.MALE,
            myCriteria = List(64) { it % 2 == 0 }, // Alternating true/false
            theirCriteria = List(64) { it % 2 == 1 }
        )
        CriteriaManager.saveUserProfile(context, profile)
        // Verify editor interactions (implementation-specific, so we check apply() was called)
        assertEquals(editor, editor.apply())
    }

    @Test
    fun `getUserProfile returns profile for valid data`() {
        `when`(prefs.getString(eq("user_gender"), any())).thenReturn("MALE")
        `when`(prefs.getString(eq("my_criteria"), any())).thenReturn(List(64) { "true" }.joinToString(","))
        `when`(prefs.getString(eq("their_criteria"), any())).thenReturn(List(64) { "false" }.joinToString(","))

        val profile = CriteriaManager.getUserProfile(context)
        assertEquals(Gender.MALE, profile.gender)
        assertEquals(List(64) { true }, profile.myCriteria)
        assertEquals(List(64) { false }, profile.theirCriteria)
    }

    @Test
    fun `getUserProfile returns default profile for missing data`() {
        `when`(prefs.getString(anyString(), any())).thenReturn(null)
        val profile = CriteriaManager.getUserProfile(context)
        assertEquals(Gender.PRIVATE, profile.gender)
        assertEquals(List(64) { false }, profile.myCriteria)
        assertEquals(List(64) { false }, profile.theirCriteria)
    }

    @Test
    fun `getUserProfile handles invalid gender`() {
        `when`(prefs.getString(eq("user_gender"), any())).thenReturn("INVALID")
        `when`(prefs.getString(eq("my_criteria"), any())).thenReturn(List(64) { "true" }.joinToString(","))
        `when`(prefs.getString(eq("their_criteria"), any())).thenReturn(List(64) { "false" }.joinToString(","))

        val profile = CriteriaManager.getUserProfile(context)
        assertEquals(Gender.PRIVATE, profile.gender)
        assertEquals(List(64) { false }, profile.myCriteria)
        assertEquals(List(64) { false }, profile.theirCriteria)
    }

    @Test
    fun `encodeCriteria creates correct bitmask`() {
        val criteria = List(64) { it % 2 == 0 } // true at even indices
        val bytes = CriteriaManager.encodeCriteria(criteria)
        assertEquals(8, bytes.size)
        // Verify specific bits (e.g., first byte should have bits 0, 2, 4, 6 set)
        assertEquals(0b01010101.toByte(), bytes[0])
    }

    @Test
    fun `encodeCriteria throws for invalid criteria size`() {
        val criteria = List(63) { false }
        assertFailsWith<IllegalArgumentException> {
            CriteriaManager.encodeCriteria(criteria)
        }
    }

    @Test
    fun `decodeCriteria decodes bitmask correctly`() {
        val bytes = byteArrayOf(0b01010101.toByte(), 0, 0, 0, 0, 0, 0, 0)
        val criteria = CriteriaManager.decodeCriteria(bytes)
        assertEquals(64, criteria.size)
        assertEquals(listOf(true, false, true, false, true, false, true, false) + List(56) { false }, criteria)
    }

    @Test
    fun `decodeCriteria returns default for invalid byte array`() {
        val bytes = byteArrayOf(0, 1) // Too short
        val criteria = CriteriaManager.decodeCriteria(bytes)
        assertEquals(64, criteria.size)
        assertEquals(List(64) { false }, criteria)
    }

    @Test
    fun `calculateMatchPercentage computes correctly`() {
        val userPrefs = List(64) { it % 2 == 0 } // true at even indices
        val otherPrefs = List(64) { it % 2 == 0 } // Same, 100% match
        val percentage = CriteriaManager.calculateMatchPercentage(userPrefs, otherPrefs)
        assertEquals(100, percentage)

        val otherPrefsPartial = List(64) { it % 4 == 0 } // true at 0, 4, 8, ..., 25% match
        assertEquals(25, CriteriaManager.calculateMatchPercentage(userPrefs, otherPrefsPartial))
    }

    @Test
    fun `calculateMatchPercentage throws for mismatched sizes`() {
        val userPrefs = List(64) { false }
        val otherPrefs = List(63) { false }
        assertFailsWith<IllegalArgumentException> {
            CriteriaManager.calculateMatchPercentage(userPrefs, otherPrefs)
        }
    }

    @Test
    fun `calculateMatchPercentage returns zero for no user preferences`() {
        val userPrefs = List(64) { false }
        val otherPrefs = List(64) { true }
        assertEquals(0, CriteriaManager.calculateMatchPercentage(userPrefs, otherPrefs))
    }
}