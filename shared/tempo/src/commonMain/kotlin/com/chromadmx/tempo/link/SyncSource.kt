package com.chromadmx.tempo.link

/**
 * Identifies which timing source is currently active in [LinkFallbackClock].
 */
enum class SyncSource {
    /** Synchronized via Ableton Link — peers are connected. */
    LINK,

    /** Tap tempo — manual BPM from user taps (Link unavailable or no peers). */
    TAP,

    /** No source active (both clocks stopped). */
    NONE
}
