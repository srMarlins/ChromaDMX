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

        val (r1, g1, b1) = when {
            hue < 60f  -> Triple(c, x, 0f)
            hue < 120f -> Triple(x, c, 0f)
            hue < 180f -> Triple(0f, c, x)
            hue < 240f -> Triple(0f, x, c)
            hue < 300f -> Triple(x, 0f, c)
            else       -> Triple(c, 0f, x)
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

        // Optimization: Inline lerp to avoid the redundant .coerceIn(0f, 1f) inside Color.lerp
        // since `frac` is already mathematically guaranteed to be in [0, 1) range here.
        val c1 = palette[lo]
        val c2 = palette[hi]
        return Color(
            c1.r + (c2.r - c1.r) * frac,
            c1.g + (c2.g - c1.g) * frac,
            c1.b + (c2.b - c1.b) * frac
        )
    }
}
