package com.chromadmx.ui.viewmodel

import com.chromadmx.ui.mascot.BubbleType
import com.chromadmx.ui.mascot.MascotState
import com.chromadmx.ui.mascot.SpeechBubble
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class MascotViewModelTest {
    @Test
    fun startsInIdleState() = runTest {
        val vm = MascotViewModel(scope = backgroundScope)
        assertEquals(MascotState.IDLE, vm.mascotState.first())
    }

    @Test
    fun showBubbleDisplaysBubble() = runTest {
        val vm = MascotViewModel(scope = backgroundScope)
        val bubble = SpeechBubble("Hello!", BubbleType.INFO)
        vm.showBubble(bubble)
        assertEquals(bubble, vm.currentBubble.first())
    }

    @Test
    fun dismissBubbleClearsBubble() = runTest {
        val vm = MascotViewModel(scope = backgroundScope)
        vm.showBubble(SpeechBubble("Hello!", BubbleType.INFO))
        vm.dismissBubble()
        assertNull(vm.currentBubble.first())
    }

    @Test
    fun triggerHappyState() = runTest {
        val vm = MascotViewModel(scope = backgroundScope)
        vm.triggerHappy()
        assertEquals(MascotState.HAPPY, vm.mascotState.first())
    }

    @Test
    fun triggerAlert() = runTest {
        val vm = MascotViewModel(scope = backgroundScope)
        vm.triggerAlert("Node disconnected!")
        assertEquals(MascotState.ALERT, vm.mascotState.first())
        assertEquals("Node disconnected!", vm.currentBubble.first()?.text)
    }

    @Test
    fun triggerThinking() = runTest {
        val vm = MascotViewModel(scope = backgroundScope)
        vm.triggerThinking()
        assertEquals(MascotState.THINKING, vm.mascotState.first())
    }

    @Test
    fun autoDismissBubble() = runTest {
        val vm = MascotViewModel(scope = backgroundScope)
        vm.showBubble(SpeechBubble("Temp", BubbleType.INFO, autoDismissMs = 1000L))
        advanceTimeBy(1100L)
        assertNull(vm.currentBubble.first())
    }
}
