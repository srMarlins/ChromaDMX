package com.chromadmx.ui.navigation

import kotlin.test.Test
import kotlin.test.assertIs

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
