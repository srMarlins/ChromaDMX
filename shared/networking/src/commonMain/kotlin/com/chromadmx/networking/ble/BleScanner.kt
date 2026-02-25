package com.chromadmx.networking.ble

import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-agnostic BLE scanner for discovering ESP32 DMX nodes.
 *
 * Scans for BLE peripherals advertising the ChromaDMX GATT service UUID.
 * Discovered nodes are exposed as a [StateFlow] for reactive UI consumption.
 *
 * Actual implementations use platform-specific BLE APIs:
 * - Android: `BluetoothLeScanner` (requires BLUETOOTH_SCAN permission on API 31+)
 * - iOS: `CBCentralManager` (requires CoreBluetooth entitlement)
 *
 * Both platforms require runtime permission grants before scanning will succeed.
 */
expect class BleScanner() {

    /**
     * Start scanning for ChromaDMX BLE nodes.
     *
     * Discovered nodes are added to [discoveredNodes]. Duplicate advertisements
     * for the same device ID update the existing entry (RSSI refresh).
     *
     * Scanning continues until [stopScan] is called or a platform timeout occurs.
     */
    fun startScan()

    /**
     * Stop the active BLE scan.
     *
     * Does nothing if no scan is in progress.
     */
    fun stopScan()

    /**
     * Flow of currently discovered BLE nodes.
     *
     * Updated in real-time as advertisements are received. Nodes that have
     * not been seen for a platform-defined timeout may be removed.
     */
    val discoveredNodes: StateFlow<List<BleNode>>

    /**
     * Whether a BLE scan is currently in progress.
     */
    val isScanning: StateFlow<Boolean>
}
