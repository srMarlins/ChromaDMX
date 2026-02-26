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
        if (isFirstLaunch) AppState.Onboarding
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
     * Complete onboarding and transition to StagePreview.
     */
    fun completeOnboarding() {
        _currentState.value = AppState.StagePreview
    }
}
