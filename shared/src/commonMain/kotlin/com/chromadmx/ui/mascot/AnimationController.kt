package com.chromadmx.ui.mascot

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Controls mascot animation frame advancement and state transitions.
 *
 * Advances frames at the rate specified by the current [AnimationSequence].
 * Non-looping animations automatically return to IDLE when complete.
 */
class AnimationController(
    private val scope: CoroutineScope,
    initialState: MascotState = MascotState.IDLE
) {
    private val _currentState = MutableStateFlow(initialState)
    val currentState: StateFlow<MascotState> = _currentState.asStateFlow()

    private val _currentFrameIndex = MutableStateFlow(0)
    val currentFrameIndex: StateFlow<Int> = _currentFrameIndex.asStateFlow()

    private var animationJob: Job? = null
    private var currentAnimation: AnimationSequence = MascotSprites.animationFor(initialState)

    /**
     * Transition to a new animation state.
     * Resets frame index and starts the new animation.
     */
    fun transitionTo(state: MascotState) {
        _currentState.value = state
        _currentFrameIndex.value = 0
        currentAnimation = MascotSprites.animationFor(state)
        startAnimationLoop()
    }

    /**
     * Start the animation frame advancement loop.
     */
    fun start() {
        startAnimationLoop()
    }

    fun stop() {
        animationJob?.cancel()
        animationJob = null
    }

    private fun startAnimationLoop() {
        animationJob?.cancel()
        animationJob = scope.launch {
            var frameIndex = 0
            while (isActive) {
                _currentFrameIndex.value = frameIndex

                delay(currentAnimation.frameDurationMs)
                frameIndex++

                // Check if non-looping animation is complete
                if (!currentAnimation.loop && frameIndex >= currentAnimation.frames.size) {
                    // Return to idle
                    _currentState.value = MascotState.IDLE
                    _currentFrameIndex.value = 0
                    currentAnimation = MascotSprites.animationFor(MascotState.IDLE)
                    frameIndex = 0
                }

                // Wrap looping animations
                if (currentAnimation.loop) {
                    frameIndex = frameIndex % currentAnimation.frames.size
                }
            }
        }
    }
}
