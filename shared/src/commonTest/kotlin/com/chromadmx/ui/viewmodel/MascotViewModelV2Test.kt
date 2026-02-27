package com.chromadmx.ui.viewmodel

import com.chromadmx.agent.LightingAgentInterface
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.KnownNode
import com.chromadmx.tempo.clock.BeatClock
import com.chromadmx.ui.mascot.BubbleType
import com.chromadmx.ui.state.MascotAnimState
import com.chromadmx.ui.state.MascotEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MascotViewModelV2Test {

    // ── Fakes ────────────────────────────────────────────────────────

    private fun fakeBeatClock() = object : BeatClock {
        override val beatState: StateFlow<BeatState> = MutableStateFlow(BeatState.IDLE)
        override val isRunning: MutableStateFlow<Boolean> = MutableStateFlow(false)
        override val bpm: MutableStateFlow<Float> = MutableStateFlow(120f)
        override val beatPhase: StateFlow<Float> = MutableStateFlow(0f)
        override val barPhase: StateFlow<Float> = MutableStateFlow(0f)
        override fun start() {}
        override fun stop() {}
    }

    /**
     * Creates a VM using the given beat clock and known-nodes flow.
     * LightingAgent is nullable; pass null for tests that don't exercise chat→agent.
     */
    private fun createVm(
        beatClock: BeatClock = fakeBeatClock(),
        knownNodesFlow: MutableStateFlow<List<KnownNode>> = MutableStateFlow(emptyList()),
        lightingAgent: LightingAgentInterface? = null,
        scope: kotlinx.coroutines.CoroutineScope,
    ): MascotViewModelV2 = MascotViewModelV2(
        beatClock = beatClock,
        knownNodesFlow = knownNodesFlow,
        lightingAgent = lightingAgent,
        scope = scope,
    )

    // ── 1. Toggle chat ──────────────────────────────────────────────

    @Test
    fun toggleChatFlipsChatOpen() = runTest {
        val vm = createVm(scope = backgroundScope)

        assertFalse(vm.state.value.isChatOpen)

        vm.onEvent(MascotEvent.ToggleChat)
        assertTrue(vm.state.value.isChatOpen)

        vm.onEvent(MascotEvent.ToggleChat)
        assertFalse(vm.state.value.isChatOpen)
    }

    // ── 2. Send message adds to chat history ────────────────────────

    @Test
    fun sendMessageAddsUserMessageToChatHistory() = runTest {
        val vm = createVm(scope = backgroundScope)

        vm.onEvent(MascotEvent.SendChatMessage("hello"))
        advanceTimeBy(50)

        val history = vm.state.value.chatHistory
        assertTrue(history.isNotEmpty(), "Chat history should contain at least the user message")
        assertEquals("hello", history.first().text)
        assertTrue(history.first().isFromUser)
    }

    // ── 3. Agent response appears in chat ───────────────────────────

    @Test
    fun agentResponseAppearsInChatWhenNoAgent() = runTest {
        val vm = createVm(scope = backgroundScope)

        vm.onEvent(MascotEvent.SendChatMessage("hello"))
        // Allow simulated response delay to complete
        advanceTimeBy(600)

        val history = vm.state.value.chatHistory
        assertTrue(history.size >= 2, "Expected user + agent response, got ${history.size}")
        assertFalse(history.last().isFromUser, "Last message should be from agent")
    }

    // ── 4. Network topology change triggers alert ───────────────────

    @Test
    fun newNodeTriggersHappyBubble() = runTest {
        val nodesFlow = MutableStateFlow<List<KnownNode>>(emptyList())
        val vm = createVm(knownNodesFlow = nodesFlow, scope = backgroundScope)
        advanceTimeBy(50)

        // First non-empty emission is suppressed (initial discovery)
        val firstNode = KnownNode(
            nodeKey = "node-1",
            ipAddress = "192.168.1.10",
            shortName = "Node 1",
            longName = "DMX Node 1",
            lastSeenMs = 1000L
        )
        nodesFlow.value = listOf(firstNode)
        advanceTimeBy(50)

        // Second emission with a new node triggers alert
        val secondNode = KnownNode(
            nodeKey = "node-2",
            ipAddress = "192.168.1.11",
            shortName = "Node 2",
            longName = "DMX Node 2",
            lastSeenMs = 2000L
        )
        nodesFlow.value = listOf(firstNode, secondNode)
        advanceTimeBy(50)

        assertEquals(MascotAnimState.HAPPY, vm.state.value.animState)
        val bubble = vm.state.value.currentBubble
        assertNotNull(bubble, "Expected a bubble for new node")
        assertTrue(bubble.text.contains("New node"), "Bubble text should mention new node: ${bubble.text}")
    }

    @Test
    fun lostNodeTriggersAlertBubble() = runTest {
        val nodesFlow = MutableStateFlow<List<KnownNode>>(emptyList())
        val vm = createVm(knownNodesFlow = nodesFlow, scope = backgroundScope)
        advanceTimeBy(50)

        // Initial nodes
        val nodeA = KnownNode("node-a", "192.168.1.10", "Alpha", "Alpha Node", 1000L)
        val nodeB = KnownNode("node-b", "192.168.1.11", "Beta", "Beta Node", 1000L)
        nodesFlow.value = listOf(nodeA, nodeB)
        advanceTimeBy(50)

        // One node goes offline
        nodesFlow.value = listOf(nodeA)
        advanceTimeBy(50)

        assertEquals(MascotAnimState.ALERT, vm.state.value.animState)
        val bubble = vm.state.value.currentBubble
        assertNotNull(bubble, "Expected an alert bubble for lost node")
        assertTrue(bubble.text.contains("Beta"), "Bubble text should mention lost node name: ${bubble.text}")
    }

    // ── 5. Beat sync dancing ────────────────────────────────────────

    @Test
    fun beatClockRunningTriggersDancing() = runTest {
        val clock = fakeBeatClock()
        val vm = createVm(beatClock = clock, scope = backgroundScope)

        assertEquals(MascotAnimState.IDLE, vm.state.value.animState)

        clock.isRunning.value = true
        advanceTimeBy(50)

        assertEquals(MascotAnimState.DANCING, vm.state.value.animState)
    }

    @Test
    fun beatClockStopReturnsToIdle() = runTest {
        val clock = fakeBeatClock()
        val vm = createVm(beatClock = clock, scope = backgroundScope)

        clock.isRunning.value = true
        advanceTimeBy(50)
        assertEquals(MascotAnimState.DANCING, vm.state.value.animState)

        clock.isRunning.value = false
        advanceTimeBy(50)
        assertEquals(MascotAnimState.IDLE, vm.state.value.animState)
    }

    // ── 6. Idle timer tips ──────────────────────────────────────────

    @Test
    fun idleTimerShowsTipAfterTimeout() = runTest {
        val vm = createVm(scope = backgroundScope)

        assertNull(vm.state.value.currentBubble)

        advanceTimeBy(MascotViewModelV2.IDLE_TIMEOUT_MS + 100)

        val bubble = vm.state.value.currentBubble
        assertNotNull(bubble, "Expected a tip bubble after idle timeout")
        assertEquals(BubbleType.INFO, bubble.type)
        assertTrue(
            bubble.text in MascotViewModelV2.IDLE_TIPS,
            "Expected tip from IDLE_TIPS, got: '${bubble.text}'"
        )
    }

    @Test
    fun idleTimerResetsOnEvent() = runTest {
        val vm = createVm(scope = backgroundScope)

        // Advance partway (20s of 30s)
        advanceTimeBy(20_000L)

        // Trigger an event — resets the idle timer
        vm.onEvent(MascotEvent.TriggerHappy)

        // Advance another 20s — should NOT show tip yet (only 20s since reset)
        advanceTimeBy(20_000L)
        assertNull(vm.state.value.currentBubble)

        // Advance past the full idle timeout from the reset (10.1s more)
        advanceTimeBy(10_100L)

        val bubble = vm.state.value.currentBubble
        assertNotNull(bubble, "Expected a tip bubble after full idle period")
        assertEquals(BubbleType.INFO, bubble.type)
    }

    // ── Additional event coverage ───────────────────────────────────

    @Test
    fun startsInIdleState() = runTest {
        val vm = createVm(scope = backgroundScope)
        assertEquals(MascotAnimState.IDLE, vm.state.value.animState)
    }

    @Test
    fun showBubbleSetsBubbleInState() = runTest {
        val vm = createVm(scope = backgroundScope)
        val bubble = com.chromadmx.ui.state.SpeechBubble("Hello!", BubbleType.INFO)
        vm.onEvent(MascotEvent.ShowBubble(bubble))
        assertEquals(bubble, vm.state.value.currentBubble)
    }

    @Test
    fun dismissBubbleClearsBubble() = runTest {
        val vm = createVm(scope = backgroundScope)
        vm.onEvent(MascotEvent.ShowBubble(com.chromadmx.ui.state.SpeechBubble("Hello!", BubbleType.INFO)))
        vm.onEvent(MascotEvent.DismissBubble)
        assertNull(vm.state.value.currentBubble)
    }

    @Test
    fun triggerAlertSetsAlertStateAndBubble() = runTest {
        val vm = createVm(scope = backgroundScope)
        vm.onEvent(MascotEvent.TriggerAlert("Node down!"))
        assertEquals(MascotAnimState.ALERT, vm.state.value.animState)
        assertEquals("Node down!", vm.state.value.currentBubble?.text)
        assertEquals(BubbleType.ALERT, vm.state.value.currentBubble?.type)
    }

    @Test
    fun triggerThinkingSetsState() = runTest {
        val vm = createVm(scope = backgroundScope)
        vm.onEvent(MascotEvent.TriggerThinking)
        assertEquals(MascotAnimState.THINKING, vm.state.value.animState)
    }

    @Test
    fun triggerConfusedSetsStateAndBubble() = runTest {
        val vm = createVm(scope = backgroundScope)
        vm.onEvent(MascotEvent.TriggerConfused("What happened?"))
        assertEquals(MascotAnimState.CONFUSED, vm.state.value.animState)
        assertEquals("What happened?", vm.state.value.currentBubble?.text)
    }

    @Test
    fun triggerDancingSetsState() = runTest {
        val vm = createVm(scope = backgroundScope)
        vm.onEvent(MascotEvent.TriggerDancing)
        assertEquals(MascotAnimState.DANCING, vm.state.value.animState)
    }

    @Test
    fun returnToIdleSetsIdleState() = runTest {
        val vm = createVm(scope = backgroundScope)
        vm.onEvent(MascotEvent.TriggerDancing)
        vm.onEvent(MascotEvent.ReturnToIdle)
        assertEquals(MascotAnimState.IDLE, vm.state.value.animState)
    }

    @Test
    fun autoDismissBubbleAfterTimeout() = runTest {
        val vm = createVm(scope = backgroundScope)
        vm.onEvent(MascotEvent.ShowBubble(
            com.chromadmx.ui.state.SpeechBubble("Temp", BubbleType.INFO, autoDismissMs = 1000L)
        ))
        assertNotNull(vm.state.value.currentBubble)
        advanceTimeBy(1100L)
        assertNull(vm.state.value.currentBubble)
    }

    @Test
    fun onBubbleActionDiagnoseTriggersThinking() = runTest {
        val vm = createVm(scope = backgroundScope)
        vm.onEvent(MascotEvent.ShowBubble(
            com.chromadmx.ui.state.SpeechBubble(
                "Node offline",
                BubbleType.ACTION,
                actionLabel = "DIAGNOSE",
                actionId = "diagnose_connection"
            )
        ))
        vm.onEvent(MascotEvent.OnBubbleAction("diagnose_connection"))
        // After bubble action, thinking state is triggered and a new bubble is shown
        assertEquals(MascotAnimState.THINKING, vm.state.value.animState)
        assertNotNull(vm.state.value.currentBubble)
        assertEquals("Analyzing network...", vm.state.value.currentBubble?.text)
        // The bubble auto-dismisses after 2000ms
        advanceTimeBy(2100L)
        assertNull(vm.state.value.currentBubble)
    }

    @Test
    fun currentFrameIndexExposedInState() = runTest {
        val vm = createVm(scope = backgroundScope)
        assertEquals(0, vm.state.value.currentFrameIndex)
    }
}
