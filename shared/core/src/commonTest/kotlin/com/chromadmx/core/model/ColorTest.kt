package com.chromadmx.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ColorTest {

    /* ---- clamped ---- */

    @Test
    fun clampedCoercesOutOfRangeValues() {
        val c = Color(-0.5f, 1.5f, 0.5f).clamped()
        assertEquals(0f, c.r)
        assertEquals(1f, c.g)
        assertEquals(0.5f, c.b)
    }

    @Test
    fun clampedLeavesValidValuesUnchanged() {
        val c = Color(0.2f, 0.4f, 0.6f).clamped()
        assertEquals(0.2f, c.r)
        assertEquals(0.4f, c.g)
        assertEquals(0.6f, c.b)
    }

    /* ---- DMX conversion round-trip ---- */

    @Test
    fun toDmxBytesAndBack() {
        val original = Color(1f, 0.5f, 0f)
        val bytes = original.toDmxBytes()
        assertEquals(3, bytes.size)

        // 1.0 -> 255, 0.5 -> 128, 0.0 -> 0
        assertEquals(255.toByte(), bytes[0])
        assertEquals(128.toByte(), bytes[1])
        assertEquals(0.toByte(), bytes[2])

        val restored = Color.fromDmxBytes(bytes)
        assertEquals(1f, restored.r)
        assertApprox(0.502f, restored.g, 0.005f) // 128/255 ≈ 0.502
        assertEquals(0f, restored.b)
    }

    @Test
    fun toDmxBytesClampsOutOfRange() {
        val bytes = Color(2f, -1f, 0.5f).toDmxBytes()
        assertEquals(255.toByte(), bytes[0])
        assertEquals(0.toByte(), bytes[1])
    }

    @Test
    fun fromDmxBytesWithOffset() {
        val bytes = byteArrayOf(0, 0, 255.toByte(), 128.toByte(), 0)
        val c = Color.fromDmxBytes(bytes, offset = 2)
        assertEquals(1f, c.r)
        assertApprox(0.502f, c.g, 0.005f)
        assertEquals(0f, c.b)
    }

    @Test
    fun fromDmxBytesRejectsInvalidOffset() {
        assertFailsWith<IllegalArgumentException> {
            Color.fromDmxBytes(byteArrayOf(0, 0), offset = 0)
        }
    }

    /* ---- lerp ---- */

    @Test
    fun lerpAtZeroReturnsSelf() {
        val a = Color(0f, 0f, 0f)
        val b = Color(1f, 1f, 1f)
        val result = a.lerp(b, 0f)
        assertEquals(a, result)
    }

    @Test
    fun lerpAtOneReturnsOther() {
        val a = Color(0f, 0f, 0f)
        val b = Color(1f, 1f, 1f)
        val result = a.lerp(b, 1f)
        assertEquals(b, result)
    }

    @Test
    fun lerpAtHalfReturnsMidpoint() {
        val a = Color(0f, 0f, 0f)
        val b = Color(1f, 1f, 1f)
        val result = a.lerp(b, 0.5f)
        assertApprox(0.5f, result.r)
        assertApprox(0.5f, result.g)
        assertApprox(0.5f, result.b)
    }

    @Test
    fun lerpClampsT() {
        val a = Color.BLACK
        val b = Color.WHITE
        assertEquals(Color.BLACK, a.lerp(b, -1f))
        assertEquals(Color.WHITE, a.lerp(b, 2f))
    }

    /* ---- operators ---- */

    @Test
    fun plusAddsComponents() {
        val c = Color(0.2f, 0.3f, 0.4f) + Color(0.1f, 0.2f, 0.3f)
        assertApprox(0.3f, c.r)
        assertApprox(0.5f, c.g)
        assertApprox(0.7f, c.b)
    }

    @Test
    fun timesMultipliesComponents() {
        val c = Color(0.5f, 0.5f, 1f) * Color(0.5f, 1f, 0.5f)
        assertApprox(0.25f, c.r)
        assertApprox(0.5f, c.g)
        assertApprox(0.5f, c.b)
    }

    @Test
    fun timesScalar() {
        val c = Color(0.5f, 0.5f, 0.5f) * 2f
        assertApprox(1f, c.r)
        assertApprox(1f, c.g)
        assertApprox(1f, c.b)
    }

    /* ---- companion constants ---- */

    @Test
    fun companionConstants() {
        assertEquals(Color(0f, 0f, 0f), Color.BLACK)
        assertEquals(Color(1f, 1f, 1f), Color.WHITE)
        assertEquals(Color(1f, 0f, 0f), Color.RED)
        assertEquals(Color(0f, 1f, 0f), Color.GREEN)
        assertEquals(Color(0f, 0f, 1f), Color.BLUE)
    }

    /* ---- helpers ---- */

    private fun assertApprox(expected: Float, actual: Float, eps: Float = 1e-4f) {
        assertTrue(
            kotlin.math.abs(expected - actual) < eps,
            "Expected ~$expected but got $actual"
        )
    }
}
