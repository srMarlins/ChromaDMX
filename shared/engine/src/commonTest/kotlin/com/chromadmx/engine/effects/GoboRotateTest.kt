package com.chromadmx.engine.effects

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GoboRotateTest {

    private val origin = Vec3.ZERO
    private val beat = BeatState.IDLE

    private fun getGobo(time: Float, params: EffectParams): Int {
        val effect = GoboRotateEffect()
        val ctx = effect.prepare(params, time, beat)
        return effect.computeMovement(origin, ctx).gobo!!
    }

    @Test
    fun goboRotateReturnsSlotIndex() {
        val params = EffectParams()
            .with("slotCount", 8)
            .with("speed", 0.5f)

        val gobo = getGobo(0f, params)
        assertNotNull(gobo)
        assertTrue(gobo in 0 until 8)
    }

    @Test
    fun goboRotateCyclesThroughSlots() {
        val params = EffectParams()
            .with("slotCount", 4)
            .with("speed", 1.0f) // 1 full rotation per second

        // At speed=1.0 and slotCount=4: each slot takes 0.25s
        val gobo0 = getGobo(0.0f, params)    // slot 0
        val gobo1 = getGobo(0.25f, params)   // slot 1
        val gobo2 = getGobo(0.50f, params)   // slot 2
        val gobo3 = getGobo(0.75f, params)   // slot 3

        assertEquals(0, gobo0)
        assertEquals(1, gobo1)
        assertEquals(2, gobo2)
        assertEquals(3, gobo3)
    }

    @Test
    fun goboRotateWrapsAround() {
        val params = EffectParams()
            .with("slotCount", 3)
            .with("speed", 1.0f)

        // After a full rotation, should wrap back to 0
        val gobo = getGobo(1.0f, params)
        assertEquals(0, gobo) // 3 slots at speed 1.0: time 1.0 = 3 slots total, 3 % 3 = 0
    }

    @Test
    fun goboRotateRespectsStartSlot() {
        val params = EffectParams()
            .with("slotCount", 8)
            .with("speed", 1.0f)
            .with("startSlot", 3)

        val gobo = getGobo(0.0f, params)
        assertEquals(3, gobo)
    }

    @Test
    fun goboRotateSlowerSpeed() {
        val params = EffectParams()
            .with("slotCount", 4)
            .with("speed", 0.5f) // Half speed: 2 seconds per full rotation

        val gobo0 = getGobo(0.0f, params) // slot 0
        val gobo1 = getGobo(0.5f, params) // slot 1 (0.5 * 0.5 * 4 = 1)
        val gobo2 = getGobo(1.0f, params) // slot 2

        assertEquals(0, gobo0)
        assertEquals(1, gobo1)
        assertEquals(2, gobo2)
    }

    @Test
    fun goboRotateDoesNotSetMovementChannels() {
        val effect = GoboRotateEffect()
        val params = EffectParams()
            .with("slotCount", 8)
            .with("speed", 0.5f)

        val ctx = effect.prepare(params, 0f, beat)
        val result = effect.computeMovement(origin, ctx)

        assertNotNull(result.gobo)
        assertNull(result.pan)
        assertNull(result.tilt)
        assertNull(result.focus)
        assertNull(result.zoom)
        assertNull(result.strobeRate)
    }

    @Test
    fun goboRotateIsPositionIndependent() {
        val effect = GoboRotateEffect()
        val params = EffectParams()
            .with("slotCount", 8)
            .with("speed", 1.0f)

        val ctx = effect.prepare(params, 0.5f, beat)
        val r1 = effect.computeMovement(Vec3(0f, 0f, 0f), ctx)
        val r2 = effect.computeMovement(Vec3(5f, 3f, -1f), ctx)

        assertEquals(r1.gobo, r2.gobo)
    }

    @Test
    fun goboRotateSlotAlwaysInRange() {
        val params = EffectParams()
            .with("slotCount", 5)
            .with("speed", 2.0f)

        for (i in 0..100) {
            val gobo = getGobo(i * 0.1f, params)
            assertTrue(gobo in 0 until 5, "Gobo slot out of range at time ${i * 0.1f}: $gobo")
        }
    }
}
