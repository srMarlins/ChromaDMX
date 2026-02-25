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

        return palette[lo].lerp(palette[hi], frac)
    }

    /**
     * Convert a hex string (e.g., "#FF00FF" or "FF00FF") to a [Color].
     * Supports both 3-digit and 6-digit hex formats.
     */
    fun parseHex(hex: String): Color {
        return try {
            val s = hex.removePrefix("#")
            when (s.length) {
                6 -> Color(
                    (s.substring(0, 2).toInt(16) / 255f),
                    (s.substring(2, 4).toInt(16) / 255f),
                    (s.substring(4, 6).toInt(16) / 255f)
                )
                3 -> Color(
                    (s.substring(0, 1).repeat(2).toInt(16) / 255f),
                    (s.substring(1, 2).repeat(2).toInt(16) / 255f),
                    (s.substring(2, 3).repeat(2).toInt(16) / 255f)
                )
                else -> Color.WHITE
            }
        } catch (_: Exception) {
            Color.WHITE
        }
    }

    /**
     * Convert a [Color] to a 6-digit hex string (e.g., "#FF00FF").
     */
    fun toHex(color: Color): String {
        val r = (color.r * 255f + 0.5f).toInt().coerceIn(0, 255)
        val g = (color.g * 255f + 0.5f).toInt().coerceIn(0, 255)
        val b = (color.b * 255f + 0.5f).toInt().coerceIn(0, 255)
        return "#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}".uppercase()
    }
}
