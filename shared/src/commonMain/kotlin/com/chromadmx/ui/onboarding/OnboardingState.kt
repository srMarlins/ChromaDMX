package com.chromadmx.ui.onboarding

/**
 * Steps in the first-launch onboarding flow.
 *
 * Modeled as a sealed class so each step can carry state (e.g. discovered
 * node count, selected genre) without external maps.
 *
 * The linear flow is:
 *   Splash -> NetworkDiscovery -> FixtureScan -> VibeCheck -> StagePreview -> Complete
 */
sealed class OnboardingStep {

    /** Animated logo splash -- auto-advances after 2.5s. */
    data object Splash : OnboardingStep()

    /** Network scan for Art-Net nodes. Offers simulation fallback. */
    data object NetworkDiscovery : OnboardingStep()

    /** Fixture scan via camera (real) or simulated rig pop-in. */
    data object FixtureScan : OnboardingStep()

    /** Genre / mood selector -- drives pre-generation of scenes. */
    data object VibeCheck : OnboardingStep()

    /** Brief stage preview before entering the main app. Auto-advances. */
    data object StagePreview : OnboardingStep()

    /** Terminal state -- onboarding is done; transition to main app. */
    data object Complete : OnboardingStep()

    companion object {
        /** Ordered list of steps for linear advancement. */
        val steps: List<OnboardingStep>
            get() = listOf(
                Splash,
                NetworkDiscovery,
                FixtureScan,
                VibeCheck,
                StagePreview,
                Complete,
            )
    }
}
