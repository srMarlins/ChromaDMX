package com.chromadmx.engine.effects

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.FixtureOutput
import com.chromadmx.core.model.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RandomMovementTest {

    private val origin = Vec3.ZERO

    private fun computeAt(
        params: EffectParams,
        beat: BeatState,
        pos: Vec3 = origin
    ): FixtureOutput {
        val effect = RandomMovementEffect()
        val ctx = effect.prepare(params, beat.elapsed, beat)
        return effect.computeMovement(pos, ctx)
    }

    @Test
    fun randomProducesPanAndTilt() {
        val params = EffectParams()
            .with("rangePan", 0.5f)
            .with("rangeTilt", 0.5f)
        val beat = BeatState(bpm = 120f, beatPhase = 0f, barPhase = 0f, elapsed = 0f)

        val result = computeAt(params, beat)
        assertNotNull(result.pan)
        assertNotNull(result.tilt)
    }

    @Test
    fun randomDoesNotSetGoboOrOtherChannels() {
        val params = EffectParams()
        val beat = BeatState(bpm = 120f, beatPhase = 0f, barPhase = 0f, elapsed = 0f)

        val result = computeAt(params, beat)
        assertNull(result.gobo)
        assertNull(result.focus)
        assertNull(result.zoom)
        assertNull(result.strobeRate)
    }

    @Test
    fun randomChangesOnDifferentBeats() {
        val params = EffectParams()
            .with("rangePan", 1.0f)
            .with("rangeTilt", 1.0f)

        // At 120 BPM, 1 beat = 0.5s
        val beat0 = BeatState(bpm = 120f, beatPhase = 0f, barPhase = 0f, elapsed = 0f)
        val beat1 = BeatState(bpm = 120f, beatPhase = 0f, barPhase = 0.25f, elapsed = 0.5f)

        val r0 = computeAt(params, beat0)
        val r1 = computeAt(params, beat1)

        // Different beats should produce different positions (extremely unlikely to match)
        assertTrue(r0.pan != r1.pan || r0.tilt != r1.tilt,
            "Different beats should produce different positions")
    }

    @Test
    fun randomIsDeterministic() {
        val params = EffectParams()
            .with("rangePan", 0.8f)
            .with("rangeTilt", 0.6f)
        val beat = BeatState(bpm = 120f, beatPhase = 0.5f, barPhase = 0.125f, elapsed = 1.0f)

        val r1 = computeAt(params, beat)
        val r2 = computeAt(params, beat)
        assertEquals(r1.pan, r2.pan)
        assertEquals(r1.tilt, r2.tilt)
    }

    @Test
    fun randomOutputStaysInRange() {
        val params = EffectParams()
            .with("rangePan", 1.0f)
            .with("rangeTilt", 1.0f)
            .with("centerPan", 0.5f)
            .with("centerTilt", 0.5f)

        // Test many different beats
        for (i in 0..50) {
            val beat = BeatState(bpm = 120f, beatPhase = 0f, barPhase = 0f, elapsed = i * 0.5f)
            val result = computeAt(params, beat)
            assertTrue(result.pan!! in 0f..1f, "Pan out of range at beat $i: ${result.pan}")
            assertTrue(result.tilt!! in 0f..1f, "Tilt out of range at beat $i: ${result.tilt}")
        }
    }

    @Test
    fun randomPerFixtureProducesDifferentPositions() {
        val params = EffectParams()
            .with("rangePan", 1.0f)
            .with("rangeTilt", 1.0f)
            .with("perFixture", true)
        val beat = BeatState(bpm = 120f, beatPhase = 0f, barPhase = 0f, elapsed = 1.0f)

        val pos1 = Vec3(0f, 0f, 0f)
        val pos2 = Vec3(1f, 0f, 0f)

        val r1 = computeAt(params, beat, pos1)
        val r2 = computeAt(params, beat, pos2)

        // Different positions should produce different random values
        assertTrue(r1.pan != r2.pan || r1.tilt != r2.tilt,
            "Per-fixture mode should produce different positions for different fixtures")
    }

    @Test
    fun randomWithoutPerFixtureGivesSamePositions() {
        val params = EffectParams()
            .with("rangePan", 1.0f)
            .with("rangeTilt", 1.0f)
            .with("perFixture", false)
        val beat = BeatState(bpm = 120f, beatPhase = 0f, barPhase = 0f, elapsed = 1.0f)

        val pos1 = Vec3(0f, 0f, 0f)
        val pos2 = Vec3(5f, 3f, -1f)

        val r1 = computeAt(params, beat, pos1)
        val r2 = computeAt(params, beat, pos2)

        // Without per-fixture, all positions should get same random value
        assertEquals(r1.pan, r2.pan)
        assertEquals(r1.tilt, r2.tilt)
    }

    @Test
    fun hashIsDeterministic() {
        val h1 = RandomMovementEffect.hash(42)
        val h2 = RandomMovementEffect.hash(42)
        assertEquals(h1, h2)
    }

    @Test
    fun hashDifferentInputsGiveDifferentOutputs() {
        val h1 = RandomMovementEffect.hash(0)
        val h2 = RandomMovementEffect.hash(1)
        assertTrue(h1 != h2, "Different inputs should produce different hashes")
    }
}
