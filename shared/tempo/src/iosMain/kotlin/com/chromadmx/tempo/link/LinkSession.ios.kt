package com.chromadmx.tempo.link

import abletonLink.ABLLinkCaptureAppSessionState
import abletonLink.ABLLinkCommitAppSessionState
import abletonLink.ABLLinkDelete
import abletonLink.ABLLinkGetBeatAtTime
import abletonLink.ABLLinkGetNumPeers
import abletonLink.ABLLinkGetTempo
import abletonLink.ABLLinkIsEnabled
import abletonLink.ABLLinkNew
import abletonLink.ABLLinkRef
import abletonLink.ABLLinkSetActive
import abletonLink.ABLLinkSetTempo
import platform.darwin.mach_absolute_time

/**
 * iOS actual for [LinkSession].
 *
 * Wraps Kotlin/Native cinterop calls to the Ableton LinkKit framework.
 * The cinterop bindings are generated from `ableton_link.def` which
 * references the LinkKit Objective-C headers (ABLLink.h).
 *
 * ## LinkKit C API
 *
 * LinkKit exposes a plain-C API (despite shipping as an ObjC framework):
 * - `ABLLinkNew(bpm)` / `ABLLinkDelete(ref)` — lifecycle
 * - `ABLLinkSetActive(ref, bool)` / `ABLLinkIsEnabled(ref)` — network
 * - `ABLLinkCaptureAppSessionState(ref)` — snapshot timeline state
 * - `ABLLinkCommitAppSessionState(ref, state)` — push changes to mesh
 * - `ABLLinkGetTempo(state)` / `ABLLinkSetTempo(state, bpm, hostTime)`
 * - `ABLLinkGetBeatAtTime(state, hostTime, quantum)` — beat position
 * - `ABLLinkGetNumPeers(ref)` — connected peer count
 *
 * Host time is provided by `mach_absolute_time()` (ticks, not nanoseconds;
 * LinkKit uses the same timebase internally on iOS).
 *
 * ## Threading
 *
 * All ABLLink functions are thread-safe. The capture/commit pattern provides
 * lock-free access suitable for audio-thread use.
 */
actual class LinkSession actual constructor() : LinkSessionApi {

    /** Opaque reference to the native Link session, created at 120 BPM. */
    private var ref: ABLLinkRef? = ABLLinkNew(DEFAULT_BPM)

    // ---- LinkSessionApi implementation ----

    actual override fun enable() {
        ref?.let { ABLLinkSetActive(it, true) }
    }

    actual override fun disable() {
        ref?.let { ABLLinkSetActive(it, false) }
    }

    actual override val isEnabled: Boolean
        get() = ref?.let { ABLLinkIsEnabled(it) } ?: false

    actual override val peerCount: Int
        get() = ref?.let { ABLLinkGetNumPeers(it).toInt() } ?: 0

    actual override val bpm: Double
        get() {
            val r = ref ?: return DEFAULT_BPM
            val state = ABLLinkCaptureAppSessionState(r) ?: return DEFAULT_BPM
            return ABLLinkGetTempo(state)
        }

    actual override val beatPhase: Double
        get() {
            val r = ref ?: return 0.0
            val state = ABLLinkCaptureAppSessionState(r) ?: return 0.0
            val hostTime = mach_absolute_time()
            val beats = ABLLinkGetBeatAtTime(state, hostTime, BEAT_QUANTUM)
            // Modulo to get fractional beat position [0.0, 1.0)
            return beats % BEAT_QUANTUM
        }

    actual override val barPhase: Double
        get() {
            val r = ref ?: return 0.0
            val state = ABLLinkCaptureAppSessionState(r) ?: return 0.0
            val hostTime = mach_absolute_time()
            val beats = ABLLinkGetBeatAtTime(state, hostTime, BAR_QUANTUM)
            // Position within 4-beat bar, normalized to [0.0, 1.0)
            return (beats % BAR_QUANTUM) / BAR_QUANTUM
        }

    actual override fun requestBpm(bpm: Double) {
        val r = ref ?: return
        val state = ABLLinkCaptureAppSessionState(r) ?: return
        val hostTime = mach_absolute_time()
        ABLLinkSetTempo(state, bpm, hostTime)
        ABLLinkCommitAppSessionState(r, state)
    }

    /**
     * Release native Link session resources.
     * After calling close, this instance must not be used.
     */
    actual override fun close() {
        ref?.let { r ->
            ABLLinkSetActive(r, false)
            ABLLinkDelete(r)
        }
        ref = null
    }

    private companion object {
        /** Default initial tempo. */
        const val DEFAULT_BPM = 120.0

        /** Quantum of 1 beat for beat-phase calculation. */
        const val BEAT_QUANTUM = 1.0

        /** Quantum of 4 beats for bar-phase calculation (4/4 time). */
        const val BAR_QUANTUM = 4.0
    }
}
