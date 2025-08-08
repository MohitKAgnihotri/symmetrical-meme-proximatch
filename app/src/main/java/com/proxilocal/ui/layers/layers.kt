package com.proxilocal.ui.layers

import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex

/** Canonical draw order so we never play zIndex whack‑a‑mole again. */
enum class UiLayer(val z: Float) {
    Map(0f),        // Background (Mapbox etc.)
    Content(1f),    // Lists, feed, body content
    Chrome(2f),     // App bars, FABs, nav
    Radar(9f),      // Interactive radar & tap catcher
    Overlays(10f)   // Dialogs/sheets/scrims
}

fun Modifier.layer(layer: UiLayer) = this.zIndex(layer.z)
