package com.chromadmx.ui.navigation

/**
 * Top-level app navigation state.
 *
 * The app is either:
 * - In the onboarding flow (first launch)
 * - On the main stage preview screen
 * - Viewing settings (overlay)
 * - Selecting a rig preset (from onboarding or settings)
 *
 * Chat panel and mascot are overlays managed independently.
 */
sealed class AppState {
    /**
     * First-launch onboarding flow. Step state is managed by
     * OnboardingViewModel, not the navigation state.
     */
    data object Onboarding : AppState()

    data object StagePreview : AppState()
    data object Settings : AppState()

    /**
     * Rig preset selection screen.
     * @param returnToOnboarding If true, confirming returns to the onboarding flow
     *                           (FixtureScan step). Otherwise returns to settings.
     */
    data class RigSelection(val returnToOnboarding: Boolean = false) : AppState()

    /** BLE provisioning screen for configuring ESP32 DMX nodes. */
    data object BleProvisioning : AppState()
}
