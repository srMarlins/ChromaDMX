package com.chromadmx.ui.navigation

import com.chromadmx.ui.onboarding.OnboardingStep
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AppStateTest {
    @Test
    fun onboardingIsInitialStateForFirstLaunch() {
        val state: AppState = AppState.Onboarding
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
        val steps = OnboardingStep.steps
        assertTrue(steps.indexOf(OnboardingStep.Splash) < steps.indexOf(OnboardingStep.NetworkDiscovery))
        assertTrue(steps.indexOf(OnboardingStep.NetworkDiscovery) < steps.indexOf(OnboardingStep.FixtureScan))
        assertTrue(steps.indexOf(OnboardingStep.FixtureScan) < steps.indexOf(OnboardingStep.VibeCheck))
        assertTrue(steps.indexOf(OnboardingStep.VibeCheck) < steps.indexOf(OnboardingStep.Complete))
    }
}
