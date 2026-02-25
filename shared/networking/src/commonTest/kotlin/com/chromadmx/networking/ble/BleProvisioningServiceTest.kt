package com.chromadmx.networking.ble

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [BleProvisioningService] state machine and validation logic.
 *
 * Since BLE hardware is unavailable in unit tests (platform stubs throw
 * [UnsupportedOperationException]), these tests exercise:
 * - Error handling for unsupported platforms
 * - Config validation before provisioning
 * - State transitions on failures
 * - Reset behavior
 *
 * Integration tests with real BLE hardware would run on-device.
 */
class BleProvisioningServiceTest {

    private fun createService(): BleProvisioningService {
        return BleProvisioningService(
            scanner = BleScanner(),
            provisioner = BleProvisioner()
        )
    }

    private fun validConfig() = NodeConfig(
        name = "Test Node",
        wifiSsid = "TestNetwork",
        wifiPassword = "password123",
        universe = 0,
        dmxStartAddress = 1
    )

    private fun testNode() = BleNode(
        deviceId = "AA:BB:CC:DD:EE:FF",
        name = "Test Node",
        rssi = -50,
        isProvisioned = false
    )

    // ================================================================
    // Initial State
    // ================================================================

    @Test
    fun initialState_isIdle() {
        val service = createService()
        assertEquals(ProvisioningState.IDLE, service.state.value)
    }

    @Test
    fun initialState_noError() {
        val service = createService()
        assertNull(service.errorMessage.value)
    }

    @Test
    fun initialState_noLastConfig() {
        val service = createService()
        assertNull(service.lastProvisionedConfig.value)
    }

    @Test
    fun initialState_emptyDiscoveredNodes() {
        val service = createService()
        assertTrue(service.discoveredNodes.value.isEmpty())
    }

    @Test
    fun initialState_notScanning() {
        val service = createService()
        assertFalse(service.isScanning.value)
    }

    // ================================================================
    // Scanning (Unsupported Platform)
    // ================================================================

    @Test
    fun startScanning_unsupportedPlatform_returnsFalse() {
        val service = createService()
        val result = service.startScanning()
        assertFalse(result)
    }

    @Test
    fun startScanning_unsupportedPlatform_transitionsToError() {
        val service = createService()
        service.startScanning()
        assertEquals(ProvisioningState.ERROR, service.state.value)
    }

    @Test
    fun startScanning_unsupportedPlatform_setsErrorMessage() {
        val service = createService()
        service.startScanning()
        assertTrue(service.errorMessage.value?.contains("not supported") == true)
    }

    @Test
    fun stopScanning_whenNotScanning_remainsIdle() {
        val service = createService()
        service.stopScanning()
        assertEquals(ProvisioningState.IDLE, service.state.value)
    }

    @Test
    fun stopScanning_afterFailedScan_remainsInErrorState() {
        val service = createService()
        service.startScanning() // Fails, goes to ERROR
        service.stopScanning()
        // stopScanning only transitions to IDLE if currently SCANNING
        assertEquals(ProvisioningState.ERROR, service.state.value)
    }

    // ================================================================
    // Provisioning — Validation Failures
    // ================================================================

    @Test
    fun provision_invalidConfig_blankName_returnsNull() = runTest {
        val service = createService()
        val config = validConfig().copy(name = "")
        val result = service.provision(testNode(), config)
        assertNull(result)
    }

    @Test
    fun provision_invalidConfig_blankName_transitionsToError() = runTest {
        val service = createService()
        val config = validConfig().copy(name = "")
        service.provision(testNode(), config)
        assertEquals(ProvisioningState.ERROR, service.state.value)
    }

    @Test
    fun provision_invalidConfig_blankName_setsErrorMessage() = runTest {
        val service = createService()
        val config = validConfig().copy(name = "")
        service.provision(testNode(), config)
        assertTrue(service.errorMessage.value?.contains("name") == true)
    }

    @Test
    fun provision_invalidConfig_blankSsid_returnsNull() = runTest {
        val service = createService()
        val config = validConfig().copy(wifiSsid = "")
        val result = service.provision(testNode(), config)
        assertNull(result)
    }

    @Test
    fun provision_invalidConfig_blankSsid_transitionsToError() = runTest {
        val service = createService()
        val config = validConfig().copy(wifiSsid = "")
        service.provision(testNode(), config)
        assertEquals(ProvisioningState.ERROR, service.state.value)
    }

    @Test
    fun provision_invalidConfig_universeOutOfRange_returnsNull() = runTest {
        val service = createService()
        val config = validConfig().copy(universe = -1)
        val result = service.provision(testNode(), config)
        assertNull(result)
    }

    @Test
    fun provision_invalidConfig_dmxAddressOutOfRange_returnsNull() = runTest {
        val service = createService()
        val config = validConfig().copy(dmxStartAddress = 0)
        val result = service.provision(testNode(), config)
        assertNull(result)
    }

    @Test
    fun provision_invalidConfig_dmxAddressTooHigh_returnsNull() = runTest {
        val service = createService()
        val config = validConfig().copy(dmxStartAddress = 513)
        val result = service.provision(testNode(), config)
        assertNull(result)
    }

    @Test
    fun provision_invalidConfig_longName_returnsNull() = runTest {
        val service = createService()
        val config = validConfig().copy(name = "A".repeat(33))
        val result = service.provision(testNode(), config)
        assertNull(result)
    }

    @Test
    fun provision_invalidConfig_longSsid_returnsNull() = runTest {
        val service = createService()
        val config = validConfig().copy(wifiSsid = "S".repeat(33))
        val result = service.provision(testNode(), config)
        assertNull(result)
    }

    @Test
    fun provision_invalidConfig_longPassword_returnsNull() = runTest {
        val service = createService()
        val config = validConfig().copy(wifiPassword = "P".repeat(65))
        val result = service.provision(testNode(), config)
        assertNull(result)
    }

    @Test
    fun provision_invalidConfig_multipleErrors_returnsFirstError() = runTest {
        val service = createService()
        val config = NodeConfig(
            name = "",
            wifiSsid = "",
            wifiPassword = "",
            universe = -1,
            dmxStartAddress = 0
        )
        service.provision(testNode(), config)
        // First error should be about the name
        assertTrue(service.errorMessage.value?.contains("name") == true)
    }

    // ================================================================
    // Provisioning — Platform Unsupported (connect throws)
    // ================================================================

    @Test
    fun provision_unsupportedPlatform_returnsNull() = runTest {
        val service = createService()
        val result = service.provision(testNode(), validConfig())
        assertNull(result)
    }

    @Test
    fun provision_unsupportedPlatform_transitionsToError() = runTest {
        val service = createService()
        service.provision(testNode(), validConfig())
        assertEquals(ProvisioningState.ERROR, service.state.value)
    }

    @Test
    fun provision_unsupportedPlatform_setsErrorMessage() = runTest {
        val service = createService()
        service.provision(testNode(), validConfig())
        assertTrue(service.errorMessage.value != null)
    }

    // ================================================================
    // Reset
    // ================================================================

    @Test
    fun reset_afterError_returnsToIdle() {
        val service = createService()
        service.startScanning() // triggers error
        assertEquals(ProvisioningState.ERROR, service.state.value)

        service.reset()
        assertEquals(ProvisioningState.IDLE, service.state.value)
    }

    @Test
    fun reset_clearsErrorMessage() {
        val service = createService()
        service.startScanning() // triggers error with message
        assertTrue(service.errorMessage.value != null)

        service.reset()
        assertNull(service.errorMessage.value)
    }

    @Test
    fun reset_clearsLastProvisionedConfig() = runTest {
        val service = createService()
        // Trigger error path which doesn't set lastProvisionedConfig, but reset should still work
        service.provision(testNode(), validConfig())
        service.reset()
        assertNull(service.lastProvisionedConfig.value)
    }

    @Test
    fun reset_fromIdle_remainsIdle() {
        val service = createService()
        service.reset()
        assertEquals(ProvisioningState.IDLE, service.state.value)
    }

    // ================================================================
    // State Flow Transitions
    // ================================================================

    @Test
    fun stateFlow_canBeObserved() {
        val service = createService()
        // Just verify that state is a StateFlow we can read
        val currentState = service.state.value
        assertEquals(ProvisioningState.IDLE, currentState)
    }

    @Test
    fun errorMessageFlow_canBeObserved() {
        val service = createService()
        val msg = service.errorMessage.value
        assertNull(msg)
    }

    @Test
    fun discoveredNodesFlow_canBeObserved() {
        val service = createService()
        val nodes = service.discoveredNodes.value
        assertTrue(nodes.isEmpty())
    }

    @Test
    fun isScanningFlow_canBeObserved() {
        val service = createService()
        val scanning = service.isScanning.value
        assertFalse(scanning)
    }

    // ================================================================
    // Multiple Operations
    // ================================================================

    @Test
    fun multipleResets_stableState() {
        val service = createService()
        service.reset()
        service.reset()
        service.reset()
        assertEquals(ProvisioningState.IDLE, service.state.value)
        assertNull(service.errorMessage.value)
    }

    @Test
    fun scanThenReset_thenScan_repeatable() {
        val service = createService()
        service.startScanning()
        assertEquals(ProvisioningState.ERROR, service.state.value)

        service.reset()
        assertEquals(ProvisioningState.IDLE, service.state.value)

        service.startScanning()
        assertEquals(ProvisioningState.ERROR, service.state.value)
    }

    @Test
    fun provision_thenReset_thenProvision_repeatable() = runTest {
        val service = createService()

        service.provision(testNode(), validConfig())
        assertEquals(ProvisioningState.ERROR, service.state.value)

        service.reset()
        assertEquals(ProvisioningState.IDLE, service.state.value)

        service.provision(testNode(), validConfig())
        assertEquals(ProvisioningState.ERROR, service.state.value)
    }
}
