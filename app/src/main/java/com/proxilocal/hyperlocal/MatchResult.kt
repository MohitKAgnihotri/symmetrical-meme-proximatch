package com.proxilocal.hyperlocal

// Enum to represent gender identity in a type-safe way
enum class Gender {
    MALE,
    FEMALE,
    LGBTQ_PLUS,
    PRIVATE
}

// The existing data class holding all necessary info for a match
data class MatchResult(
    val id: String,
    val matchPercentage: Int, // A value from 0 to 100
    val distanceRssi: Int,    // A signal strength value (e.g., -30 to -90)
    val gender: Gender,
    val lastSeen: Long = System.currentTimeMillis()
)

/* ──────────────────────────────────────────────────────────────
   Phase 1: Domain types for interaction (inert — no UI changes)
   These are used by later phases (gestures, BLE intents, rings).
   Keeping them in this file avoids import sprawl.
   ────────────────────────────────────────────────────────────── */

/** The type of sentiment we can send to another user. */
enum class LikeType { LIKE, SUPER_LIKE }

/** UI/connection lifecycle for a dot/person on the radar. */
enum class MatchStatus {
    NONE,          // no action taken
    LIKED,         // we sent a like
    SUPER_LIKED,   // we sent a super like
    MUTUAL,        // both sides liked (or super liked)
    CONNECTED      // secure connection established
}

/**
 * Optional per-dot UI wrapper. Keeps the raw MatchResult intact while
 * carrying local interaction state. You can adopt this later without
 * touching existing screens.
 */
data class MatchUiState(
    val match: MatchResult,
    val status: MatchStatus = MatchStatus.NONE,
    val likeType: LikeType? = null
)
