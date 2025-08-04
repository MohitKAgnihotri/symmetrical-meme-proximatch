package com.proxilocal.hyperlocal

import java.io.Serializable

// This data class will hold all the user's settings.
// It's Serializable to be easily saved to SharedPreferences.
data class UserProfile(
    val gender: Gender,
    val myCriteria: List<Boolean>,      // 64 characteristics
    val theirCriteria: List<Boolean>    // 64 characteristics
) : Serializable