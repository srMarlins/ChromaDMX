package com.chromadmx.core.model

import kotlinx.serialization.Serializable
import kotlin.math.sqrt

/**
 * Immutable 3-D vector used for fixture positions and spatial effect math.
 *
 * All operators return new instances; [Vec3] is never mutated.
 */
@Serializable
data class Vec3(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
) {
    /* ------------------------------------------------------------------ */
    /*  Arithmetic operators                                               */
    /* ------------------------------------------------------------------ */

    operator fun plus(other: Vec3): Vec3 = Vec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3): Vec3 = Vec3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float): Vec3 = Vec3(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Float): Vec3 {
        require(scalar != 0f) { "Cannot divide Vec3 by zero" }
        return Vec3(x / scalar, y / scalar, z / scalar)
    }

    operator fun unaryMinus(): Vec3 = Vec3(-x, -y, -z)

    /* ------------------------------------------------------------------ */
    /*  Vector products                                                    */
    /* ------------------------------------------------------------------ */

    /** Dot (scalar) product. */
    infix fun dot(other: Vec3): Float = x * other.x + y * other.y + z * other.z

    /** Cross (vector) product. */
    infix fun cross(other: Vec3): Vec3 = Vec3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    /* ------------------------------------------------------------------ */
    /*  Length & distance                                                   */
    /* ------------------------------------------------------------------ */

    /** Squared magnitude (avoids sqrt when only comparing distances). */
    fun magnitudeSquared(): Float = x * x + y * y + z * z

    /** Euclidean magnitude / length. */
    fun magnitude(): Float = sqrt(magnitudeSquared())

    /** Unit vector in the same direction, or [ZERO] if magnitude is ~0. */
    fun normalized(): Vec3 {
        val mag = magnitude()
        return if (mag < 1e-7f) ZERO else this / mag
    }

    /** Euclidean distance to [other]. */
    fun distanceTo(other: Vec3): Float = (this - other).magnitude()

    /* ------------------------------------------------------------------ */
    /*  Interpolation                                                      */
    /* ------------------------------------------------------------------ */

    /** Linearly interpolate between `this` and [other] by [t] (unclamped). */
    fun lerp(other: Vec3, t: Float): Vec3 = Vec3(
        x + (other.x - x) * t,
        y + (other.y - y) * t,
        z + (other.z - z) * t
    )

    /* ------------------------------------------------------------------ */
    /*  Companion                                                          */
    /* ------------------------------------------------------------------ */

    companion object {
        val ZERO = Vec3(0f, 0f, 0f)
        val ONE = Vec3(1f, 1f, 1f)
        val UP = Vec3(0f, 1f, 0f)
        val RIGHT = Vec3(1f, 0f, 0f)
        val FORWARD = Vec3(0f, 0f, 1f)
    }
}
