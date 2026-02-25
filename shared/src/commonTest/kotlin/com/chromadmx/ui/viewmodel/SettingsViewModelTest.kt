package com.chromadmx.ui.viewmodel

import com.chromadmx.networking.discovery.NodeDiscovery
import com.chromadmx.networking.transport.PlatformUdpTransport
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsViewModelTest {

    private val transport = PlatformUdpTransport()
    private val nodeDiscovery = NodeDiscovery(transport)

    @Test
    fun simulationModeDefaultsToOff() = runTest {
        val vm = SettingsViewModel(nodeDiscovery = nodeDiscovery, scope = backgroundScope)
        assertFalse(vm.simulationEnabled.value)
    }

    @Test
    fun toggleSimulationMode() = runTest {
        val vm = SettingsViewModel(nodeDiscovery = nodeDiscovery, scope = backgroundScope)
        vm.toggleSimulation(true)
        assertTrue(vm.simulationEnabled.value)
    }
}
