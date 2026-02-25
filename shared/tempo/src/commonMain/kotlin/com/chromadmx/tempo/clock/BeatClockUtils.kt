package com.chromadmx.tempo.clock

/**
 * Pure math helpers for beat/bar phase calculation.
 *
 * All phase calculations are time-based (computed from elapsed time and BPM)
 * rather than accumulation-based, to prevent drift over long running times.
 */
object BeatClockUtils {

    /** Minimum supported BPM. */
    const val MIN_BPM: Float = 20f

    /** Maximum supported BPM. */
    const val MAX_BPM: Float = 300f

    /** Default BPM when no tempo has been established. */
    const val DEFAULT_BPM: Float = 120f

    /** Number of beats per bar (4/4 time). */
    const val BEATS_PER_BAR: Int = 4

    /**
     * Duration of one beat in seconds at the given [bpm].
     */
    fun beatDurationSec(bpm: Float): Double {
        require(bpm > 0f) { "BPM must be positive, got $bpm" }
        return 60.0 / bpm.toDouble()
    }

    /**
     * Duration of one bar (4 beats) in seconds at the given [bpm].
     */
    fun barDurationSec(bpm: Float): Double = beatDurationSec(bpm) * BEATS_PER_BAR

    /**
     * Compute beat phase (0.0..1.0) from elapsed time in seconds and BPM.
     *
     * Phase is calculated as the fractional part of (elapsed / beatDuration),
     * ensuring no drift regardless of how long the clock has been running.
     */
    fun computeBeatPhase(elapsedSec: Double, bpm: Float): Float {
        if (bpm <= 0f) return 0f
        val beatDuration = beatDurationSec(bpm)
        val totalBeats = elapsedSec / beatDuration
        return (totalBeats % 1.0).toFloat()
    }

    /**
     * Compute bar phase (0.0..1.0) from elapsed time in seconds and BPM.
     *
     * Bar phase covers 4 beats: one full bar cycle in 4/4 time.
     */
    fun computeBarPhase(elapsedSec: Double, bpm: Float): Float {
        if (bpm <= 0f) return 0f
        val barDuration = barDurationSec(bpm)
        val totalBars = elapsedSec / barDuration
        return (totalBars % 1.0).toFloat()
    }

    /**
     * Clamp a BPM value to the supported range [MIN_BPM]..[MAX_BPM].
     */
    fun clampBpm(bpm: Float): Float = bpm.coerceIn(MIN_BPM, MAX_BPM)

    /**
     * Compute the median of a list of values. Used for stable BPM
     * estimation from tap intervals, as median is robust to outliers.
     *
     * @return The median value, or 0.0 if the list is empty.
     */
    fun median(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[mid - 1] + sorted[mid]) / 2.0
        } else {
            sorted[mid]
        }
    }
}
