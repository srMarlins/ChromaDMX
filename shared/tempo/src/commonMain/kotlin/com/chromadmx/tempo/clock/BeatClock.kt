package com.chromadmx.tempo.clock

import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.BeatSyncSource
import kotlinx.coroutines.flow.StateFlow

/**
 * Provides real-time musical timing information as continuous phase values.
 *
 * Implementations include:
 * - [com.chromadmx.tempo.tap.TapTempoClock] — manual tap-tempo with phase tracking
 * - [com.chromadmx.tempo.link.AbletonLinkClock] — network-synced via Ableton Link
 *
 * Consumers (typically the effect engine) observe [beatState] each frame to
 * drive time-based lighting effects.
 */
interface BeatClock {

    /** Current beats-per-minute. */
    val bpm: StateFlow<Float>

    /**
     * Phase within the current beat, cycling 0.0 (downbeat) to 1.0 (just
     * before the next downbeat). Advances continuously while [isRunning].
     */
    val beatPhase: StateFlow<Float>

    /**
     * Phase within the current bar (4 beats), cycling 0.0 to 1.0.
     * One full cycle = 4 beat cycles.
     */
    val barPhase: StateFlow<Float>

    /** Whether the clock is actively advancing phase. */
    val isRunning: StateFlow<Boolean>

    /** Current source of synchronization. */
    val syncSource: StateFlow<BeatSyncSource>

    /**
     * Composite snapshot of the current timing state, suitable for
     * passing into the effect engine each frame.
     */
    val beatState: StateFlow<BeatState>

    /** Start phase advancement. */
    fun start()

    /** Stop phase advancement (phase freezes at its current value). */
    fun stop()
}
