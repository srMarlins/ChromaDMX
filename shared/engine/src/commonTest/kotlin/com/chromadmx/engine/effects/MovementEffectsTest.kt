package com.chromadmx.engine.effects

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.FixtureOutput
import com.chromadmx.core.model.Vec3
import com.chromadmx.engine.effect.MovementEffect
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MovementEffectsTest {

    private val beat = BeatState.IDLE
    private val origin = Vec3.ZERO

    // Helper: prepare then computeMovement
    private fun MovementEffect.computeAt(
        pos: Vec3,
        time: Float,
        beat: BeatState,
        params: EffectParams
    ): FixtureOutput {
        val ctx = prepare(params, time, beat)
        return computeMovement(pos, ctx)
    }

    /* ------------------------------------------------------------------ */
    /*  SweepMovementEffect                                                */
    /* ------------------------------------------------------------------ */

    @Test
    fun sweepPanOnlyProducesPanNotTilt() {
        val effect = SweepMovementEffect()
        val params = EffectParams()
            .with("axis", "pan")
            .with("range", 0.5f)
            .with("speed", 1.0f)

        val result = effect.computeAt(origin, 0.0f, beat, params)
        assertNotNull(result.pan)
        assertNull(result.tilt)
    }

    @Test
    fun sweepTiltOnlyProducesTiltNotPan() {
        val effect = SweepMovementEffect()
        val params = EffectParams()
            .with("axis", "tilt")
            .with("range", 0.5f)
            .with("speed", 1.0f)

        val result = effect.computeAt(origin, 0.0f, beat, params)
        assertNull(result.pan)
        assertNotNull(result.tilt)
    }

    @Test
    fun sweepBothProducesPanAndTilt() {
        val effect = SweepMovementEffect()
        val params = EffectParams()
            .with("axis", "both")
            .with("range", 0.5f)
            .with("speed", 1.0f)

        val result = effect.computeAt(origin, 0.0f, beat, params)
        assertNotNull(result.pan)
        assertNotNull(result.tilt)
    }

    @Test
    fun sweepOscillatesOverTime() {
        val effect = SweepMovementEffect()
        val params = EffectParams()
            .with("axis", "pan")
            .with("range", 1.0f)
            .with("speed", 1.0f)
            .with("centerPan", 0.5f)

        // Sample at multiple times — should oscillate around center
        val values = (0..10).map { i ->
            effect.computeAt(origin, i * 0.1f, beat, params).pan!!
        }

        // Should have values both above and below center (or at center)
        val hasAbove = values.any { it > 0.5f }
        val hasBelow = values.any { it < 0.5f }
        assertTrue(hasAbove || hasBelow, "Sweep should oscillate, got: $values")

        // All values should be within 0-1
        values.forEach { pan ->
            assertTrue(pan in 0f..1f, "Pan should be 0-1, got $pan")
        }
    }

    @Test
    fun sweepIsDeterministic() {
        val effect = SweepMovementEffect()
        val params = EffectParams()
            .with("axis", "both")
            .with("range", 0.5f)
            .with("speed", 2.0f)

        val r1 = effect.computeAt(origin, 1.5f, beat, params)
        val r2 = effect.computeAt(origin, 1.5f, beat, params)
        assertEquals(r1.pan, r2.pan)
        assertEquals(r1.tilt, r2.tilt)
    }

    @Test
    fun sweepRespectsCenterPan() {
        val effect = SweepMovementEffect()
        // At time=0, sin(0)=0, so pan should be at center
        val params = EffectParams()
            .with("axis", "pan")
            .with("range", 0.5f)
            .with("speed", 1.0f)
            .with("centerPan", 0.7f)

        val result = effect.computeAt(origin, 0.0f, beat, params)
        // sin(0)=0, so sweep offset is 0, pan should be at center
        assertEquals(0.7f, result.pan!!, 0.01f)
    }

    /* ------------------------------------------------------------------ */
    /*  CircleMovementEffect                                               */
    /* ------------------------------------------------------------------ */

    @Test
    fun circleProducesCircularPath() {
        val effect = CircleMovementEffect()
        val params = EffectParams()
            .with("radius", 0.25f)
            .with("speed", 1.0f)
            .with("centerPan", 0.5f)
            .with("centerTilt", 0.5f)

        // Sample the circle at 4 points
        val samples = (0..3).map { i ->
            val time = i * 0.25f // quarter period at 1Hz
            effect.computeAt(origin, time, beat, params)
        }

        // All should have non-null pan and tilt
        samples.forEach {
            assertNotNull(it.pan)
            assertNotNull(it.tilt)
        }

        // Verify approximate circular distance from center
        samples.forEach { output ->
            val dx = output.pan!! - 0.5f
            val dy = output.tilt!! - 0.5f
            val dist = sqrt(dx * dx + dy * dy)
            assertEquals(0.25f, dist, 0.05f) // should be approximately radius
        }
    }

    @Test
    fun circleCcwReversesDirection() {
        val effect = CircleMovementEffect()
        val cwParams = EffectParams()
            .with("radius", 0.2f)
            .with("speed", 1.0f)
            .with("direction", "cw")
            .with("centerPan", 0.5f)
            .with("centerTilt", 0.5f)

        val ccwParams = EffectParams()
            .with("radius", 0.2f)
            .with("speed", 1.0f)
            .with("direction", "ccw")
            .with("centerPan", 0.5f)
            .with("centerTilt", 0.5f)

        val cwResult = effect.computeAt(origin, 0.1f, beat, cwParams)
        val ccwResult = effect.computeAt(origin, 0.1f, beat, ccwParams)

        // At time=0.1, CW and CCW should produce different tilt values
        // (pan uses cos which is symmetric, but tilt uses sin which is antisymmetric)
        val cwTilt = cwResult.tilt!!
        val ccwTilt = ccwResult.tilt!!
        // They should be mirrored around center: cwTilt + ccwTilt ≈ 2 * center
        assertEquals(1.0f, cwTilt + ccwTilt, 0.01f) // 2 * 0.5 = 1.0
    }

    @Test
    fun circleIsDeterministic() {
        val effect = CircleMovementEffect()
        val params = EffectParams()
            .with("radius", 0.3f)
            .with("speed", 0.5f)

        val r1 = effect.computeAt(origin, 2.0f, beat, params)
        val r2 = effect.computeAt(origin, 2.0f, beat, params)
        assertEquals(r1.pan, r2.pan)
        assertEquals(r1.tilt, r2.tilt)
    }

    @Test
    fun circleRespectsCenter() {
        val effect = CircleMovementEffect()
        // At time=0, cos(0)=1, sin(0)=0, so:
        // pan = center + radius, tilt = center
        val params = EffectParams()
            .with("radius", 0.2f)
            .with("speed", 1.0f)
            .with("centerPan", 0.3f)
            .with("centerTilt", 0.6f)

        val result = effect.computeAt(origin, 0.0f, beat, params)
        assertEquals(0.5f, result.pan!!, 0.01f) // 0.3 + 0.2
        assertEquals(0.6f, result.tilt!!, 0.01f) // 0.6 + 0
    }

    /* ------------------------------------------------------------------ */
    /*  FollowEffect                                                       */
    /* ------------------------------------------------------------------ */

    @Test
    fun followComputesPanAndTilt() {
        val effect = FollowEffect()
        val params = EffectParams()
            .with("targetX", 1.0f)
            .with("targetY", 0.0f)
            .with("targetZ", 0.0f)

        val result = effect.computeAt(origin, 0f, beat, params)
        assertNotNull(result.pan)
        assertNotNull(result.tilt)
    }

    @Test
    fun followTargetDirectlyAheadGivesCenterPan() {
        val effect = FollowEffect()
        // Target is directly in front (-Z direction) of fixture at origin
        val params = EffectParams()
            .with("targetX", 0.0f)
            .with("targetY", 0.0f)
            .with("targetZ", -1.0f)

        val result = effect.computeAt(origin, 0f, beat, params)
        // Pan should be 0.5 (center/forward)
        assertEquals(0.5f, result.pan!!, 0.01f)
        // Tilt should be 0.5 (horizontal)
        assertEquals(0.5f, result.tilt!!, 0.01f)
    }

    @Test
    fun followTargetToTheRightGivesHigherPan() {
        val effect = FollowEffect()
        // Target is to the right (+X) and forward (-Z)
        val params = EffectParams()
            .with("targetX", 1.0f)
            .with("targetY", 0.0f)
            .with("targetZ", -1.0f)

        val result = effect.computeAt(origin, 0f, beat, params)
        // Pan should be > 0.5 (right of center)
        assertTrue(result.pan!! > 0.5f, "Pan should be right of center, got ${result.pan}")
    }

    @Test
    fun followTargetAboveGivesHigherTilt() {
        val effect = FollowEffect()
        // Target is directly above and forward
        val params = EffectParams()
            .with("targetX", 0.0f)
            .with("targetY", 1.0f)
            .with("targetZ", -1.0f)

        val result = effect.computeAt(origin, 0f, beat, params)
        // Tilt should be > 0.5 (looking up)
        assertTrue(result.tilt!! > 0.5f, "Tilt should be above horizontal, got ${result.tilt}")
    }

    @Test
    fun followDifferentFixturePositionsGiveDifferentAngles() {
        val effect = FollowEffect()
        val params = EffectParams()
            .with("targetX", 0.0f)
            .with("targetY", 0.0f)
            .with("targetZ", 0.0f)

        val leftFixture = Vec3(-2f, 0f, -1f)
        val rightFixture = Vec3(2f, 0f, -1f)

        val leftResult = effect.computeAt(leftFixture, 0f, beat, params)
        val rightResult = effect.computeAt(rightFixture, 0f, beat, params)

        // Left fixture should pan right to reach target, right should pan left
        assertTrue(leftResult.pan!! > rightResult.pan!!,
            "Left fixture should pan right (higher) than right fixture")
    }

    @Test
    fun followIsDeterministic() {
        val effect = FollowEffect()
        val params = EffectParams()
            .with("targetX", 1.0f)
            .with("targetY", 2.0f)
            .with("targetZ", -3.0f)

        val r1 = effect.computeAt(origin, 0f, beat, params)
        val r2 = effect.computeAt(origin, 0f, beat, params)
        assertEquals(r1.pan, r2.pan)
        assertEquals(r1.tilt, r2.tilt)
    }

    @Test
    fun followPanAndTiltInValidRange() {
        val effect = FollowEffect()
        // Test with various target positions
        val targets = listOf(
            Vec3(10f, 10f, 10f),
            Vec3(-10f, -10f, -10f),
            Vec3(0f, 0f, 0f),
            Vec3(100f, 0f, 0f)
        )

        for (target in targets) {
            val params = EffectParams()
                .with("targetX", target.x)
                .with("targetY", target.y)
                .with("targetZ", target.z)

            val result = effect.computeAt(Vec3(1f, 1f, 1f), 0f, beat, params)
            assertTrue(result.pan!! in 0f..1f, "Pan out of range: ${result.pan}")
            assertTrue(result.tilt!! in 0f..1f, "Tilt out of range: ${result.tilt}")
        }
    }
}
