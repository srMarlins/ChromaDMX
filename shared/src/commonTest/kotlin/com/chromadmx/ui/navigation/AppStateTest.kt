package com.chromadmx.ui.navigation

import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppStateTest {
    @Test
    fun onboardingIsInitialStateForFirstLaunch() {
        val state: AppState = AppState.Onboarding(OnboardingStep.SPLASH)
        assertIs<AppState.Onboarding>(state)
    }

    @Test
    fun stagePreviewIsDefaultState() {
        val state: AppState = AppState.StagePreview
        assertIs<AppState.StagePreview>(state)
    }

    @Test
    fun settingsIsOverlayState() {
        val state: AppState = AppState.Settings
        assertIs<AppState.Settings>(state)
    }

    @Test
    fun onboardingStepsAreOrdered() {
        val steps = OnboardingStep.entries
        assertTrue(steps.indexOf(OnboardingStep.SPLASH) < steps.indexOf(OnboardingStep.NETWORK_DISCOVERY))
        assertTrue(steps.indexOf(OnboardingStep.NETWORK_DISCOVERY) < steps.indexOf(OnboardingStep.FIXTURE_SCAN))
        assertTrue(steps.indexOf(OnboardingStep.FIXTURE_SCAN) < steps.indexOf(OnboardingStep.VIBE_CHECK))
    }
}
