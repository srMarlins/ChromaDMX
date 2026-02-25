package com.chromadmx.tempo.tap

import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.BeatSyncSource
import com.chromadmx.tempo.clock.BeatClock
import com.chromadmx.tempo.clock.BeatClockUtils
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Tap-tempo clock: the user taps a button repeatedly to establish BPM,
 * and the clock maintains a continuously-advancing phase.
 *
 * ## Algorithm
 *
 * 1. Each tap records the current time (via [timeSource]).
 * 2. Inter-tap intervals are computed from the last [maxTapHistory] taps.
 * 3. Outlier intervals (> 2x or < 0.5x the median) are discarded.
 * 4. The median of remaining intervals yields the beat period.
 * 5. BPM = 60 / medianInterval, clamped to [BeatClockUtils.MIN_BPM]..[BeatClockUtils.MAX_BPM].
 *
 * ## Phase tracking
 *
 * Phase is calculated lazily from elapsed time since the "phase origin" —
 * the timestamp at which phase was last reset to zero. This avoids drift.
 * Each tap resets the phase origin so the downbeat aligns with the tap.
 *
 * ## Time source
 *
 * An injectable [timeSource] (returns nanoseconds) enables deterministic testing.
 * In production, the default uses [TimeSource.Monotonic] which is available
 * on all Kotlin Multiplatform targets.
 *
 * @param scope       Coroutine scope for the phase update loop.
 * @param timeSource  Returns the current time in nanoseconds. Injectable for testing.
 * @param updateIntervalMs  How often to refresh the phase StateFlows (default 16ms ~ 60fps).
 * @param maxTapHistory  Maximum number of taps to retain for BPM calculation.
 */
class TapTempoClock(
    private val scope: CoroutineScope,
    private val timeSource: () -> Long = defaultTimeSource,
    private val updateIntervalMs: Long = 16L,
    private val maxTapHistory: Int = 8
) : BeatClock {

    // ---- Internal state ----

    /** Timestamps (nanos) of recent taps. */
    private val tapTimestamps = mutableListOf<Long>()

    /**
     * The "phase origin" — the nanoTime at which phase is defined as 0.0.
     * Reset on each tap so the downbeat aligns with the user's input.
     */
    private var phaseOriginNanos: Long = 0L

    /** Timestamp when the clock was started (for elapsed calculation). */
    private var startTimeNanos: Long = 0L

    /** Accumulated elapsed time before the most recent start(). */
    private var accumulatedElapsedNanos: Long = 0L

    /** Manual phase offset in seconds, adjusted via [nudgePhase]. */
    private var phaseNudgeOffsetSec: Double = 0.0

    /** The update loop job. */
    private var updateJob: Job? = null

    // ---- StateFlows (exposed via BeatClock) ----

    private val _bpm = MutableStateFlow(BeatClockUtils.DEFAULT_BPM)
    override val bpm: StateFlow<Float> = _bpm.asStateFlow()

    private val _beatPhase = MutableStateFlow(0f)
    override val beatPhase: StateFlow<Float> = _beatPhase.asStateFlow()

    private val _barPhase = MutableStateFlow(0f)
    override val barPhase: StateFlow<Float> = _barPhase.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _syncSource = MutableStateFlow(BeatSyncSource.NONE)
    override val syncSource: StateFlow<BeatSyncSource> = _syncSource.asStateFlow()

    private val _beatState = MutableStateFlow(BeatState.IDLE)
    override val beatState: StateFlow<BeatState> = _beatState.asStateFlow()

    // ---- BeatClock lifecycle ----

    override fun start() {
        if (_isRunning.value) return
        _isRunning.value = true
        startTimeNanos = timeSource()
        phaseOriginNanos = startTimeNanos
        startUpdateLoop()
    }

    override fun stop() {
        if (!_isRunning.value) return
        // Accumulate elapsed time so far
        accumulatedElapsedNanos += timeSource() - startTimeNanos
        _isRunning.value = false
        updateJob?.cancel()
        updateJob = null
    }

    // ---- Tap tempo API ----

    /**
     * Register a tap. Call this each time the user presses the tap button.
     *
     * Automatically starts the clock on the first tap if not already running.
     * Resets tap history if the interval since the last tap exceeds [tapTimeoutNanos].
     */
    fun tap() {
        val now = timeSource()

        // If not running, start on first tap
        if (!_isRunning.value) {
            _isRunning.value = true
            startTimeNanos = now
            startUpdateLoop()
        }

        // Reset history if gap too long (3 seconds)
        if (tapTimestamps.isNotEmpty()) {
            val gap = now - tapTimestamps.last()
            if (gap > TAP_TIMEOUT_NANOS) {
                tapTimestamps.clear()
            }
        }

        tapTimestamps.add(now)
        _syncSource.value = BeatSyncSource.TAP

        // Trim to maxTapHistory
        while (tapTimestamps.size > maxTapHistory) {
            tapTimestamps.removeAt(0)
        }

        // Recalculate BPM from intervals
        if (tapTimestamps.size >= 2) {
            val newBpm = calculateBpmFromTaps()
            if (newBpm != null) {
                _bpm.value = BeatClockUtils.clampBpm(newBpm)
            }
        }

        // Reset phase origin to this tap (downbeat aligns with tap)
        phaseOriginNanos = now
        phaseNudgeOffsetSec = 0.0

        // Immediately update phase to 0 (downbeat)
        updatePhase()
    }

    /**
     * Nudge the phase by a small offset (in seconds).
     * Positive values push the phase forward; negative values pull it back.
     * Useful for manual fine-tuning of sync.
     */
    fun nudgePhase(offsetSec: Double) {
        phaseNudgeOffsetSec += offsetSec
        updatePhase()
    }

    /**
     * Reset the clock: clear taps, reset BPM to default, stop.
     */
    fun reset() {
        stop()
        tapTimestamps.clear()
        phaseOriginNanos = 0L
        accumulatedElapsedNanos = 0L
        phaseNudgeOffsetSec = 0.0
        _syncSource.value = BeatSyncSource.NONE
        _bpm.value = BeatClockUtils.DEFAULT_BPM
        _beatPhase.value = 0f
        _barPhase.value = 0f
        _beatState.value = BeatState.IDLE
    }

    // ---- Internal ----

    /**
     * Calculate BPM from recorded tap timestamps using median filtering.
     *
     * Steps:
     * 1. Compute all inter-tap intervals.
     * 2. Calculate the median interval.
     * 3. Discard outliers (intervals more than 2x or less than 0.5x the median).
     * 4. Recompute the median of the filtered set.
     * 5. Convert median interval to BPM.
     */
    internal fun calculateBpmFromTaps(): Float? {
        if (tapTimestamps.size < 2) return null

        // Compute inter-tap intervals in seconds
        val intervals = mutableListOf<Double>()
        for (i in 1 until tapTimestamps.size) {
            val intervalSec = (tapTimestamps[i] - tapTimestamps[i - 1]).toDouble() / NANOS_PER_SEC
            intervals.add(intervalSec)
        }

        if (intervals.isEmpty()) return null

        // First pass: median for outlier detection
        val rawMedian = BeatClockUtils.median(intervals)
        if (rawMedian <= 0.0) return null

        // Filter outliers: keep intervals within 0.5x to 2.0x of the median
        val filtered = intervals.filter { it >= rawMedian * 0.5 && it <= rawMedian * 2.0 }

        if (filtered.isEmpty()) return null

        // Final median of filtered intervals
        val finalMedian = BeatClockUtils.median(filtered)
        if (finalMedian <= 0.0) return null

        return (60.0 / finalMedian).toFloat()
    }

    private fun startUpdateLoop() {
        updateJob?.cancel()
        updateJob = scope.launch {
            while (isActive) {
                updatePhase()
                delay(updateIntervalMs)
            }
        }
    }

    /**
     * Recompute phase values from the current time. This is the core
     * time-based (not accumulation-based) phase calculation.
     */
    internal fun updatePhase() {
        val now = timeSource()
        val currentBpm = _bpm.value

        // Update sync source if tap timeout reached
        if (tapTimestamps.isNotEmpty()) {
            val gap = now - tapTimestamps.last()
            if (gap > TAP_TIMEOUT_NANOS) {
                _syncSource.value = BeatSyncSource.NONE
            }
        }

        // Elapsed since phase origin, plus any nudge offset
        val elapsedSinceOriginSec =
            (now - phaseOriginNanos).toDouble() / NANOS_PER_SEC + phaseNudgeOffsetSec

        // Use the absolute elapsed since origin (may be negative briefly after nudge)
        val effectiveElapsed = if (elapsedSinceOriginSec < 0.0) 0.0 else elapsedSinceOriginSec

        val newBeatPhase = BeatClockUtils.computeBeatPhase(effectiveElapsed, currentBpm)
        val newBarPhase = BeatClockUtils.computeBarPhase(effectiveElapsed, currentBpm)

        // Total elapsed since the clock was started
        val totalElapsedSec = if (_isRunning.value) {
            (accumulatedElapsedNanos + (now - startTimeNanos)).toDouble() / NANOS_PER_SEC
        } else {
            accumulatedElapsedNanos.toDouble() / NANOS_PER_SEC
        }

        _beatPhase.value = newBeatPhase
        _barPhase.value = newBarPhase
        _beatState.value = BeatState(
            bpm = currentBpm,
            beatPhase = newBeatPhase,
            barPhase = newBarPhase,
            elapsed = totalElapsedSec.toFloat(),
            syncSource = _syncSource.value
        )
    }

    companion object {
        /** 3-second timeout: if no tap within this window, reset tap history. */
        internal const val TAP_TIMEOUT_NANOS: Long = 3_000_000_000L

        internal const val NANOS_PER_SEC: Double = 1_000_000_000.0

        /**
         * Default time source using Kotlin's [TimeSource.Monotonic], which
         * is available across all KMP targets (JVM, iOS native, JS).
         * Returns elapsed nanoseconds since an arbitrary fixed epoch.
         */
        private val monotonicStart = TimeSource.Monotonic.markNow()
        private val defaultTimeSource: () -> Long = {
            monotonicStart.elapsedNow().inWholeNanoseconds
        }
    }
}
