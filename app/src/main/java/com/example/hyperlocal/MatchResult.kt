package com.example.hyperlocal

import java.util.*

data class MatchResult(
    val matchPercentage: Int,
    val colorCode: String,
    val id: String = UUID.randomUUID().toString().take(8)
)