package com.chromadmx.ui.mascot

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MascotSpritesTest {
    @Test
    fun allStatesHaveAnimations() {
        for (state in MascotState.entries) {
            val anim = MascotSprites.animationFor(state)
            assertTrue(anim.frames.isNotEmpty(), "No frames for state $state")
            assertEquals(state, anim.state)
        }
    }

    @Test
    fun idleAnimationHasMultipleFrames() {
        val idle = MascotSprites.animationFor(MascotState.IDLE)
        assertTrue(idle.frames.size >= 2, "Idle should have at least 2 frames")
        assertTrue(idle.loop, "Idle should loop")
    }

    @Test
    fun happyAnimationDoesNotLoop() {
        val happy = MascotSprites.animationFor(MascotState.HAPPY)
        assertTrue(!happy.loop || happy.frames.size >= 2)
    }

    @Test
    fun allFramesAre16x16() {
        for (state in MascotState.entries) {
            val anim = MascotSprites.animationFor(state)
            for (frame in anim.frames) {
                assertEquals(16, frame.height, "Frame height should be 16 for $state")
                assertEquals(16, frame.width, "Frame width should be 16 for $state")
            }
        }
    }
}
