package com.chromadmx.ui.viewmodel

import com.chromadmx.core.model.BeatState
import com.chromadmx.tempo.clock.BeatClock
import com.chromadmx.ui.mascot.BubbleType
import com.chromadmx.ui.mascot.MascotState
import com.chromadmx.ui.mascot.SpeechBubble
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class MascotViewModelTest {

    private fun stubBeatClock() = object : BeatClock {
        override val beatState: StateFlow<BeatState> = MutableStateFlow(BeatState.IDLE)
        override val isRunning: MutableStateFlow<Boolean> = MutableStateFlow(false)
        override val bpm: MutableStateFlow<Float> = MutableStateFlow(120f)
        override val beatPhase: StateFlow<Float> = MutableStateFlow(0f)
        override val barPhase: StateFlow<Float> = MutableStateFlow(0f)
        override fun start() {}
        override fun stop() {}
    }

    @Test
    fun startsInIdleState() = runTest {
        val vm = MascotViewModel(scope = backgroundScope, beatClock = stubBeatClock())
        assertEquals(MascotState.IDLE, vm.mascotState.first())
    }

    @Test
    fun showBubbleDisplaysBubble() = runTest {
        val vm = MascotViewModel(scope = backgroundScope, beatClock = stubBeatClock())
        val bubble = SpeechBubble("Hello!", BubbleType.INFO)
        vm.showBubble(bubble)
        assertEquals(bubble, vm.currentBubble.first())
    }

    @Test
    fun dismissBubbleClearsBubble() = runTest {
        val vm = MascotViewModel(scope = backgroundScope, beatClock = stubBeatClock())
        vm.showBubble(SpeechBubble("Hello!", BubbleType.INFO))
        vm.dismissBubble()
        assertNull(vm.currentBubble.first())
    }

    @Test
    fun triggerHappyState() = runTest {
        val vm = MascotViewModel(scope = backgroundScope, beatClock = stubBeatClock())
        vm.triggerHappy()
        assertEquals(MascotState.HAPPY, vm.mascotState.first())
    }

    @Test
    fun triggerAlert() = runTest {
        val vm = MascotViewModel(scope = backgroundScope, beatClock = stubBeatClock())
        vm.triggerAlert("Node disconnected!")
        assertEquals(MascotState.ALERT, vm.mascotState.first())
        assertEquals("Node disconnected!", vm.currentBubble.first()?.text)
    }

    @Test
    fun triggerThinking() = runTest {
        val vm = MascotViewModel(scope = backgroundScope, beatClock = stubBeatClock())
        vm.triggerThinking()
        assertEquals(MascotState.THINKING, vm.mascotState.first())
    }

    @Test
    fun autoDismissBubble() = runTest {
        val vm = MascotViewModel(scope = backgroundScope, beatClock = stubBeatClock())
        vm.showBubble(SpeechBubble("Temp", BubbleType.INFO, autoDismissMs = 1000L))
        advanceTimeBy(1100L)
        assertNull(vm.currentBubble.first())
    }

    // ── Beat-reactive tests ─────────────────────────────────────────

    @Test
    fun beatClockRunningTriggersDancing() = runTest {
        val clock = stubBeatClock()
        val vm = MascotViewModel(scope = backgroundScope, beatClock = clock)

        // Initially idle
        assertEquals(MascotState.IDLE, vm.mascotState.first())

        // Clock starts running → should auto-trigger DANCING
        clock.isRunning.value = true
        advanceTimeBy(50)

        assertEquals(MascotState.DANCING, vm.mascotState.first())
    }

    @Test
    fun beatClockStopReturnsToIdle() = runTest {
        val clock = stubBeatClock()
        val vm = MascotViewModel(scope = backgroundScope, beatClock = clock)

        // Start running → DANCING
        clock.isRunning.value = true
        advanceTimeBy(50)
        assertEquals(MascotState.DANCING, vm.mascotState.first())

        // Stop running → should return to IDLE
        clock.isRunning.value = false
        advanceTimeBy(50)
        assertEquals(MascotState.IDLE, vm.mascotState.first())
    }

    @Test
    fun triggerDancingMethod() = runTest {
        val vm = MascotViewModel(scope = backgroundScope, beatClock = stubBeatClock())
        vm.triggerDancing()
        assertEquals(MascotState.DANCING, vm.mascotState.first())
    }

    // ── Encapsulated frame index ────────────────────────────────────

    @Test
    fun currentFrameIndexExposedFromController() = runTest {
        val vm = MascotViewModel(scope = backgroundScope, beatClock = stubBeatClock())
        // Frame index starts at 0
        assertEquals(0, vm.currentFrameIndex.first())
    }
}
