package com.chromadmx.ui.navigation

import kotlin.test.Test
import kotlin.test.assertIs

class AppStateTest {
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
