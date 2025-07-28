package com.example.hyperlocal

// Enum to represent gender identity in a type-safe way
enum class Gender {
    MALE,
    FEMALE,
    LGBTQ_PLUS,
    PRIVATE
}

// The updated data class holding all necessary info for a match
data class MatchResult(
    val id: String,
    val matchPercentage: Int, // A value from 0 to 100
    val distanceRssi: Int,    // A signal strength value (e.g., -30 to -90)
    val gender: Gender
)