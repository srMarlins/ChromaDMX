package com.chromadmx.tempo.link

/**
 * Common interface for Ableton Link session operations.
 *
 * This interface exists so that common code (including tests) can depend on
 * Link session behavior without referencing the `expect/actual` [LinkSession]
 * class directly. Test fakes implement this interface.
 *
 * Production code uses [LinkSession] (which implements this interface) via
 * platform-specific `actual` classes.
 */
interface LinkSessionApi {

    /** Enable this Link session, joining the network mesh. */
    fun enable()

    /** Disable this Link session, leaving the network mesh. */
    fun disable()

    /** Whether this session is currently enabled. */
    val isEnabled: Boolean

    /** Number of connected Link peers. */
    val peerCount: Int

    /** Current tempo in BPM from the Link timeline. */
    val bpm: Double

    /** Phase within the current beat, 0.0 to just below 1.0. */
    val beatPhase: Double

    /** Phase within the current bar (4-beat quantum), 0.0 to just below 1.0. */
    val barPhase: Double

    /** Request a tempo change propagated to all peers. */
    fun requestBpm(bpm: Double)
}
