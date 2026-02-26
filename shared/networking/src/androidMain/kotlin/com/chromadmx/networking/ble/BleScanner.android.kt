package com.chromadmx.networking.ble

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Android stub implementation of [BleScanner].
 *
 * TODO: Implement using `BluetoothLeScanner` from `android.bluetooth.le`.
 * Requires BLUETOOTH_SCAN and BLUETOOTH_CONNECT permissions on API 31+,
 * or BLUETOOTH and BLUETOOTH_ADMIN on older versions.
 *
 * This stub allows the module to compile for Android targets. Actual BLE
 * scanning requires device testing and cannot be verified in unit tests.
 */
actual class BleScanner actual constructor() {

    private val _discoveredNodes = MutableStateFlow<List<BleNode>>(emptyList())
    actual val discoveredNodes: StateFlow<List<BleNode>> = _discoveredNodes

    private val _isScanning = MutableStateFlow(false)
    actual val isScanning: StateFlow<Boolean> = _isScanning

    actual fun startScan() {
        // Stub: Android BLE scan not yet implemented
        // TODO: Use BluetoothLeScanner.startScan() with ScanFilter for ChromaDMX service UUID
        throw UnsupportedOperationException("Android BLE scanning not yet implemented")
    }

    actual fun stopScan() {
        // Stub: Android BLE scan stop not yet implemented
        // TODO: Use BluetoothLeScanner.stopScan()
        throw UnsupportedOperationException("Android BLE scanning not yet implemented")
    }
}
