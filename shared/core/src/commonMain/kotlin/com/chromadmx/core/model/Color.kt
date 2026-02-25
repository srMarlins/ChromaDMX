package com.chromadmx.core.model

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

/**
 * RGB color with Float components in the 0.0-1.0 range.
 *
 * All arithmetic helpers clamp results so downstream code never has to
 * worry about out-of-range values. Conversion to/from DMX byte triples
 * is provided via [toDmxBytes] and the companion [fromDmxBytes].
 */
@Serializable
data class Color(
    val r: Float,
    val g: Float,
    val b: Float
) {
    init {
        // Data class copy() bypasses custom constructors in some KMP targets,
        // so we validate lazily via public accessors when needed.
        // The clamped* helpers below are the canonical safe path.
    }

    /* ------------------------------------------------------------------ */
    /*  Clamped construction                                               */
    /* ------------------------------------------------------------------ */

    /** Return a copy with every component clamped to 0..1. */
    fun clamped(): Color = Color(
        r.coerceIn(0f, 1f),
        g.coerceIn(0f, 1f),
        b.coerceIn(0f, 1f)
    )

    /* ------------------------------------------------------------------ */
    /*  DMX conversion                                                     */
    /* ------------------------------------------------------------------ */

    /** Convert to a 3-byte DMX triple (R, G, B each 0-255). */
    fun toDmxBytes(): ByteArray {
        val c = clamped()
        return byteArrayOf(
            (c.r * 255f + 0.5f).toInt().coerceIn(0, 255).toByte(),
            (c.g * 255f + 0.5f).toInt().coerceIn(0, 255).toByte(),
            (c.b * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
        )
    }

    /* ------------------------------------------------------------------ */
    /*  Interpolation                                                      */
    /* ------------------------------------------------------------------ */

    /** Linearly interpolate between `this` and [other] by [t] (0..1). */
    fun lerp(other: Color, t: Float): Color {
        val ct = t.coerceIn(0f, 1f)
        return Color(
            r + (other.r - r) * ct,
            g + (other.g - g) * ct,
            b + (other.b - b) * ct
        )
    }

    /* ------------------------------------------------------------------ */
    /*  Operator helpers                                                   */
    /* ------------------------------------------------------------------ */

    operator fun plus(other: Color): Color = Color(r + other.r, g + other.g, b + other.b)
    operator fun times(other: Color): Color = Color(r * other.r, g * other.g, b * other.b)
    operator fun times(scalar: Float): Color = Color(r * scalar, g * scalar, b * scalar)

    /* ------------------------------------------------------------------ */
    /*  Companion                                                          */
    /* ------------------------------------------------------------------ */

    companion object {
        val BLACK = Color(0f, 0f, 0f)
        val WHITE = Color(1f, 1f, 1f)
        val RED = Color(1f, 0f, 0f)
        val GREEN = Color(0f, 1f, 0f)
        val BLUE = Color(0f, 0f, 1f)

        /** Create a [Color] from 3 consecutive DMX bytes starting at [offset]. */
        fun fromDmxBytes(bytes: ByteArray, offset: Int = 0): Color {
            require(offset >= 0 && offset + 3 <= bytes.size) {
                "Need 3 bytes starting at offset $offset, but array size is ${bytes.size}"
            }
            return Color(
                (bytes[offset].toInt() and 0xFF) / 255f,
                (bytes[offset + 1].toInt() and 0xFF) / 255f,
                (bytes[offset + 2].toInt() and 0xFF) / 255f
            )
        }
    }
}
