package com.chromadmx.ui.navigation

import com.chromadmx.ui.onboarding.OnboardingStep
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AppStateTest {
    // ---- Legacy AppState tests (still valid while AppState.kt exists) ----

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

    // ---- New AppScreen tests ----

    @Test
    fun appScreenSetupExists() {
        val screen: AppScreen = AppScreen.Setup
        assertIs<AppScreen.Setup>(screen)
    }

    @Test
    fun appScreenStageExists() {
        val screen: AppScreen = AppScreen.Stage
        assertIs<AppScreen.Stage>(screen)
    }

    @Test
    fun appScreenSettingsExists() {
        val screen: AppScreen = AppScreen.Settings
        assertIs<AppScreen.Settings>(screen)
    }

    @Test
    fun appScreenProvisioningExists() {
        val screen: AppScreen = AppScreen.Provisioning
        assertIs<AppScreen.Provisioning>(screen)
    }
}
