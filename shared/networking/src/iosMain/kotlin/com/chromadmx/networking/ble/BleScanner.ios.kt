package com.chromadmx.networking.ble

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBCentralManagerScanOptionAllowDuplicatesKey
import platform.CoreBluetooth.CBManagerStatePoweredOff
import platform.CoreBluetooth.CBManagerStatePoweredOn
import platform.CoreBluetooth.CBManagerStateResetting
import platform.CoreBluetooth.CBManagerStateUnauthorized
import platform.CoreBluetooth.CBManagerStateUnsupported
import platform.CoreBluetooth.CBPeripheral
import platform.CoreBluetooth.CBUUID
import platform.Foundation.NSNumber
import platform.darwin.NSObject

/**
 * iOS implementation of [BleScanner] using CoreBluetooth.
 *
 * Uses [CBCentralManager] to scan for BLE peripherals advertising the
 * ChromaDMX GATT service UUID. Discovered peripherals are mapped to
 * [BleNode] instances and emitted through [discoveredNodes].
 *
 * Requires `NSBluetoothAlwaysUsageDescription` in Info.plist and user
 * consent at runtime. If Bluetooth is powered off when [startScan] is
 * called, the scan is deferred and automatically started when the radio
 * transitions to [CBManagerStatePoweredOn].
 *
 * The scanner requests duplicate advertisements
 * (`CBCentralManagerScanOptionAllowDuplicatesKey`) so that RSSI values
 * are updated in real-time for already-discovered devices.
 */
actual class BleScanner actual constructor() {

    private val _discoveredNodes = MutableStateFlow<List<BleNode>>(emptyList())
    actual val discoveredNodes: StateFlow<List<BleNode>> = _discoveredNodes

    private val _isScanning = MutableStateFlow(false)
    actual val isScanning: StateFlow<Boolean> = _isScanning

    /** ChromaDMX GATT service UUID used to filter BLE advertisements. */
    private val serviceUuid: CBUUID =
        CBUUID.UUIDWithString(GattServiceSpec.CHROMA_DMX.serviceUuid)

    /**
     * Whether a scan has been requested but is waiting for the central
     * manager to reach the [CBManagerStatePoweredOn] state.
     */
    private var pendingScan = false

    /** Delegate handling all CBCentralManager callbacks. */
    private val delegate = CentralManagerDelegate()

    /**
     * The CBCentralManager instance. Created with our delegate so that
     * state-change callbacks are received immediately.
     */
    private val centralManager = CBCentralManager(
        delegate = delegate,
        queue = null // dispatch on main queue
    )

    actual fun startScan() {
        if (_isScanning.value) return

        if (centralManager.state == CBManagerStatePoweredOn) {
            beginScan()
        } else {
            // Bluetooth not ready yet; defer until centralManagerDidUpdateState fires.
            pendingScan = true
        }
    }

    actual fun stopScan() {
        pendingScan = false
        if (_isScanning.value) {
            centralManager.stopScan()
            _isScanning.value = false
        }
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    /**
     * Initiate the actual CoreBluetooth scan.
     *
     * Called either from [startScan] (if already powered on) or from
     * [CentralManagerDelegate.centralManagerDidUpdateState] when the
     * radio becomes available.
     */
    private fun beginScan() {
        // Clear previous results when starting a fresh scan
        _discoveredNodes.value = emptyList()
        _isScanning.value = true

        centralManager.scanForPeripheralsWithServices(
            serviceUUIDs = listOf(serviceUuid),
            options = mapOf(
                CBCentralManagerScanOptionAllowDuplicatesKey to true
            )
        )
    }

    /**
     * Process a discovered or re-discovered peripheral.
     *
     * If the device has already been seen, its RSSI and name are updated.
     * Otherwise a new [BleNode] is appended to the list.
     */
    private fun handleDiscoveredPeripheral(
        peripheral: CBPeripheral,
        rssi: Int
    ) {
        val deviceId = peripheral.identifier.UUIDString
        val name = peripheral.name
        val currentList = _discoveredNodes.value.toMutableList()

        val existingIndex = currentList.indexOfFirst { it.deviceId == deviceId }
        if (existingIndex >= 0) {
            // Update RSSI (and potentially name) for an already-known device
            currentList[existingIndex] = currentList[existingIndex].copy(
                name = name ?: currentList[existingIndex].name,
                rssi = rssi
            )
        } else {
            currentList.add(
                BleNode(
                    deviceId = deviceId,
                    name = name,
                    rssi = rssi,
                    isProvisioned = false // determined after GATT connection
                )
            )
        }
        _discoveredNodes.value = currentList
    }

    // ------------------------------------------------------------------
    // CBCentralManagerDelegate
    // ------------------------------------------------------------------

    /**
     * Inner delegate class implementing [CBCentralManagerDelegateProtocol].
     *
     * **Must** extend [NSObject] for Kotlin/Native ObjC protocol conformance.
     */
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    private inner class CentralManagerDelegate : NSObject(), CBCentralManagerDelegateProtocol {

        /**
         * Called when the CBCentralManager's state changes.
         *
         * If a scan was requested while Bluetooth was off, we auto-start
         * it once the radio reaches [CBManagerStatePoweredOn].
         */
        override fun centralManagerDidUpdateState(central: CBCentralManager) {
            when (central.state) {
                CBManagerStatePoweredOn -> {
                    if (pendingScan) {
                        pendingScan = false
                        beginScan()
                    }
                }
                CBManagerStatePoweredOff,
                CBManagerStateResetting,
                CBManagerStateUnauthorized,
                CBManagerStateUnsupported -> {
                    // Radio went away — stop tracking the scan
                    if (_isScanning.value) {
                        _isScanning.value = false
                    }
                    pendingScan = false
                }
                else -> {
                    // CBManagerStateUnknown or future states — ignore
                }
            }
        }

        /**
         * Called for each peripheral discovered (or re-discovered with
         * allow-duplicates enabled).
         *
         * Maps the CoreBluetooth callback parameters to our domain model.
         */
        override fun centralManager(
            central: CBCentralManager,
            didDiscoverPeripheral: CBPeripheral,
            advertisementData: Map<Any?, *>,
            RSSI: NSNumber
        ) {
            handleDiscoveredPeripheral(
                peripheral = didDiscoverPeripheral,
                rssi = RSSI.intValue
            )
        }
    }
}
