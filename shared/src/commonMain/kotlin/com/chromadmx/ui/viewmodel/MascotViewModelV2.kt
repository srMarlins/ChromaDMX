package com.chromadmx.ui.viewmodel

import com.chromadmx.agent.LightingAgent
import com.chromadmx.core.model.KnownNode
import com.chromadmx.networking.discovery.currentTimeMillis
import com.chromadmx.tempo.clock.BeatClock
import com.chromadmx.ui.mascot.AnimationController
import com.chromadmx.ui.mascot.BubbleType
import com.chromadmx.ui.mascot.MascotState
import com.chromadmx.ui.state.ChatMessage
import com.chromadmx.ui.state.MascotAnimState
import com.chromadmx.ui.state.MascotEvent
import com.chromadmx.ui.state.MascotUiState
import com.chromadmx.ui.state.SpeechBubble
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Rewritten MascotViewModel following the UDF (Unidirectional Data Flow) pattern.
 *
 * Exposes a single [state] flow and accepts events through [onEvent].
 * Integrates chat functionality (absorbing AgentViewModel responsibilities)
 * and reacts to BeatClock and network topology changes.
 *
 * @param beatClock Clock for beat-reactive dancing animation.
 * @param knownNodesFlow Flow of known nodes, typically from [NetworkStateRepository.knownNodes()].
 * @param lightingAgent Optional AI agent for chat integration. Null when no API key is configured.
 * @param scope CoroutineScope for async operations.
 */
class MascotViewModelV2(
    private val beatClock: BeatClock,
    private val knownNodesFlow: Flow<List<KnownNode>>,
    private val lightingAgent: LightingAgent? = null,
    private val scope: CoroutineScope,
) {
    internal val animationController = AnimationController(scope)

    private val _state = MutableStateFlow(MascotUiState())
    val state: StateFlow<MascotUiState> = _state.asStateFlow()

    private var autoDismissJob: Job? = null
    private var idleTimerJob: Job? = null

    /** Whether the dancing state was auto-triggered by BeatClock (vs. manual). */
    private var beatDriven = false

    /** Previous node keys for topology change detection. */
    private var lastKnownNodes: List<KnownNode> = emptyList()

    /** Simple counter for generating chat message IDs. */
    private var messageCounter = 0L

    companion object {
        /** How long (ms) the mascot must be idle before showing a proactive tip. */
        internal const val IDLE_TIMEOUT_MS = 30_000L

        internal val IDLE_TIPS = listOf(
            "Tap me to chat with the AI!",
            "Try generating some scenes for tonight",
            "Swipe down on the preset strip for more options",
        )
    }

    init {
        animationController.start()
        startBeatSync()
        startStateSync()
        startNodeSync()
        resetIdleTimer()
    }

    fun onEvent(event: MascotEvent) {
        when (event) {
            is MascotEvent.ShowBubble -> showBubble(event.bubble)
            is MascotEvent.DismissBubble -> dismissBubble()
            is MascotEvent.OnBubbleAction -> onBubbleAction(event.actionId)
            is MascotEvent.TriggerHappy -> triggerHappy()
            is MascotEvent.TriggerAlert -> triggerAlert(event.message)
            is MascotEvent.TriggerThinking -> triggerThinking()
            is MascotEvent.TriggerConfused -> triggerConfused(event.message)
            is MascotEvent.TriggerDancing -> triggerDancing()
            is MascotEvent.ReturnToIdle -> returnToIdle()
            is MascotEvent.ToggleChat -> toggleChat()
            is MascotEvent.SendChatMessage -> sendChatMessage(event.text)
        }
    }

    // ── State sync ──────────────────────────────────────────────────

    /**
     * Keep animState in sync with the animation controller's state.
     * The controller can autonomously transition back to IDLE when a
     * non-looping animation completes.
     */
    private fun startStateSync() {
        scope.launch {
            animationController.currentState.collect { controllerState ->
                val animState = controllerState.toAnimState()
                _state.update { it.copy(animState = animState) }
            }
        }
        // Also sync the frame index
        scope.launch {
            animationController.currentFrameIndex.collect { frameIndex ->
                _state.update { it.copy(currentFrameIndex = frameIndex) }
            }
        }
    }

    // ── Node-reactive sync ──────────────────────────────────────────

    /**
     * Subscribe to known node updates.
     * Detects new nodes and disconnections to trigger mascot alerts.
     */
    private fun startNodeSync() {
        scope.launch {
            knownNodesFlow.collect { currentNodes ->
                detectNodeChanges(currentNodes)
                lastKnownNodes = currentNodes
            }
        }
    }

    private fun detectNodeChanges(currentNodes: List<KnownNode>) {
        if (lastKnownNodes.isEmpty() && currentNodes.isNotEmpty()) {
            // First time we see nodes — don't spam
            return
        }

        val lastKeys = lastKnownNodes.map { it.nodeKey }.toSet()
        val currentKeys = currentNodes.map { it.nodeKey }.toSet()

        // 1. Detect new nodes
        val newNodeKeys = currentKeys - lastKeys
        if (newNodeKeys.isNotEmpty()) {
            triggerHappy()
            showBubble(SpeechBubble(
                text = "New node found! Auto-configuring...",
                type = BubbleType.INFO
            ))
            return
        }

        // 2. Detect disconnected nodes
        val lostNodeKeys = lastKeys - currentKeys
        if (lostNodeKeys.isNotEmpty()) {
            val lostNode = lastKnownNodes.firstOrNull { it.nodeKey in lostNodeKeys }
            val nodeName = lostNode?.shortName ?: "Node"
            triggerAlert("$nodeName disconnected — diagnose?")
            // Update bubble to have an action
            _state.update { state ->
                state.copy(
                    currentBubble = state.currentBubble?.copy(
                        actionLabel = "DIAGNOSE",
                        actionId = "diagnose_connection",
                        type = BubbleType.ACTION
                    )
                )
            }
            return
        }
    }

    // ── Beat-reactive sync ──────────────────────────────────────────

    /**
     * Subscribe to beat clock running state.
     * When isRunning becomes true (and BPM > 0), auto-trigger DANCING.
     * When isRunning becomes false, return to IDLE (only if beat-driven).
     */
    private fun startBeatSync() {
        scope.launch {
            beatClock.isRunning
                .map { it && beatClock.bpm.value > 0f }
                .distinctUntilChanged()
                .collect { shouldDance ->
                    if (shouldDance) {
                        beatDriven = true
                        triggerDancing()
                    } else if (beatDriven) {
                        beatDriven = false
                        returnToIdle()
                    }
                }
        }
    }

    // ── Proactive idle timer ────────────────────────────────────────

    /**
     * Reset (or start) the idle timer. After [IDLE_TIMEOUT_MS] of no state
     * changes or bubble activity, shows a random tip bubble.
     */
    internal fun resetIdleTimer() {
        idleTimerJob?.cancel()
        idleTimerJob = scope.launch {
            delay(IDLE_TIMEOUT_MS)
            if (isActive) {
                val tip = IDLE_TIPS.random()
                showBubble(SpeechBubble(text = tip, type = BubbleType.INFO))
            }
        }
    }

    // ── Chat integration ────────────────────────────────────────────

    private fun toggleChat() {
        _state.update { it.copy(isChatOpen = !it.isChatOpen) }
        resetIdleTimer()
    }

    private fun sendChatMessage(text: String) {
        val userMsg = ChatMessage(
            id = "user-${nextMessageId()}",
            text = text,
            isFromUser = true,
            timestampMs = currentTimeMillis()
        )
        _state.update { it.copy(chatHistory = it.chatHistory + userMsg) }
        triggerThinking()

        scope.launch {
            val responseText = if (lightingAgent != null && lightingAgent.isAvailable) {
                lightingAgent.send(text)
            } else {
                // Simulated fallback response when no agent is configured
                delay(500)
                "I'll help you with that! (Agent not configured — set an API key in Settings.)"
            }

            val response = ChatMessage(
                id = "agent-${nextMessageId()}",
                text = responseText,
                isFromUser = false,
                timestampMs = currentTimeMillis()
            )
            _state.update { it.copy(chatHistory = it.chatHistory + response) }
            returnToIdle()
        }
    }

    private fun nextMessageId(): Long = messageCounter++

    // ── State triggers ──────────────────────────────────────────────

    private fun showBubble(bubble: SpeechBubble) {
        _state.update { it.copy(currentBubble = bubble) }
        resetIdleTimer()
        autoDismissJob?.cancel()
        if (bubble.autoDismissMs > 0) {
            autoDismissJob = scope.launch {
                delay(bubble.autoDismissMs)
                _state.update { it.copy(currentBubble = null) }
            }
        }
    }

    private fun dismissBubble() {
        autoDismissJob?.cancel()
        _state.update { it.copy(currentBubble = null) }
        resetIdleTimer()
    }

    /**
     * Handle a speech bubble action by its [actionId].
     * After handling, the bubble is dismissed automatically.
     */
    private fun onBubbleAction(actionId: String?) {
        when (actionId) {
            "diagnose_connection" -> {
                triggerThinking()
                showBubble(SpeechBubble(text = "Analyzing network...", autoDismissMs = 2000))
            }
            else -> { /* unknown action — no-op */ }
        }
        dismissBubble()
    }

    private fun triggerHappy() {
        setAnimState(MascotAnimState.HAPPY)
        resetIdleTimer()
    }

    private fun triggerAlert(message: String) {
        setAnimState(MascotAnimState.ALERT)
        showBubble(SpeechBubble(text = message, type = BubbleType.ALERT))
    }

    private fun triggerThinking() {
        setAnimState(MascotAnimState.THINKING)
        resetIdleTimer()
    }

    private fun triggerConfused(message: String) {
        setAnimState(MascotAnimState.CONFUSED)
        showBubble(SpeechBubble(text = message, type = BubbleType.INFO))
    }

    private fun triggerDancing() {
        setAnimState(MascotAnimState.DANCING)
        resetIdleTimer()
    }

    private fun returnToIdle() {
        setAnimState(MascotAnimState.IDLE)
        resetIdleTimer()
    }

    /**
     * Update the animation state in both the UI state and the animation controller.
     */
    private fun setAnimState(animState: MascotAnimState) {
        _state.update { it.copy(animState = animState) }
        animationController.transitionTo(animState.toMascotState())
    }

    fun onCleared() {
        animationController.stop()
        autoDismissJob?.cancel()
        idleTimerJob?.cancel()
    }
}

// ── Extension mappings between MascotAnimState and MascotState ──────

/**
 * Convert [MascotAnimState] (contract) to [MascotState] (mascot package).
 */
internal fun MascotAnimState.toMascotState(): MascotState = when (this) {
    MascotAnimState.IDLE -> MascotState.IDLE
    MascotAnimState.THINKING -> MascotState.THINKING
    MascotAnimState.HAPPY -> MascotState.HAPPY
    MascotAnimState.ALERT -> MascotState.ALERT
    MascotAnimState.CONFUSED -> MascotState.CONFUSED
    MascotAnimState.DANCING -> MascotState.DANCING
}

/**
 * Convert [MascotState] (mascot package) to [MascotAnimState] (contract).
 */
internal fun MascotState.toAnimState(): MascotAnimState = when (this) {
    MascotState.IDLE -> MascotAnimState.IDLE
    MascotState.THINKING -> MascotAnimState.THINKING
    MascotState.HAPPY -> MascotAnimState.HAPPY
    MascotState.ALERT -> MascotAnimState.ALERT
    MascotState.CONFUSED -> MascotAnimState.CONFUSED
    MascotState.DANCING -> MascotAnimState.DANCING
}
