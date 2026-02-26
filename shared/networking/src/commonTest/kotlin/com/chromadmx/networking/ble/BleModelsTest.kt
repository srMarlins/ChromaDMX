package com.chromadmx.networking.ble

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for BLE data models: [BleNode], [NodeConfig], [GattServiceSpec],
 * [SignalQuality], and [ProvisioningState].
 */
class BleModelsTest {

    // ================================================================
    // BleNode
    // ================================================================

    @Test
    fun bleNode_displayName_usesNameWhenPresent() {
        val node = BleNode(
            deviceId = "AA:BB:CC:DD:EE:FF",
            name = "Stage Left",
            rssi = -50,
            isProvisioned = false
        )
        assertEquals("Stage Left", node.displayName)
    }

    @Test
    fun bleNode_displayName_fallsBackToDeviceIdSuffix() {
        val node = BleNode(
            deviceId = "AA:BB:CC:DD:EE:FF",
            name = null,
            rssi = -50,
            isProvisioned = false
        )
        assertEquals("Node DD:EE:FF", node.displayName)
    }

    @Test
    fun bleNode_displayName_shortDeviceId() {
        val node = BleNode(
            deviceId = "ABCD",
            name = null,
            rssi = -50,
            isProvisioned = false
        )
        assertEquals("Node ABCD", node.displayName)
    }

    @Test
    fun bleNode_signalQuality_excellent() {
        val node = BleNode("id", "Node", -30, false)
        assertEquals(SignalQuality.EXCELLENT, node.signalQuality)
    }

    @Test
    fun bleNode_signalQuality_excellentBoundary() {
        val node = BleNode("id", "Node", -50, false)
        assertEquals(SignalQuality.EXCELLENT, node.signalQuality)
    }

    @Test
    fun bleNode_signalQuality_good() {
        val node = BleNode("id", "Node", -51, false)
        assertEquals(SignalQuality.GOOD, node.signalQuality)
    }

    @Test
    fun bleNode_signalQuality_goodBoundary() {
        val node = BleNode("id", "Node", -65, false)
        assertEquals(SignalQuality.GOOD, node.signalQuality)
    }

    @Test
    fun bleNode_signalQuality_fair() {
        val node = BleNode("id", "Node", -66, false)
        assertEquals(SignalQuality.FAIR, node.signalQuality)
    }

    @Test
    fun bleNode_signalQuality_fairBoundary() {
        val node = BleNode("id", "Node", -80, false)
        assertEquals(SignalQuality.FAIR, node.signalQuality)
    }

    @Test
    fun bleNode_signalQuality_weak() {
        val node = BleNode("id", "Node", -81, false)
        assertEquals(SignalQuality.WEAK, node.signalQuality)
    }

    @Test
    fun bleNode_signalQuality_veryWeak() {
        val node = BleNode("id", "Node", -100, false)
        assertEquals(SignalQuality.WEAK, node.signalQuality)
    }

    @Test
    fun bleNode_isProvisioned_true() {
        val node = BleNode("id", "Node", -50, true)
        assertTrue(node.isProvisioned)
    }

    @Test
    fun bleNode_isProvisioned_false() {
        val node = BleNode("id", "Node", -50, false)
        assertFalse(node.isProvisioned)
    }

    @Test
    fun bleNode_equality() {
        val node1 = BleNode("id1", "Node", -50, false)
        val node2 = BleNode("id1", "Node", -50, false)
        assertEquals(node1, node2)
    }

    @Test
    fun bleNode_copy_updatesRssi() {
        val node = BleNode("id", "Node", -50, false)
        val updated = node.copy(rssi = -30)
        assertEquals(-30, updated.rssi)
        assertEquals("id", updated.deviceId)
    }

    // ================================================================
    // NodeConfig — Validation
    // ================================================================

    @Test
    fun nodeConfig_validConfig_noErrors() {
        val config = NodeConfig(
            name = "Stage Left",
            wifiSsid = "MyNetwork",
            wifiPassword = "secret123",
            universe = 0,
            dmxStartAddress = 1
        )
        assertTrue(config.isValid)
        assertTrue(config.validate().isEmpty())
    }

    @Test
    fun nodeConfig_blankName_error() {
        val config = NodeConfig(
            name = "",
            wifiSsid = "MyNetwork",
            wifiPassword = "secret",
            universe = 0,
            dmxStartAddress = 1
        )
        assertFalse(config.isValid)
        assertTrue(config.validate().any { "name" in it.lowercase() })
    }

    @Test
    fun nodeConfig_longName_error() {
        val config = NodeConfig(
            name = "A".repeat(33),
            wifiSsid = "MyNetwork",
            wifiPassword = "secret",
            universe = 0,
            dmxStartAddress = 1
        )
        assertFalse(config.isValid)
        assertTrue(config.validate().any { "32" in it })
    }

    @Test
    fun nodeConfig_maxLengthName_valid() {
        val config = NodeConfig(
            name = "A".repeat(32),
            wifiSsid = "MyNetwork",
            wifiPassword = "secret",
            universe = 0,
            dmxStartAddress = 1
        )
        assertTrue(config.isValid)
    }

    @Test
    fun nodeConfig_blankSsid_error() {
        val config = NodeConfig(
            name = "Node",
            wifiSsid = "",
            wifiPassword = "secret",
            universe = 0,
            dmxStartAddress = 1
        )
        assertFalse(config.isValid)
        assertTrue(config.validate().any { "ssid" in it.lowercase() })
    }

    @Test
    fun nodeConfig_longSsid_error() {
        val config = NodeConfig(
            name = "Node",
            wifiSsid = "S".repeat(33),
            wifiPassword = "secret",
            universe = 0,
            dmxStartAddress = 1
        )
        assertFalse(config.isValid)
    }

    @Test
    fun nodeConfig_maxLengthSsid_valid() {
        val config = NodeConfig(
            name = "Node",
            wifiSsid = "S".repeat(32),
            wifiPassword = "secret",
            universe = 0,
            dmxStartAddress = 1
        )
        assertTrue(config.isValid)
    }

    @Test
    fun nodeConfig_longPassword_error() {
        val config = NodeConfig(
            name = "Node",
            wifiSsid = "MyNetwork",
            wifiPassword = "P".repeat(65),
            universe = 0,
            dmxStartAddress = 1
        )
        assertFalse(config.isValid)
    }

    @Test
    fun nodeConfig_maxLengthPassword_valid() {
        val config = NodeConfig(
            name = "Node",
            wifiSsid = "MyNetwork",
            wifiPassword = "P".repeat(64),
            universe = 0,
            dmxStartAddress = 1
        )
        assertTrue(config.isValid)
    }

    @Test
    fun nodeConfig_emptyPassword_valid() {
        val config = NodeConfig(
            name = "Node",
            wifiSsid = "OpenNetwork",
            wifiPassword = "",
            universe = 0,
            dmxStartAddress = 1
        )
        assertTrue(config.isValid)
    }

    @Test
    fun nodeConfig_negativeUniverse_error() {
        val config = NodeConfig(
            name = "Node",
            wifiSsid = "Net",
            wifiPassword = "pass",
            universe = -1,
            dmxStartAddress = 1
        )
        assertFalse(config.isValid)
        assertTrue(config.validate().any { "universe" in it.lowercase() })
    }

    @Test
    fun nodeConfig_universeZero_valid() {
        val config = NodeConfig(
            name = "Node",
            wifiSsid = "Net",
            wifiPassword = "pass",
            universe = 0,
            dmxStartAddress = 1
        )
        assertTrue(config.isValid)
    }

    @Test
    fun nodeConfig_maxUniverse_valid() {
        val config = NodeConfig(
            name = "Node",
            wifiSsid = "Net",
            wifiPassword = "pass",
            universe = 32767,
            dmxStartAddress = 1
        )
        assertTrue(config.isValid)
    }

    @Test
    fun nodeConfig_universeOverMax_error() {
        val config = NodeConfig(
            name = "Node",
            wifiSsid = "Net",
            wifiPassword = "pass",
            universe = 32768,
            dmxStartAddress = 1
        )
        assertFalse(config.isValid)
    }

    @Test
    fun nodeConfig_dmxAddressZero_error() {
        val config = NodeConfig(
            name = "Node",
            wifiSsid = "Net",
            wifiPassword = "pass",
            universe = 0,
            dmxStartAddress = 0
        )
        assertFalse(config.isValid)
        assertTrue(config.validate().any { "start address" in it.lowercase() })
    }

    @Test
    fun nodeConfig_dmxAddressOne_valid() {
        val config = NodeConfig(
            name = "Node",
            wifiSsid = "Net",
            wifiPassword = "pass",
            universe = 0,
            dmxStartAddress = 1
        )
        assertTrue(config.isValid)
    }

    @Test
    fun nodeConfig_dmxAddress512_valid() {
        val config = NodeConfig(
            name = "Node",
            wifiSsid = "Net",
            wifiPassword = "pass",
            universe = 0,
            dmxStartAddress = 512
        )
        assertTrue(config.isValid)
    }

    @Test
    fun nodeConfig_dmxAddress513_error() {
        val config = NodeConfig(
            name = "Node",
            wifiSsid = "Net",
            wifiPassword = "pass",
            universe = 0,
            dmxStartAddress = 513
        )
        assertFalse(config.isValid)
    }

    @Test
    fun nodeConfig_multipleErrors() {
        val config = NodeConfig(
            name = "",
            wifiSsid = "",
            wifiPassword = "P".repeat(65),
            universe = -1,
            dmxStartAddress = 0
        )
        val errors = config.validate()
        assertEquals(5, errors.size)
    }

    @Test
    fun nodeConfig_equality() {
        val config1 = NodeConfig("Node", "SSID", "pass", 0, 1)
        val config2 = NodeConfig("Node", "SSID", "pass", 0, 1)
        assertEquals(config1, config2)
    }

    // ================================================================
    // GattServiceSpec
    // ================================================================

    @Test
    fun gattServiceSpec_chromaDmx_hasCorrectServiceUuid() {
        val spec = GattServiceSpec.CHROMA_DMX
        assertEquals("4368726f-6d61-444d-5800-000000000001", spec.serviceUuid)
    }

    @Test
    fun gattServiceSpec_chromaDmx_hasAllCharacteristics() {
        val spec = GattServiceSpec.CHROMA_DMX
        val expectedKeys = setOf(
            "node_name", "wifi_ssid", "wifi_password",
            "universe", "dmx_start_address", "provisioned_flag",
            "firmware_version", "command"
        )
        assertEquals(expectedKeys, spec.characteristics.keys)
    }

    @Test
    fun gattServiceSpec_chromaDmx_nodeNameUuid() {
        val spec = GattServiceSpec.CHROMA_DMX
        assertEquals(
            "4368726f-6d61-444d-5800-000000000010",
            spec.characteristics["node_name"]
        )
    }

    @Test
    fun gattServiceSpec_chromaDmx_wifiSsidUuid() {
        val spec = GattServiceSpec.CHROMA_DMX
        assertEquals(
            "4368726f-6d61-444d-5800-000000000011",
            spec.characteristics["wifi_ssid"]
        )
    }

    @Test
    fun gattServiceSpec_chromaDmx_wifiPasswordUuid() {
        val spec = GattServiceSpec.CHROMA_DMX
        assertEquals(
            "4368726f-6d61-444d-5800-000000000012",
            spec.characteristics["wifi_password"]
        )
    }

    @Test
    fun gattServiceSpec_chromaDmx_universeUuid() {
        val spec = GattServiceSpec.CHROMA_DMX
        assertEquals(
            "4368726f-6d61-444d-5800-000000000013",
            spec.characteristics["universe"]
        )
    }

    @Test
    fun gattServiceSpec_chromaDmx_dmxStartAddressUuid() {
        val spec = GattServiceSpec.CHROMA_DMX
        assertEquals(
            "4368726f-6d61-444d-5800-000000000014",
            spec.characteristics["dmx_start_address"]
        )
    }

    @Test
    fun gattServiceSpec_chromaDmx_provisionedFlagUuid() {
        val spec = GattServiceSpec.CHROMA_DMX
        assertEquals(
            "4368726f-6d61-444d-5800-000000000015",
            spec.characteristics["provisioned_flag"]
        )
    }

    @Test
    fun gattServiceSpec_chromaDmx_firmwareVersionUuid() {
        val spec = GattServiceSpec.CHROMA_DMX
        assertEquals(
            "4368726f-6d61-444d-5800-000000000016",
            spec.characteristics["firmware_version"]
        )
    }

    @Test
    fun gattServiceSpec_chromaDmx_commandUuid() {
        val spec = GattServiceSpec.CHROMA_DMX
        assertEquals(
            "4368726f-6d61-444d-5800-000000000020",
            spec.characteristics["command"]
        )
    }

    @Test
    fun gattServiceSpec_chromaDmx_characteristicCount() {
        val spec = GattServiceSpec.CHROMA_DMX
        assertEquals(8, spec.characteristics.size)
    }

    @Test
    fun gattServiceSpec_allUuidsUnique() {
        val spec = GattServiceSpec.CHROMA_DMX
        val uuids = spec.characteristics.values.toList()
        assertEquals(uuids.size, uuids.toSet().size)
    }

    @Test
    fun gattServiceSpec_serviceUuidDifferentFromCharacteristics() {
        val spec = GattServiceSpec.CHROMA_DMX
        assertFalse(spec.characteristics.values.contains(spec.serviceUuid))
    }

    @Test
    fun gattServiceSpec_customSpec() {
        val custom = GattServiceSpec(
            serviceUuid = "custom-uuid",
            characteristics = mapOf("test" to "test-uuid")
        )
        assertEquals("custom-uuid", custom.serviceUuid)
        assertEquals(1, custom.characteristics.size)
    }

    // ================================================================
    // ProvisioningState
    // ================================================================

    @Test
    fun provisioningState_allStatesExist() {
        val states = ProvisioningState.entries
        assertEquals(8, states.size)
    }

    @Test
    fun provisioningState_ordinalOrder() {
        assertEquals(0, ProvisioningState.IDLE.ordinal)
        assertEquals(1, ProvisioningState.SCANNING.ordinal)
        assertEquals(2, ProvisioningState.CONNECTING.ordinal)
        assertEquals(3, ProvisioningState.READING_CONFIG.ordinal)
        assertEquals(4, ProvisioningState.WRITING_CONFIG.ordinal)
        assertEquals(5, ProvisioningState.VERIFYING.ordinal)
        assertEquals(6, ProvisioningState.SUCCESS.ordinal)
        assertEquals(7, ProvisioningState.ERROR.ordinal)
    }

    // ================================================================
    // SignalQuality
    // ================================================================

    @Test
    fun signalQuality_allValuesExist() {
        val qualities = SignalQuality.entries
        assertEquals(4, qualities.size)
    }

    @Test
    fun signalQuality_orderMatches() {
        assertEquals(0, SignalQuality.EXCELLENT.ordinal)
        assertEquals(1, SignalQuality.GOOD.ordinal)
        assertEquals(2, SignalQuality.FAIR.ordinal)
        assertEquals(3, SignalQuality.WEAK.ordinal)
    }
}
