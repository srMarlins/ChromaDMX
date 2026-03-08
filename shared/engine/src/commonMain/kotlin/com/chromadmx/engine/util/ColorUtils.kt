package com.chromadmx.engine.util

import com.chromadmx.core.model.Color
import kotlin.math.abs

/**
 * Color conversion utilities — pure Kotlin, no platform dependencies.
 */
object ColorUtils {

    /**
     * Convert HSV to RGB.
     *
     * @param h Hue in degrees (0-360, wraps).
     * @param s Saturation (0-1).
     * @param v Value / brightness (0-1).
     * @return An RGB [Color] with components in 0-1.
     */
    fun hsvToRgb(h: Float, s: Float, v: Float): Color {
        val hue = ((h % 360f) + 360f) % 360f  // normalize to 0..360
        val c = v * s
        val x = c * (1f - abs((hue / 60f) % 2f - 1f))
        val m = v - c

        // Optimization: avoid allocating Triple objects in this frequently-called
        // render path function by assigning to local primitive variables directly
        val r1: Float
        val g1: Float
        val b1: Float

        when {
            hue < 60f  -> { r1 = c; g1 = x; b1 = 0f }
            hue < 120f -> { r1 = x; g1 = c; b1 = 0f }
            hue < 180f -> { r1 = 0f; g1 = c; b1 = x }
            hue < 240f -> { r1 = 0f; g1 = x; b1 = c }
            hue < 300f -> { r1 = x; g1 = 0f; b1 = c }
            else       -> { r1 = c; g1 = 0f; b1 = x }
        }

        return Color(r1 + m, g1 + m, b1 + m)
    }

    /**
     * Sample a color palette at a normalized position [t] (0-1).
     *
     * Linearly interpolates between adjacent palette entries.
     * If the palette is empty, returns [Color.BLACK].
     * If the palette has one entry, returns that entry.
     */
    fun samplePalette(palette: List<Color>, t: Float): Color {
        if (palette.isEmpty()) return Color.BLACK
        if (palette.size == 1) return palette[0]

        val clamped = t.coerceIn(0f, 1f)
        val maxIdx = palette.size - 1
        val scaled = clamped * maxIdx
        val lo = scaled.toInt().coerceIn(0, maxIdx - 1)
        val hi = (lo + 1).coerceAtMost(maxIdx)
        val frac = scaled - lo

        return palette[lo].lerp(palette[hi], frac)
    }
}
