package com.chromadmx.ui.navigation

/**
 * Top-level app navigation state.
 *
 * Replaces the old [Screen] enum. The app is either:
 * - In the onboarding flow (first launch)
 * - On the main stage preview screen
 * - Viewing settings (overlay)
 *
 * Chat panel and mascot are overlays managed independently.
 */
sealed class AppState {
    data class Onboarding(val step: OnboardingStep) : AppState()
    data object StagePreview : AppState()
    data object Settings : AppState()
}

/**
 * Steps in the first-launch onboarding flow.
 */
enum class OnboardingStep {
    SPLASH,
    NETWORK_DISCOVERY,
    FIXTURE_SCAN,
    VIBE_CHECK
}
