package com.chromadmx.ui.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages top-level app navigation state.
 *
 * @param isFirstLaunch True if this is the first app launch (show onboarding).
 */
class AppStateManager(isFirstLaunch: Boolean) {

    private val _currentState = MutableStateFlow<AppState>(
        if (isFirstLaunch) AppState.Onboarding(OnboardingStep.SPLASH)
        else AppState.StagePreview
    )
    val currentState: StateFlow<AppState> = _currentState.asStateFlow()

    private var previousState: AppState = AppState.StagePreview

    fun navigateTo(state: AppState) {
        previousState = _currentState.value
        _currentState.value = state
    }

    /**
     * Navigate back one level. Only supports single-level overlay depth
     * (e.g., StagePreview -> Settings -> back to StagePreview).
     */
    fun navigateBack() {
        _currentState.value = previousState
        previousState = AppState.StagePreview
    }

    /**
     * Advance to the next onboarding step.
     * If already at the last step, transitions to StagePreview.
     */
    fun advanceOnboarding() {
        val current = _currentState.value
        if (current !is AppState.Onboarding) return

        val steps = OnboardingStep.entries
        val currentIndex = steps.indexOf(current.step)
        if (currentIndex < steps.lastIndex) {
            _currentState.value = AppState.Onboarding(steps[currentIndex + 1])
        } else {
            _currentState.value = AppState.StagePreview
        }
    }
}
