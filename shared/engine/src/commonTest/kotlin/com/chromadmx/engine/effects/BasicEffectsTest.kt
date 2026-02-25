package com.chromadmx.engine.effects

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Vec3
import com.chromadmx.engine.effect.SpatialEffect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BasicEffectsTest {

    private val beat = BeatState.IDLE
    private val origin = Vec3.ZERO

    /* ------------------------------------------------------------------ */
    /*  SolidColor                                                         */
    /* ------------------------------------------------------------------ */

    @Test
    fun solidColorReturnsParamColor() {
        val effect = SolidColorEffect()
        val params = EffectParams().with("color", Color.RED)
        val result = effect.compute(origin, 0f, beat, params)
        assertEquals(Color.RED, result)
    }

    @Test
    fun solidColorDefaultsToWhite() {
        val effect = SolidColorEffect()
        val result = effect.compute(origin, 0f, beat, EffectParams.EMPTY)
        assertEquals(Color.WHITE, result)
    }

    @Test
    fun solidColorPositionIndependent() {
        val effect = SolidColorEffect()
        val params = EffectParams().with("color", Color.BLUE)
        val r1 = effect.compute(Vec3(0f, 0f, 0f), 0f, beat, params)
        val r2 = effect.compute(Vec3(5f, 3f, -1f), 0f, beat, params)
        assertEquals(r1, r2)
    }

    @Test
    fun solidColorTimeIndependent() {
        val effect = SolidColorEffect()
        val params = EffectParams().with("color", Color.GREEN)
        val r1 = effect.compute(origin, 0f, beat, params)
        val r2 = effect.compute(origin, 100f, beat, params)
        assertEquals(r1, r2)
    }

    /* ------------------------------------------------------------------ */
    /*  GradientSweep3D                                                    */
    /* ------------------------------------------------------------------ */

    @Test
    fun gradientSweepReturnsDifferentColorsAtDifferentPositions() {
        val effect = GradientSweep3DEffect()
        val palette = listOf(Color.RED, Color.BLUE)
        val params = EffectParams()
            .with("axis", "x")
            .with("speed", 0f) // no scrolling
            .with("palette", palette)

        val c1 = effect.compute(Vec3(0.0f, 0f, 0f), 0f, beat, params)
        val c2 = effect.compute(Vec3(0.5f, 0f, 0f), 0f, beat, params)

        // At x=0 (t=0) should be red, at x=0.5 (t=0.5) should be midway
        assertEquals(1.0f, c1.r, 0.01f)
        assertEquals(0.0f, c1.b, 0.01f)
        // At midpoint, should be 50/50 red-blue
        assertEquals(0.5f, c2.r, 0.01f)
        assertEquals(0.5f, c2.b, 0.01f)
    }

    @Test
    fun gradientSweepAxisY() {
        val effect = GradientSweep3DEffect()
        val palette = listOf(Color.RED, Color.GREEN)
        val params = EffectParams()
            .with("axis", "y")
            .with("speed", 0f)
            .with("palette", palette)

        val c1 = effect.compute(Vec3(0f, 0.0f, 0f), 0f, beat, params)
        val c2 = effect.compute(Vec3(0f, 0.5f, 0f), 0f, beat, params)

        // x should not matter for y-axis sweep
        assertEquals(1.0f, c1.r, 0.01f) // RED at y=0
        assertTrue(c2.g > 0.4f) // Greener at y=0.5
    }

    @Test
    fun gradientSweepIsDeterministic() {
        val effect = GradientSweep3DEffect()
        val params = EffectParams()
            .with("axis", "x")
            .with("speed", 1.0f)
            .with("palette", listOf(Color.RED, Color.GREEN, Color.BLUE))
        val pos = Vec3(0.3f, 0.5f, 0.7f)

        val c1 = effect.compute(pos, 2.5f, beat, params)
        val c2 = effect.compute(pos, 2.5f, beat, params)
        assertEquals(c1, c2)
    }

    /* ------------------------------------------------------------------ */
    /*  RainbowSweep3D                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    fun rainbowSweepAtOriginZeroTimeIsRed() {
        val effect = RainbowSweep3DEffect()
        val params = EffectParams()
            .with("axis", "x")
            .with("speed", 0f)
            .with("spread", 1.0f)

        // At x=0, hue=0 -> red
        val c = effect.compute(Vec3(0f, 0f, 0f), 0f, beat, params)
        assertEquals(1.0f, c.r, 0.01f)
        assertEquals(0.0f, c.g, 0.01f)
        assertEquals(0.0f, c.b, 0.01f)
    }

    @Test
    fun rainbowSweepAtOneThirdIsGreen() {
        val effect = RainbowSweep3DEffect()
        val params = EffectParams()
            .with("axis", "x")
            .with("speed", 0f)
            .with("spread", 1.0f)

        // At x=1/3, hue=120 -> green
        val c = effect.compute(Vec3(1f / 3f, 0f, 0f), 0f, beat, params)
        assertEquals(0.0f, c.r, 0.01f)
        assertEquals(1.0f, c.g, 0.01f)
        assertEquals(0.0f, c.b, 0.01f)
    }

    @Test
    fun rainbowSweepAtTwoThirdsIsBlue() {
        val effect = RainbowSweep3DEffect()
        val params = EffectParams()
            .with("axis", "x")
            .with("speed", 0f)
            .with("spread", 1.0f)

        // At x=2/3, hue=240 -> blue
        val c = effect.compute(Vec3(2f / 3f, 0f, 0f), 0f, beat, params)
        assertEquals(0.0f, c.r, 0.01f)
        assertEquals(0.0f, c.g, 0.01f)
        assertEquals(1.0f, c.b, 0.01f)
    }

    @Test
    fun rainbowSweepZAxis() {
        val effect = RainbowSweep3DEffect()
        val params = EffectParams()
            .with("axis", "z")
            .with("speed", 0f)
            .with("spread", 1.0f)

        // At z=0, hue=0 -> red regardless of x,y
        val c = effect.compute(Vec3(5f, 3f, 0f), 0f, beat, params)
        assertEquals(1.0f, c.r, 0.01f)
        assertEquals(0.0f, c.g, 0.01f)
    }

    @Test
    fun rainbowSweepIsDeterministic() {
        val effect = RainbowSweep3DEffect()
        val params = EffectParams()
            .with("axis", "x")
            .with("speed", 2.0f)
            .with("spread", 0.5f)
        val pos = Vec3(0.7f, 0f, 0f)

        val c1 = effect.compute(pos, 1.5f, beat, params)
        val c2 = effect.compute(pos, 1.5f, beat, params)
        assertEquals(c1, c2)
    }
}

// Helper for migration: calls prepare() then compute()
private fun SpatialEffect.compute(pos: Vec3, time: Float, beat: BeatState, params: EffectParams): Color {
    val ctx = this.prepare(params, time, beat)
    return this.compute(pos, ctx)
}
