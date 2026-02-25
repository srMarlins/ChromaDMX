package com.chromadmx.ui.mascot

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AnimationControllerTest {
    @Test
    fun startsInIdleState() = runTest {
        val controller = AnimationController(scope = backgroundScope)
        assertEquals(MascotState.IDLE, controller.currentState.first())
    }

    @Test
    fun currentFrameIndexStartsAtZero() = runTest {
        val controller = AnimationController(scope = backgroundScope)
        assertEquals(0, controller.currentFrameIndex.first())
    }

    @Test
    fun transitionChangesState() = runTest {
        val controller = AnimationController(scope = backgroundScope)
        controller.transitionTo(MascotState.HAPPY)
        assertEquals(MascotState.HAPPY, controller.currentState.first())
    }

    @Test
    fun nonLoopingAnimationReturnsToIdle() = runTest {
        val controller = AnimationController(scope = backgroundScope)
        controller.transitionTo(MascotState.HAPPY)
        // Happy has 4 frames at 150ms = 600ms total
        advanceTimeBy(700L)
        assertEquals(MascotState.IDLE, controller.currentState.first())
    }

    @Test
    fun frameIndexAdvancesOverTime() = runTest {
        val controller = AnimationController(scope = backgroundScope)
        controller.start()
        // Idle has 500ms per frame
        advanceTimeBy(600L)
        val index = controller.currentFrameIndex.first()
        assertEquals(1, index)
    }
}
