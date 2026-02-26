package com.chromadmx.networking.ble

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Android stub implementation of [BleProvisioner].
 *
 * TODO: Implement using `BluetoothGatt` + `BluetoothGattCallback`.
 * Requires BLUETOOTH_CONNECT permission on API 31+.
 *
 * The implementation should:
 * 1. Connect via `BluetoothDevice.connectGatt()`
 * 2. Discover services and locate the ChromaDMX GATT service
 * 3. Read/write characteristics using `BluetoothGatt.readCharacteristic()`
 *    and `writeCharacteristic()`
 * 4. Handle connection state changes and errors via the callback
 *
 * This stub allows the module to compile for Android targets.
 */
actual class BleProvisioner actual constructor() {

    private val _state = MutableStateFlow(ProvisioningState.IDLE)
    actual val state: StateFlow<ProvisioningState> = _state

    actual suspend fun connect(deviceId: String): Boolean {
        // Stub: Android GATT connect not yet implemented
        throw UnsupportedOperationException("Android BLE provisioning not yet implemented")
    }

    actual suspend fun readConfig(): NodeConfig? {
        // Stub: Android GATT read not yet implemented
        throw UnsupportedOperationException("Android BLE provisioning not yet implemented")
    }

    actual suspend fun writeConfig(config: NodeConfig): Boolean {
        // Stub: Android GATT write not yet implemented
        throw UnsupportedOperationException("Android BLE provisioning not yet implemented")
    }

    actual suspend fun disconnect() {
        // Stub: Android GATT disconnect not yet implemented
        // Safe no-op when not connected
    }
}
