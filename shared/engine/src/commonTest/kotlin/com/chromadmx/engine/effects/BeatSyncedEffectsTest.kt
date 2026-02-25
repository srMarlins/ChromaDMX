package com.chromadmx.engine.effects

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Vec3
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BeatSyncedEffectsTest {

    private val origin = Vec3.ZERO

    /* ------------------------------------------------------------------ */
    /*  Strobe                                                             */
    /* ------------------------------------------------------------------ */

    @Test
    fun strobeOnDuringDutyCycle() {
        val effect = StrobeEffect()
        val params = EffectParams()
            .with("color", Color.WHITE)
            .with("dutyCycle", 0.5f)
        val beat = BeatState(bpm = 120f, beatPhase = 0.2f, barPhase = 0f, elapsed = 0f)

        val result = effect.compute(origin, 0f, beat, params)
        assertEquals(Color.WHITE, result)
    }

    @Test
    fun strobeOffOutsideDutyCycle() {
        val effect = StrobeEffect()
        val params = EffectParams()
            .with("color", Color.WHITE)
            .with("dutyCycle", 0.5f)
        val beat = BeatState(bpm = 120f, beatPhase = 0.8f, barPhase = 0f, elapsed = 0f)

        val result = effect.compute(origin, 0f, beat, params)
        assertEquals(Color.BLACK, result)
    }

    @Test
    fun strobeUsesParamColor() {
        val effect = StrobeEffect()
        val params = EffectParams()
            .with("color", Color.RED)
            .with("dutyCycle", 1.0f) // always on
        val beat = BeatState(bpm = 120f, beatPhase = 0.5f, barPhase = 0f, elapsed = 0f)

        val result = effect.compute(origin, 0f, beat, params)
        assertEquals(Color.RED, result)
    }

    @Test
    fun strobeZeroDutyCycleAlwaysOff() {
        val effect = StrobeEffect()
        val params = EffectParams()
            .with("color", Color.WHITE)
            .with("dutyCycle", 0.0f)
        val beat = BeatState(bpm = 120f, beatPhase = 0.0f, barPhase = 0f, elapsed = 0f)

        val result = effect.compute(origin, 0f, beat, params)
        assertEquals(Color.BLACK, result)
    }

    @Test
    fun strobeIsPositionIndependent() {
        val effect = StrobeEffect()
        val params = EffectParams().with("color", Color.WHITE).with("dutyCycle", 0.5f)
        val beat = BeatState(bpm = 120f, beatPhase = 0.1f, barPhase = 0f, elapsed = 0f)

        val r1 = effect.compute(Vec3(0f, 0f, 0f), 0f, beat, params)
        val r2 = effect.compute(Vec3(5f, 3f, -2f), 0f, beat, params)
        assertEquals(r1, r2)
    }

    /* ------------------------------------------------------------------ */
    /*  Chase 3D                                                           */
    /* ------------------------------------------------------------------ */

    @Test
    fun chaseAtHeadPositionIsBright() {
        val effect = Chase3DEffect()
        val params = EffectParams()
            .with("axis", "x")
            .with("speed", 1.0f)
            .with("color", Color.WHITE)
            .with("tail", 0.5f)

        // At time=0, head is at x=0
        val result = effect.compute(Vec3(0f, 0f, 0f), 0f, BeatState.IDLE, params)
        assertEquals(1.0f, result.r, 0.01f)
        assertEquals(1.0f, result.g, 0.01f)
        assertEquals(1.0f, result.b, 0.01f)
    }

    @Test
    fun chaseFarFromHeadIsDark() {
        val effect = Chase3DEffect()
        val params = EffectParams()
            .with("axis", "x")
            .with("speed", 1.0f)
            .with("color", Color.WHITE)
            .with("tail", 0.1f)

        // At time=0, head is at x=0. A fixture at x=0.5 is far from tail
        val result = effect.compute(Vec3(0.5f, 0f, 0f), 0f, BeatState.IDLE, params)
        assertEquals(0.0f, result.r, 0.01f)
    }

    @Test
    fun chaseTailFades() {
        val effect = Chase3DEffect()
        val params = EffectParams()
            .with("axis", "x")
            .with("speed", 0f) // static head at x=0
            .with("color", Color.WHITE)
            .with("tail", 1.0f) // tail extends full range

        // At head (x=0) should be brightest
        val atHead = effect.compute(Vec3(0f, 0f, 0f), 0f, BeatState.IDLE, params)
        // Slightly behind head
        val behind = effect.compute(Vec3(0.5f, 0f, 0f), 0f, BeatState.IDLE, params)

        assertTrue(atHead.r > behind.r, "Head should be brighter than tail")
    }

    @Test
    fun chaseIsDeterministic() {
        val effect = Chase3DEffect()
        val params = EffectParams()
            .with("axis", "x")
            .with("speed", 2.0f)
            .with("color", Color.RED)
            .with("tail", 0.3f)
        val pos = Vec3(0.3f, 0f, 0f)

        val c1 = effect.compute(pos, 1.5f, BeatState.IDLE, params)
        val c2 = effect.compute(pos, 1.5f, BeatState.IDLE, params)
        assertEquals(c1, c2)
    }

    /* ------------------------------------------------------------------ */
    /*  Wave 3D                                                            */
    /* ------------------------------------------------------------------ */

    @Test
    fun waveOscillatesBetweenColors() {
        val effect = WaveEffect3DEffect()
        val params = EffectParams()
            .with("axis", "x")
            .with("wavelength", 1.0f)
            .with("speed", 0f) // static wave
            .with("colors", listOf(Color.BLACK, Color.WHITE))

        // Sample several x positions, should find both high and low values
        var foundHigh = false
        var foundLow = false
        for (i in 0 until 20) {
            val x = i * 0.05f
            val c = effect.compute(Vec3(x, 0f, 0f), 0f, BeatState.IDLE, params)
            if (c.r > 0.9f) foundHigh = true
            if (c.r < 0.1f) foundLow = true
        }
        assertTrue(foundHigh, "Wave should reach near-white")
        assertTrue(foundLow, "Wave should reach near-black")
    }

    @Test
    fun waveUsesYAxis() {
        val effect = WaveEffect3DEffect()
        val params = EffectParams()
            .with("axis", "y")
            .with("wavelength", 1.0f)
            .with("speed", 0f)
            .with("colors", listOf(Color.RED, Color.BLUE))

        // Two fixtures at same x but different y should get different colors
        val c1 = effect.compute(Vec3(0f, 0.0f, 0f), 0f, BeatState.IDLE, params)
        val c2 = effect.compute(Vec3(0f, 0.25f, 0f), 0f, BeatState.IDLE, params)

        // They should be different (sin at different points in the wave)
        val diff = abs(c1.r - c2.r) + abs(c1.b - c2.b)
        assertTrue(diff > 0.01f, "Different y positions should produce different colors")
    }

    @Test
    fun waveIsDeterministic() {
        val effect = WaveEffect3DEffect()
        val params = EffectParams()
            .with("axis", "x")
            .with("wavelength", 2.0f)
            .with("speed", 1.0f)
            .with("colors", listOf(Color.RED, Color.GREEN))
        val pos = Vec3(0.3f, 0f, 0f)

        val c1 = effect.compute(pos, 2.0f, BeatState.IDLE, params)
        val c2 = effect.compute(pos, 2.0f, BeatState.IDLE, params)
        assertEquals(c1, c2)
    }

    @Test
    fun waveSpeedShiftsPhase() {
        val effect = WaveEffect3DEffect()
        val params = EffectParams()
            .with("axis", "x")
            .with("wavelength", 1.0f)
            .with("speed", 1.0f)
            .with("colors", listOf(Color.BLACK, Color.WHITE))

        val pos = Vec3(0.5f, 0f, 0f)
        val c1 = effect.compute(pos, 0f, BeatState.IDLE, params)
        val c2 = effect.compute(pos, 0.25f, BeatState.IDLE, params)

        // At different times the wave should give different colors
        val diff = abs(c1.r - c2.r)
        assertTrue(diff > 0.01f, "Different times should produce different colors for moving wave")
    }
}
