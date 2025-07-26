package com.example.hyperlocal

import androidx.compose.ui.graphics.Color
import android.graphics.Color as AndroidColor


object ColorUtils {

    fun generatePinkShade(intensity: Float, distanceFactor: Float): Color {
        // Pink base: hue ~340, saturation high, value based on intensity/distance
        val hsv = floatArrayOf(340f, 0.7f + 0.3f * distanceFactor, intensity * distanceFactor)
        val argb = AndroidColor.HSVToColor(hsv)
        return Color(argb)

    }

    fun generateBlueShade(intensity: Float, distanceFactor: Float): Color {
        // Blue base: hue ~220, saturation high, value based on intensity/distance
        val hsv = floatArrayOf(220f, 0.7f + 0.3f * distanceFactor, intensity * distanceFactor)
        val argb = AndroidColor.HSVToColor(hsv)
        return Color(argb)

    }
}
