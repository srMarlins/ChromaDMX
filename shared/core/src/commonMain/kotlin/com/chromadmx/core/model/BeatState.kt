package com.chromadmx.core.model

import kotlinx.serialization.Serializable

/**
 * Snapshot of the current musical timing state, produced by the
 * tempo module and consumed by the effect engine each frame.
 *
 * @property bpm        Current beats-per-minute.
 * @property beatPhase  Phase within the current beat, 0.0 (downbeat) to 1.0.
 * @property barPhase   Phase within the current bar (4 beats), 0.0 to 1.0.
 * @property elapsed    Seconds since the clock was started.
 * @property syncSource Current source of synchronization.
 */
@Serializable
data class BeatState(
    val bpm: Float,
    val beatPhase: Float,
    val barPhase: Float,
    val elapsed: Float,
    val syncSource: BeatSyncSource = BeatSyncSource.NONE
) {
    companion object {
        /** Idle / default state (120 BPM, no phase). */
        val IDLE = BeatState(
            bpm = 120f,
            beatPhase = 0f,
            barPhase = 0f,
            elapsed = 0f,
            syncSource = BeatSyncSource.NONE
        )
    }
}
