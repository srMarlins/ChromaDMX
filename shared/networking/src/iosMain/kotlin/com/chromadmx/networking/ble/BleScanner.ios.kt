package com.chromadmx.networking.ble

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * iOS stub implementation of [BleScanner].
 *
 * TODO: Implement using `CBCentralManager` from CoreBluetooth.
 * Requires NSBluetoothAlwaysUsageDescription in Info.plist and
 * user consent at runtime.
 *
 * This stub allows the module to compile for iOS targets. Actual BLE
 * scanning requires device testing and cannot be verified in unit tests.
 */
actual class BleScanner actual constructor() {

    private val _discoveredNodes = MutableStateFlow<List<BleNode>>(emptyList())
    actual val discoveredNodes: StateFlow<List<BleNode>> = _discoveredNodes

    private val _isScanning = MutableStateFlow(false)
    actual val isScanning: StateFlow<Boolean> = _isScanning

    actual fun startScan() {
        // Stub: iOS BLE scan not yet implemented
        // TODO: Use CBCentralManager.scanForPeripherals(withServices:) filtering on ChromaDMX UUID
        throw UnsupportedOperationException("iOS BLE scanning not yet implemented")
    }

    actual fun stopScan() {
        // Stub: iOS BLE scan stop not yet implemented
        // TODO: Use CBCentralManager.stopScan()
        throw UnsupportedOperationException("iOS BLE scanning not yet implemented")
    }
}
