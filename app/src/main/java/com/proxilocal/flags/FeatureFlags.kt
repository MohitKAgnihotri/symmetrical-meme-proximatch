package com.proxilocal.flags

/**
 * Global feature flags.
 * Phase 0: kill-switch for the interactive radar.
 *
 * HOW TO USE (Phase 0):
 *  - Leave enableInteractiveRadar = false to keep current behavior.
 *  - In Phase 1+, we’ll thread this into the ViewModel/UI.
 */
object FeatureFlags {
    // Flip to true only when you want to start testing interactions.
    @Volatile
    var enableInteractiveRadar: Boolean = true
}
