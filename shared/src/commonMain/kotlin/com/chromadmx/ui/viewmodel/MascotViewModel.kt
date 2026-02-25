package com.chromadmx.ui.viewmodel

import com.chromadmx.tempo.clock.BeatClock
import com.chromadmx.ui.mascot.AnimationController
import com.chromadmx.ui.mascot.BubbleType
import com.chromadmx.ui.mascot.MascotState
import com.chromadmx.ui.mascot.SpeechBubble
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * ViewModel managing the pixel mascot's state, animations, and speech bubbles.
 *
 * Integrates with [BeatClock] to sync mascot animations to the musical beat:
 * - When the clock is running and BPM > 0, the mascot auto-transitions to DANCING.
 * - When the clock stops, the mascot returns to IDLE.
 */
class MascotViewModel(
    private val scope: CoroutineScope,
    private val beatClock: BeatClock,
) {
    internal val animationController = AnimationController(scope)

    private val _mascotState = MutableStateFlow(MascotState.IDLE)
    val mascotState: StateFlow<MascotState> = _mascotState.asStateFlow()

    /** Encapsulated current frame index — read from the animation controller. */
    val currentFrameIndex: StateFlow<Int> = animationController.currentFrameIndex

    private val _currentBubble = MutableStateFlow<SpeechBubble?>(null)
    val currentBubble: StateFlow<SpeechBubble?> = _currentBubble.asStateFlow()

    /** Whether the chat panel is open. */
    private val _isChatOpen = MutableStateFlow(false)
    val isChatOpen: StateFlow<Boolean> = _isChatOpen.asStateFlow()

    private var autoDismissJob: Job? = null
    private var beatSyncJob: Job? = null

    /** Whether the dancing state was auto-triggered by BeatClock (vs. manual). */
    private var beatDriven = false

    init {
        animationController.start()
        startBeatSync()
    }

    // ── Beat-reactive sync ──────────────────────────────────────────

    /**
     * Subscribe to beat clock running state.
     * When isRunning becomes true (and BPM > 0), auto-trigger DANCING.
     * When isRunning becomes false, return to IDLE (only if beat-driven).
     */
    private fun startBeatSync() {
        beatSyncJob = scope.launch {
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

    // ── State triggers ──────────────────────────────────────────────

    fun showBubble(bubble: SpeechBubble) {
        _currentBubble.value = bubble
        autoDismissJob?.cancel()
        if (bubble.autoDismissMs > 0) {
            autoDismissJob = scope.launch {
                delay(bubble.autoDismissMs)
                _currentBubble.value = null
            }
        }
    }

    fun dismissBubble() {
        autoDismissJob?.cancel()
        _currentBubble.value = null
    }

    fun triggerHappy() {
        _mascotState.value = MascotState.HAPPY
        animationController.transitionTo(MascotState.HAPPY)
    }

    fun triggerAlert(message: String) {
        _mascotState.value = MascotState.ALERT
        animationController.transitionTo(MascotState.ALERT)
        showBubble(SpeechBubble(text = message, type = BubbleType.ALERT))
    }

    fun triggerThinking() {
        _mascotState.value = MascotState.THINKING
        animationController.transitionTo(MascotState.THINKING)
    }

    fun triggerConfused(message: String) {
        _mascotState.value = MascotState.CONFUSED
        animationController.transitionTo(MascotState.CONFUSED)
        showBubble(SpeechBubble(text = message, type = BubbleType.INFO))
    }

    /** Transition to DANCING state (beat-synced movement). */
    fun triggerDancing() {
        _mascotState.value = MascotState.DANCING
        animationController.transitionTo(MascotState.DANCING)
    }

    fun returnToIdle() {
        _mascotState.value = MascotState.IDLE
        animationController.transitionTo(MascotState.IDLE)
    }

    fun toggleChat() {
        _isChatOpen.value = !_isChatOpen.value
    }

    fun onCleared() {
        animationController.stop()
        autoDismissJob?.cancel()
        beatSyncJob?.cancel()
    }
}
