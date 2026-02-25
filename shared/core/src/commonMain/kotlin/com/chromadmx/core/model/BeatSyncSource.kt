package com.chromadmx.core.model

import kotlinx.serialization.Serializable

/**
 * Sources of musical timing synchronization.
 */
@Serializable
enum class BeatSyncSource {
    /** Synced via network (Ableton Link). */
    LINK,
    /** Manual tap tempo. */
    TAP,
    /** No active clock or internal free-run. */
    NONE
}
