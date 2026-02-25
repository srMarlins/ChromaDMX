package com.chromadmx.core.util

import kotlin.test.Test
import kotlin.test.assertTrue

class MathUtilsTest {

    @Test
    fun lerpEndpoints() {
        assertApprox(0f, MathUtils.lerp(0f, 10f, 0f))
        assertApprox(10f, MathUtils.lerp(0f, 10f, 1f))
    }

    @Test
    fun lerpMidpoint() {
        assertApprox(5f, MathUtils.lerp(0f, 10f, 0.5f))
    }

    @Test
    fun inverseLerp() {
        assertApprox(0.5f, MathUtils.inverseLerp(0f, 10f, 5f))
    }

    @Test
    fun inverseLerpDegenerateRange() {
        assertApprox(0f, MathUtils.inverseLerp(5f, 5f, 5f))
    }

    @Test
    fun remap() {
        // 5 in 0..10 -> 50 in 0..100
        assertApprox(50f, MathUtils.remap(5f, 0f, 10f, 0f, 100f))
    }

    @Test
    fun clamp() {
        assertApprox(0f, MathUtils.clamp(-1f))
        assertApprox(1f, MathUtils.clamp(2f))
        assertApprox(0.5f, MathUtils.clamp(0.5f))
    }

    @Test
    fun smoothStep() {
        assertApprox(0f, MathUtils.smoothStep(0f))
        assertApprox(1f, MathUtils.smoothStep(1f))
        assertApprox(0.5f, MathUtils.smoothStep(0.5f))
    }

    @Test
    fun wrapPositive() {
        assertApprox(0.5f, MathUtils.wrap(4.5f, 1f))
    }

    @Test
    fun wrapNegative() {
        assertApprox(0.75f, MathUtils.wrap(-0.25f, 1f))
    }

    @Test
    fun wrapZeroMax() {
        assertApprox(0f, MathUtils.wrap(5f, 0f))
    }

    @Test
    fun tauApproximatelyTwoPi() {
        assertApprox(6.2831855f, MathUtils.TAU)
    }

    private fun assertApprox(expected: Float, actual: Float, eps: Float = 1e-5f) {
        assertTrue(
            kotlin.math.abs(expected - actual) < eps,
            "Expected ~$expected but got $actual"
        )
    }
}
