package com.chromadmx.engine.effects

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Vec3
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.chromadmx.engine.effect.SpatialEffect

class AdvancedEffectsTest {

    private val beat = BeatState.IDLE

    /* ------------------------------------------------------------------ */
    /*  RadialPulse3D                                                      */
    /* ------------------------------------------------------------------ */

    @Test
    fun radialPulseAtCenterTimeZeroIsBright() {
        val effect = RadialPulse3DEffect()
        val params = EffectParams()
            .with("centerX", 0f)
            .with("centerY", 0f)
            .with("centerZ", 0f)
            .with("speed", 2.0f)
            .with("color", Color.WHITE)
            .with("width", 0.5f)

        // At time=0, radius=0, fixture at center => shellDist=0 => full brightness
        val result = effect.compute(Vec3.ZERO, 0f, beat, params)
        assertEquals(1.0f, result.r, 0.01f)
    }

    @Test
    fun radialPulseFarFromShellIsDark() {
        val effect = RadialPulse3DEffect()
        val params = EffectParams()
            .with("centerX", 0f)
            .with("centerY", 0f)
            .with("centerZ", 0f)
            .with("speed", 1.0f)
            .with("color", Color.WHITE)
            .with("width", 0.2f)

        // At time=1, radius=1. Fixture at distance 5 is far from shell (at radius=1)
        val result = effect.compute(Vec3(5f, 0f, 0f), 1f, beat, params)
        assertEquals(0.0f, result.r, 0.01f)
    }

    @Test
    fun radialPulseShellExpands() {
        val effect = RadialPulse3DEffect()
        val params = EffectParams()
            .with("centerX", 0f)
            .with("centerY", 0f)
            .with("centerZ", 0f)
            .with("speed", 1.0f)
            .with("color", Color.WHITE)
            .with("width", 0.4f)
            .with("maxRadius", 10f)

        // Fixture at distance 2 from center
        val pos = Vec3(2f, 0f, 0f)

        // At time=1 (radius=1), fixture at dist=2 is 1 unit from shell => dark
        val early = effect.compute(pos, 1f, beat, params)
        // At time=2 (radius=2), fixture at dist=2 is 0 from shell => bright
        val onShell = effect.compute(pos, 2f, beat, params)

        assertTrue(onShell.r > early.r, "Fixture should brighten as shell reaches it")
    }

    @Test
    fun radialPulseIsDeterministic() {
        val effect = RadialPulse3DEffect()
        val params = EffectParams()
            .with("speed", 2.0f)
            .with("color", Color.RED)
            .with("width", 0.3f)
        val pos = Vec3(1f, 1f, 1f)

        val c1 = effect.compute(pos, 1.5f, beat, params)
        val c2 = effect.compute(pos, 1.5f, beat, params)
        assertEquals(c1, c2)
    }

    /* ------------------------------------------------------------------ */
    /*  PerlinNoise3D                                                      */
    /* ------------------------------------------------------------------ */

    @Test
    fun perlinNoiseProducesColorInRange() {
        val effect = PerlinNoise3DEffect()
        val params = EffectParams()
            .with("scale", 1.0f)
            .with("speed", 0.5f)
            .with("palette", listOf(Color.BLACK, Color.WHITE))

        for (i in 0 until 50) {
            val pos = Vec3(i * 0.3f, i * 0.2f, i * 0.1f)
            val c = effect.compute(pos, 0f, beat, params)
            assertTrue(c.r >= 0f && c.r <= 1f, "R out of range: ${c.r}")
            assertTrue(c.g >= 0f && c.g <= 1f, "G out of range: ${c.g}")
            assertTrue(c.b >= 0f && c.b <= 1f, "B out of range: ${c.b}")
        }
    }

    @Test
    fun perlinNoiseVariesSpatially() {
        val effect = PerlinNoise3DEffect()
        val params = EffectParams()
            .with("scale", 2.0f)
            .with("speed", 0f)
            .with("palette", listOf(Color.BLACK, Color.WHITE))

        val colors = (0 until 10).map { i ->
            effect.compute(Vec3(i * 0.5f, i * 0.3f, i * 0.2f), 0f, beat, params)
        }

        // Not all colors should be the same
        val distinct = colors.map { (it.r * 100).toInt() }.toSet()
        assertTrue(distinct.size > 1, "Perlin noise should vary across positions")
    }

    @Test
    fun perlinNoiseIsDeterministic() {
        val effect = PerlinNoise3DEffect()
        val params = EffectParams()
            .with("scale", 1.5f)
            .with("speed", 1.0f)
            .with("palette", listOf(Color.RED, Color.BLUE))
        val pos = Vec3(0.7f, 0.3f, 0.5f)

        val c1 = effect.compute(pos, 2.0f, beat, params)
        val c2 = effect.compute(pos, 2.0f, beat, params)
        assertEquals(c1, c2)
    }

    @Test
    fun perlinNoiseUsesColorPalette() {
        val effect = PerlinNoise3DEffect()
        val params = EffectParams()
            .with("scale", 1.0f)
            .with("speed", 0f)
            .with("palette", listOf(Color.RED, Color.GREEN))

        val c = effect.compute(Vec3(0.5f, 0.5f, 0.5f), 0f, beat, params)
        // Should be a mix of red and green (no blue component)
        assertEquals(0f, c.b, 0.01f)
        // Should have some red and/or green
        assertTrue(c.r > 0f || c.g > 0f, "Should use palette colors")
    }

    /* ------------------------------------------------------------------ */
    /*  ParticleBurst3D                                                    */
    /* ------------------------------------------------------------------ */

    @Test
    fun particleBurstAtCenterTimeZeroIsBright() {
        val effect = ParticleBurst3DEffect()
        val params = EffectParams()
            .with("centerX", 0f)
            .with("centerY", 0f)
            .with("centerZ", 0f)
            .with("speed", 3.0f)
            .with("count", 12)
            .with("fade", 0.5f)
            .with("color", Color.WHITE)

        // At time=0, all particles are at center, fixture at center => bright
        val result = effect.compute(Vec3.ZERO, 0f, beat, params)
        assertTrue(result.r > 0.5f, "Center fixture at t=0 should be bright, got ${result.r}")
    }

    @Test
    fun particleBurstFarAwayIsDark() {
        val effect = ParticleBurst3DEffect()
        val params = EffectParams()
            .with("centerX", 0f)
            .with("centerY", 0f)
            .with("centerZ", 0f)
            .with("speed", 1.0f)
            .with("count", 8)
            .with("fade", 0.3f)
            .with("color", Color.WHITE)

        // Fixture very far away, time=0 (particles at center)
        val result = effect.compute(Vec3(100f, 100f, 100f), 0f, beat, params)
        assertEquals(0f, result.r, 0.01f)
    }

    @Test
    fun particleBurstIsDeterministic() {
        val effect = ParticleBurst3DEffect()
        val params = EffectParams()
            .with("speed", 2.0f)
            .with("count", 16)
            .with("fade", 0.4f)
            .with("color", Color.RED)
        val pos = Vec3(1f, 0.5f, 0f)

        val c1 = effect.compute(pos, 0.5f, beat, params)
        val c2 = effect.compute(pos, 0.5f, beat, params)
        assertEquals(c1, c2)
    }

    @Test
    fun particleDirectionsAreUnitVectors() {
        for (i in 0 until 20) {
            val dir = ParticleBurst3DEffect.particleDirection(i, 20)
            val mag = dir.magnitude()
            assertEquals(1f, mag, 0.01f, "Particle direction $i should be unit vector, got $mag")
        }
    }

    @Test
    fun particleDirectionsAreDistinct() {
        val dirs = (0 until 12).map { ParticleBurst3DEffect.particleDirection(it, 12) }
        // All directions should be different
        for (i in dirs.indices) {
            for (j in i + 1 until dirs.size) {
                val dist = dirs[i].distanceTo(dirs[j])
                assertTrue(dist > 0.01f, "Directions $i and $j too similar: dist=$dist")
            }
        }
    }
}

// Helper for migration: calls prepare() then compute()
private fun SpatialEffect.compute(pos: Vec3, time: Float, beat: BeatState, params: EffectParams): Color {
    val ctx = this.prepare(params, time, beat)
    return this.compute(pos, ctx)
}
