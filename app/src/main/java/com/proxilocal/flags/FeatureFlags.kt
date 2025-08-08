package com.proxilocal.flags

/**
 * Global feature flags.
 * Phase 0: kill-switch for the interactive radar.
 * Phase 5: gate mutual-transition behavior independently.
 *
 * HOW TO USE:
 *  - enableInteractiveRadar controls tap/ping interactions on the radar.
 *  - enableMutualTransition gates auto-flipping to MUTUAL when both sides like each other.
 */
object FeatureFlags {
    // Flip to true only when you want to start testing interactions.
    @Volatile
    var enableInteractiveRadar: Boolean = true

    // NEW: When off, inbound LIKEs won’t flip status to MUTUAL.
    @Volatile
    var enableMutualTransition: Boolean = true
}
