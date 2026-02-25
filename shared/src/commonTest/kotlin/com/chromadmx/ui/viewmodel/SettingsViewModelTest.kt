package com.chromadmx.ui.viewmodel

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsViewModelTest {
    @Test
    fun simulationModeDefaultsToOff() = runTest {
        val vm = SettingsViewModel(scope = backgroundScope)
        assertFalse(vm.simulationEnabled.value)
    }

    @Test
    fun toggleSimulationMode() = runTest {
        val vm = SettingsViewModel(scope = backgroundScope)
        vm.setSimulationEnabled(true)
        assertTrue(vm.simulationEnabled.value)
    }
}
