package com.chromadmx.engine.effects

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GoboBeatSyncTest {

    private val origin = Vec3.ZERO

    @Test
    fun goboReturnsSlotIndex() {
        val effect = GoboBeatSyncEffect()
        val params = EffectParams()
            .with("slotCount", 8)
            .with("changeOnBeat", true)

        val beat = BeatState(bpm = 120f, beatPhase = 0f, barPhase = 0f, elapsed = 0f)
        val ctx = effect.prepare(params, 0f, beat)
        val result = effect.computeMovement(origin, ctx)

        assertNotNull(result.gobo)
        assertTrue(result.gobo!! in 0 until 8)
    }

    @Test
    fun goboChangesOnBeatTransition() {
        val effect = GoboBeatSyncEffect()
        val params = EffectParams()
            .with("slotCount", 4)
            .with("changeOnBeat", true)

        // At 120 BPM, 1 beat = 0.5 seconds
        val beat0 = BeatState(bpm = 120f, beatPhase = 0f, barPhase = 0f, elapsed = 0f)
        val beat1 = BeatState(bpm = 120f, beatPhase = 0f, barPhase = 0.25f, elapsed = 0.5f)
        val beat2 = BeatState(bpm = 120f, beatPhase = 0f, barPhase = 0.5f, elapsed = 1.0f)
        val beat3 = BeatState(bpm = 120f, beatPhase = 0f, barPhase = 0.75f, elapsed = 1.5f)

        val gobo0 = getGobo(effect, params, beat0)
        val gobo1 = getGobo(effect, params, beat1)
        val gobo2 = getGobo(effect, params, beat2)
        val gobo3 = getGobo(effect, params, beat3)

        // Each beat should produce a different (sequential) gobo slot
        assertEquals(0, gobo0)
        assertEquals(1, gobo1)
        assertEquals(2, gobo2)
        assertEquals(3, gobo3)
    }

    @Test
    fun goboWrapsAroundSlotCount() {
        val effect = GoboBeatSyncEffect()
        val params = EffectParams()
            .with("slotCount", 3)
            .with("changeOnBeat", true)

        // At 120 BPM, beat 3 should wrap to slot 0
        val beat = BeatState(bpm = 120f, beatPhase = 0f, barPhase = 0.75f, elapsed = 1.5f)
        val gobo = getGobo(effect, params, beat)
        assertEquals(0, gobo) // 3 % 3 = 0
    }

    @Test
    fun goboChangeOnBarUsesBarTiming() {
        val effect = GoboBeatSyncEffect()
        val params = EffectParams()
            .with("slotCount", 4)
            .with("changeOnBeat", false) // change on bar

        // At 120 BPM, 1 bar = 4 beats = 2.0 seconds
        val bar0 = BeatState(bpm = 120f, beatPhase = 0f, barPhase = 0f, elapsed = 0f)
        val bar1 = BeatState(bpm = 120f, beatPhase = 0f, barPhase = 0f, elapsed = 2.0f)
        val bar2 = BeatState(bpm = 120f, beatPhase = 0f, barPhase = 0f, elapsed = 4.0f)

        val gobo0 = getGobo(effect, params, bar0)
        val gobo1 = getGobo(effect, params, bar1)
        val gobo2 = getGobo(effect, params, bar2)

        assertEquals(0, gobo0)
        assertEquals(1, gobo1)
        assertEquals(2, gobo2)
    }

    @Test
    fun goboIsPositionIndependent() {
        val effect = GoboBeatSyncEffect()
        val params = EffectParams()
            .with("slotCount", 8)
            .with("changeOnBeat", true)

        val beat = BeatState(bpm = 120f, beatPhase = 0.5f, barPhase = 0.125f, elapsed = 0.5f)

        val ctx = effect.prepare(params, 0f, beat)
        val r1 = effect.computeMovement(Vec3(0f, 0f, 0f), ctx)
        val r2 = effect.computeMovement(Vec3(5f, 3f, -1f), ctx)

        assertEquals(r1.gobo, r2.gobo)
    }

    @Test
    fun goboDefaultSlotCountIs8() {
        val effect = GoboBeatSyncEffect()
        val params = EffectParams() // no slotCount specified

        val beat = BeatState(bpm = 120f, beatPhase = 0f, barPhase = 0f, elapsed = 0f)
        val gobo = getGobo(effect, params, beat)

        assertTrue(gobo in 0 until 8)
    }

    @Test
    fun goboDoesNotSetColorOrMovement() {
        val effect = GoboBeatSyncEffect()
        val params = EffectParams()
            .with("slotCount", 4)

        val beat = BeatState(bpm = 120f, beatPhase = 0f, barPhase = 0f, elapsed = 0f)
        val ctx = effect.prepare(params, 0f, beat)
        val result = effect.computeMovement(origin, ctx)

        // Only gobo should be set
        assertNotNull(result.gobo)
        assertEquals(null, result.pan)
        assertEquals(null, result.tilt)
        assertEquals(null, result.focus)
        assertEquals(null, result.zoom)
        assertEquals(null, result.strobeRate)
    }

    private fun getGobo(
        effect: GoboBeatSyncEffect,
        params: EffectParams,
        beat: BeatState
    ): Int {
        val ctx = effect.prepare(params, beat.elapsed, beat)
        return effect.computeMovement(origin, ctx).gobo!!
    }
}
