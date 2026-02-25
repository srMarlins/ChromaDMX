package com.chromadmx.core.util

/**
 * Pure-Kotlin math helpers shared across all modules.
 */
object MathUtils {

    /** Two-pi constant. */
    const val TAU: Float = 6.2831855f // 2 * PI

    /** Linearly interpolate between [a] and [b] by [t]. */
    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    /** Inverse lerp: returns the `t` value such that `lerp(a, b, t) == value`. */
    fun inverseLerp(a: Float, b: Float, value: Float): Float {
        val denom = b - a
        return if (denom == 0f) 0f else (value - a) / denom
    }

    /** Remap [value] from range [inMin]..[inMax] to [outMin]..[outMax]. */
    fun remap(value: Float, inMin: Float, inMax: Float, outMin: Float, outMax: Float): Float {
        val t = inverseLerp(inMin, inMax, value)
        return lerp(outMin, outMax, t)
    }

    /** Clamp [value] to [min]..[max]. */
    fun clamp(value: Float, min: Float = 0f, max: Float = 1f): Float =
        value.coerceIn(min, max)

    /** Smooth-step (Hermite interpolation) for t in 0..1. */
    fun smoothStep(t: Float): Float {
        val ct = t.coerceIn(0f, 1f)
        return ct * ct * (3f - 2f * ct)
    }

    /**
     * Wrap [value] into the half-open range 0 ..< [max].
     * Useful for cyclic phase values (e.g. beat phase, hue angle).
     */
    fun wrap(value: Float, max: Float): Float {
        if (max == 0f) return 0f
        val m = value % max
        return if (m < 0f) m + max else m
    }
}
