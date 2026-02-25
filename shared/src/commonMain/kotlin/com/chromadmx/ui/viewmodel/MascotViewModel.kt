package com.chromadmx.ui.viewmodel

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
import kotlinx.coroutines.launch

/**
 * ViewModel managing the pixel mascot's state, animations, and speech bubbles.
 */
class MascotViewModel(
    private val scope: CoroutineScope,
) {
    val animationController = AnimationController(scope)

    private val _mascotState = MutableStateFlow(MascotState.IDLE)
    val mascotState: StateFlow<MascotState> = _mascotState.asStateFlow()

    private val _currentBubble = MutableStateFlow<SpeechBubble?>(null)
    val currentBubble: StateFlow<SpeechBubble?> = _currentBubble.asStateFlow()

    /** Whether the chat panel is open. */
    private val _isChatOpen = MutableStateFlow(false)
    val isChatOpen: StateFlow<Boolean> = _isChatOpen.asStateFlow()

    private var autoDismissJob: Job? = null

    init {
        animationController.start()
    }

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
    }
}
