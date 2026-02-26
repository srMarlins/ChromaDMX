package com.chromadmx.networking.ble

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * iOS stub implementation of [BleProvisioner].
 *
 * TODO: Implement using `CBPeripheral` + `CBPeripheralDelegate`.
 *
 * The implementation should:
 * 1. Connect via `CBCentralManager.connect(_:options:)`
 * 2. Discover services via `CBPeripheral.discoverServices()`
 * 3. Read/write characteristics via `CBPeripheral.readValue(for:)`
 *    and `writeValue(_:for:type:)`
 * 4. Handle delegate callbacks for state changes and errors
 *
 * This stub allows the module to compile for iOS targets.
 */
actual class BleProvisioner actual constructor() {

    private val _state = MutableStateFlow(ProvisioningState.IDLE)
    actual val state: StateFlow<ProvisioningState> = _state

    actual suspend fun connect(deviceId: String): Boolean {
        // Stub: iOS GATT connect not yet implemented
        throw UnsupportedOperationException("iOS BLE provisioning not yet implemented")
    }

    actual suspend fun readConfig(): NodeConfig? {
        // Stub: iOS GATT read not yet implemented
        throw UnsupportedOperationException("iOS BLE provisioning not yet implemented")
    }

    actual suspend fun writeConfig(config: NodeConfig): Boolean {
        // Stub: iOS GATT write not yet implemented
        throw UnsupportedOperationException("iOS BLE provisioning not yet implemented")
    }

    actual suspend fun disconnect() {
        // Stub: iOS GATT disconnect not yet implemented
        // Safe no-op when not connected
    }
}
