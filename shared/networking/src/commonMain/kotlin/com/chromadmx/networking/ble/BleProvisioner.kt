package com.chromadmx.networking.ble

import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-agnostic BLE provisioner for configuring ESP32 DMX nodes.
 *
 * Manages the GATT connection lifecycle: connect, read current config,
 * write new config, verify, and disconnect. The provisioning state is
 * exposed as a [StateFlow] for reactive UI binding.
 *
 * Actual implementations use platform-specific BLE GATT APIs:
 * - Android: `BluetoothGatt` + `BluetoothGattCallback`
 * - iOS: `CBPeripheral` + `CBPeripheralDelegate`
 *
 * The provisioner writes to the ChromaDMX custom GATT service defined
 * in [GattServiceSpec.CHROMA_DMX].
 */
expect class BleProvisioner() {

    /**
     * Connect to a BLE node by its device ID.
     *
     * Establishes a GATT connection and discovers the ChromaDMX service.
     *
     * @param deviceId Platform-specific device identifier from [BleNode.deviceId]
     * @return true if connection and service discovery succeeded
     */
    suspend fun connect(deviceId: String): Boolean

    /**
     * Read the current configuration from the connected node.
     *
     * Reads all config characteristics from the ChromaDMX GATT service
     * and assembles them into a [NodeConfig].
     *
     * @return current config, or null if read failed or node is not connected
     */
    suspend fun readConfig(): NodeConfig?

    /**
     * Write a new configuration to the connected node.
     *
     * Writes each config field to its corresponding GATT characteristic.
     * After a successful write, the node should be rebooted (via the
     * command characteristic) to apply the new settings.
     *
     * @param config The configuration to write
     * @return true if all characteristics were written successfully
     */
    suspend fun writeConfig(config: NodeConfig): Boolean

    /**
     * Disconnect from the currently connected node.
     *
     * Releases the GATT connection and cleans up resources.
     * Safe to call even if no connection is active.
     */
    suspend fun disconnect()

    /**
     * Current state of the provisioning workflow.
     *
     * UI should observe this to display progress and handle errors.
     */
    val state: StateFlow<ProvisioningState>
}
