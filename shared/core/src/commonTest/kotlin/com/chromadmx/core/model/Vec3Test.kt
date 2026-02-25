package com.chromadmx.core.model

import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class Vec3Test {

    /* ---- arithmetic operators ---- */

    @Test
    fun plus() {
        val r = Vec3(1f, 2f, 3f) + Vec3(4f, 5f, 6f)
        assertEquals(Vec3(5f, 7f, 9f), r)
    }

    @Test
    fun minus() {
        val r = Vec3(5f, 7f, 9f) - Vec3(4f, 5f, 6f)
        assertEquals(Vec3(1f, 2f, 3f), r)
    }

    @Test
    fun timesScalar() {
        val r = Vec3(1f, 2f, 3f) * 2f
        assertEquals(Vec3(2f, 4f, 6f), r)
    }

    @Test
    fun divScalar() {
        val r = Vec3(2f, 4f, 6f) / 2f
        assertEquals(Vec3(1f, 2f, 3f), r)
    }

    @Test
    fun divByZeroThrows() {
        assertFailsWith<IllegalArgumentException> {
            Vec3(1f, 2f, 3f) / 0f
        }
    }

    @Test
    fun unaryMinus() {
        assertEquals(Vec3(-1f, -2f, -3f), -Vec3(1f, 2f, 3f))
    }

    /* ---- dot product ---- */

    @Test
    fun dotProductPerpendicularIsZero() {
        val r = Vec3.RIGHT dot Vec3.UP
        assertApprox(0f, r)
    }

    @Test
    fun dotProductParallel() {
        val r = Vec3(1f, 0f, 0f) dot Vec3(3f, 0f, 0f)
        assertApprox(3f, r)
    }

    @Test
    fun dotProductGeneral() {
        // (1,2,3) . (4,5,6) = 4+10+18 = 32
        val r = Vec3(1f, 2f, 3f) dot Vec3(4f, 5f, 6f)
        assertApprox(32f, r)
    }

    /* ---- cross product ---- */

    @Test
    fun crossProductBasisVectors() {
        // i x j = k
        val r = Vec3.RIGHT cross Vec3.UP
        assertEquals(Vec3.FORWARD, r)
    }

    @Test
    fun crossProductAnticommutative() {
        val a = Vec3(1f, 2f, 3f)
        val b = Vec3(4f, 5f, 6f)
        val ab = a cross b
        val ba = b cross a
        assertApprox(-ab.x, ba.x)
        assertApprox(-ab.y, ba.y)
        assertApprox(-ab.z, ba.z)
    }

    @Test
    fun crossProductSelfIsZero() {
        val v = Vec3(3f, 7f, 2f)
        val r = v cross v
        assertApprox(0f, r.x)
        assertApprox(0f, r.y)
        assertApprox(0f, r.z)
    }

    /* ---- magnitude & distance ---- */

    @Test
    fun magnitudeOfUnitVector() {
        assertApprox(1f, Vec3.RIGHT.magnitude())
    }

    @Test
    fun magnitudeGeneral() {
        // |<3,4,0>| = 5
        assertApprox(5f, Vec3(3f, 4f, 0f).magnitude())
    }

    @Test
    fun magnitudeSquared() {
        assertApprox(25f, Vec3(3f, 4f, 0f).magnitudeSquared())
    }

    @Test
    fun distanceTo() {
        val a = Vec3(1f, 0f, 0f)
        val b = Vec3(4f, 0f, 0f)
        assertApprox(3f, a.distanceTo(b))
    }

    /* ---- normalize ---- */

    @Test
    fun normalizedUnitLength() {
        val n = Vec3(3f, 4f, 0f).normalized()
        assertApprox(1f, n.magnitude())
        assertApprox(0.6f, n.x)
        assertApprox(0.8f, n.y)
    }

    @Test
    fun normalizedZeroVectorReturnsZero() {
        assertEquals(Vec3.ZERO, Vec3.ZERO.normalized())
    }

    /* ---- lerp ---- */

    @Test
    fun lerpAtZero() {
        val a = Vec3(0f, 0f, 0f)
        val b = Vec3(10f, 10f, 10f)
        assertEquals(a, a.lerp(b, 0f))
    }

    @Test
    fun lerpAtOne() {
        val a = Vec3(0f, 0f, 0f)
        val b = Vec3(10f, 10f, 10f)
        assertEquals(b, a.lerp(b, 1f))
    }

    @Test
    fun lerpMidpoint() {
        val a = Vec3(0f, 0f, 0f)
        val b = Vec3(10f, 10f, 10f)
        val mid = a.lerp(b, 0.5f)
        assertApprox(5f, mid.x)
        assertApprox(5f, mid.y)
        assertApprox(5f, mid.z)
    }

    /* ---- companion constants ---- */

    @Test
    fun companionConstants() {
        assertEquals(Vec3(0f, 0f, 0f), Vec3.ZERO)
        assertEquals(Vec3(1f, 1f, 1f), Vec3.ONE)
        assertEquals(Vec3(0f, 1f, 0f), Vec3.UP)
        assertEquals(Vec3(1f, 0f, 0f), Vec3.RIGHT)
        assertEquals(Vec3(0f, 0f, 1f), Vec3.FORWARD)
    }

    /* ---- helpers ---- */

    private fun assertApprox(expected: Float, actual: Float, eps: Float = 1e-5f) {
        assertTrue(
            kotlin.math.abs(expected - actual) < eps,
            "Expected ~$expected but got $actual"
        )
    }
}
