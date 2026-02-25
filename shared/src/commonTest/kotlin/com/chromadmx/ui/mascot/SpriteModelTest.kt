package com.chromadmx.ui.mascot

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpriteModelTest {
    @Test
    fun mascotStateHasAllSixStates() {
        val states = MascotState.entries
        assertEquals(6, states.size)
        assertTrue(states.contains(MascotState.IDLE))
        assertTrue(states.contains(MascotState.THINKING))
        assertTrue(states.contains(MascotState.HAPPY))
        assertTrue(states.contains(MascotState.ALERT))
        assertTrue(states.contains(MascotState.CONFUSED))
        assertTrue(states.contains(MascotState.DANCING))
    }

    @Test
    fun spriteFrameHasCorrectDimensions() {
        val frame = SpriteFrame(
            pixels = Array(16) { IntArray(16) { 0 } }
        )
        assertEquals(16, frame.pixels.size)
        assertEquals(16, frame.pixels[0].size)
    }

    @Test
    fun animationSequenceReturnsFrameByIndex() {
        val frame0 = SpriteFrame(Array(16) { IntArray(16) { 0 } })
        val frame1 = SpriteFrame(Array(16) { IntArray(16) { 1 } })
        val anim = AnimationSequence(
            state = MascotState.IDLE,
            frames = listOf(frame0, frame1),
            frameDurationMs = 200L,
            loop = true
        )
        assertEquals(frame0, anim.frameAt(0))
        assertEquals(frame1, anim.frameAt(1))
        assertEquals(frame0, anim.frameAt(2)) // loops
    }

    @Test
    fun nonLoopingAnimationClampsToLastFrame() {
        val frame0 = SpriteFrame(Array(16) { IntArray(16) { 0 } })
        val frame1 = SpriteFrame(Array(16) { IntArray(16) { 1 } })
        val anim = AnimationSequence(
            state = MascotState.HAPPY,
            frames = listOf(frame0, frame1),
            frameDurationMs = 200L,
            loop = false
        )
        assertEquals(frame1, anim.frameAt(5)) // clamps to last
    }
}
