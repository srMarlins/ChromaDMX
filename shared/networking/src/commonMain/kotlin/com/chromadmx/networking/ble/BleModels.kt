package com.chromadmx.networking.ble

import kotlinx.serialization.Serializable

/**
 * Represents a BLE-discoverable ESP32 DMX node.
 *
 * During BLE scanning, the app discovers nearby ESP32 nodes that advertise
 * the ChromaDMX GATT service. Each node reports its device ID, optional
 * friendly name, signal strength (RSSI), and whether it has been previously
 * provisioned with Wi-Fi and Art-Net configuration.
 *
 * @property deviceId   Platform-specific BLE device identifier (MAC on Android, UUID on iOS)
 * @property name       Advertised device name, or null if not broadcast
 * @property rssi       Received signal strength in dBm (typical range: -100 to 0)
 * @property isProvisioned Whether the node already has a valid Wi-Fi + Art-Net config
 */
@Serializable
data class BleNode(
    val deviceId: String,
    val name: String?,
    val rssi: Int,
    val isProvisioned: Boolean
) {
    /**
     * Human-readable display name: uses advertised name if available,
     * otherwise falls back to a truncated device ID.
     */
    val displayName: String
        get() = name ?: "Node ${deviceId.takeLast(8)}"

    /**
     * Signal quality category based on RSSI.
     */
    val signalQuality: SignalQuality
        get() = when {
            rssi >= -50 -> SignalQuality.EXCELLENT
            rssi >= -65 -> SignalQuality.GOOD
            rssi >= -80 -> SignalQuality.FAIR
            else -> SignalQuality.WEAK
        }
}

/**
 * BLE signal quality categories for UI display.
 */
enum class SignalQuality {
    EXCELLENT, GOOD, FAIR, WEAK
}

/**
 * Configuration to be written to an ESP32 node during provisioning.
 *
 * Contains the Wi-Fi credentials and Art-Net addressing needed for the
 * node to join the local network and begin outputting DMX data.
 *
 * @property name            Friendly name for the node (written to GATT)
 * @property wifiSsid        Wi-Fi network SSID
 * @property wifiPassword    Wi-Fi network password
 * @property universe        Art-Net universe number (0-32767)
 * @property dmxStartAddress DMX start address within the universe (1-512)
 */
@Serializable
data class NodeConfig(
    val name: String,
    val wifiSsid: String,
    val wifiPassword: String,
    val universe: Int,
    val dmxStartAddress: Int
) {
    /**
     * Validate the configuration values.
     *
     * @return list of validation error messages; empty if valid
     */
    fun validate(): List<String> = buildList {
        if (name.isBlank()) add("Node name must not be empty")
        if (name.length > 32) add("Node name must be 32 characters or fewer")
        if (wifiSsid.isBlank()) add("Wi-Fi SSID must not be empty")
        if (wifiSsid.length > 32) add("Wi-Fi SSID must be 32 characters or fewer")
        if (wifiPassword.length > 64) add("Wi-Fi password must be 64 characters or fewer")
        if (universe !in 0..32767) add("Universe must be between 0 and 32767")
        if (dmxStartAddress !in 1..512) add("DMX start address must be between 1 and 512")
    }

    val isValid: Boolean get() = validate().isEmpty()
}

/**
 * State machine for the BLE provisioning workflow.
 *
 * Transitions: IDLE -> SCANNING -> CONNECTING -> READING_CONFIG -> WRITING_CONFIG -> VERIFYING -> SUCCESS
 * Any state may transition to ERROR. ERROR and SUCCESS transition back to IDLE.
 */
enum class ProvisioningState {
    /** No provisioning activity in progress. */
    IDLE,

    /** Scanning for BLE devices advertising the ChromaDMX service. */
    SCANNING,

    /** Establishing a GATT connection to the selected node. */
    CONNECTING,

    /** Reading the current configuration from the node. */
    READING_CONFIG,

    /** Writing new configuration to the node. */
    WRITING_CONFIG,

    /** Verifying the written configuration by reading it back. */
    VERIFYING,

    /** Provisioning completed successfully; node will reboot. */
    SUCCESS,

    /** An error occurred during provisioning. */
    ERROR
}

/**
 * Specification for the custom GATT service and its characteristics.
 *
 * Used to define the BLE service contract between the app and the ESP32 firmware.
 *
 * @property serviceUuid       UUID of the custom GATT service
 * @property characteristics   Map of characteristic name to its UUID
 */
@Serializable
data class GattServiceSpec(
    val serviceUuid: String,
    val characteristics: Map<String, String>
) {
    companion object {
        /**
         * Default ChromaDMX GATT service specification.
         *
         * Service UUID: 4368726f-6d61-444d-5800-000000000001
         * (Derived from "ChromaDMX" in hex)
         */
        val CHROMA_DMX = GattServiceSpec(
            serviceUuid = "4368726f-6d61-444d-5800-000000000001",
            characteristics = mapOf(
                "node_name" to "4368726f-6d61-444d-5800-000000000010",
                "wifi_ssid" to "4368726f-6d61-444d-5800-000000000011",
                "wifi_password" to "4368726f-6d61-444d-5800-000000000012",
                "universe" to "4368726f-6d61-444d-5800-000000000013",
                "dmx_start_address" to "4368726f-6d61-444d-5800-000000000014",
                "provisioned_flag" to "4368726f-6d61-444d-5800-000000000015",
                "firmware_version" to "4368726f-6d61-444d-5800-000000000016",
                "command" to "4368726f-6d61-444d-5800-000000000020",
            )
        )
    }
}
