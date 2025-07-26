package com.example.hyperlocal

import java.util.*

data class MatchResult(
    val matchPercentage: Int,
    val colorCode: String,
    val id: String = UUID.randomUUID().toString().take(8),
    val rssi: Int? = null // Optional RSSI signal strength for radar positioning
)