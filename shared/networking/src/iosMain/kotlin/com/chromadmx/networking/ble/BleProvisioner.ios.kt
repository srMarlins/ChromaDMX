package com.chromadmx.networking.ble

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBCharacteristic
import platform.CoreBluetooth.CBCharacteristicWriteWithResponse
import platform.CoreBluetooth.CBManagerStatePoweredOn
import platform.CoreBluetooth.CBPeripheral
import platform.CoreBluetooth.CBPeripheralDelegateProtocol
import platform.CoreBluetooth.CBService
import platform.CoreBluetooth.CBUUID
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUUID
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.darwin.NSObject
import kotlin.coroutines.resume

/**
 * iOS implementation of [BleProvisioner] using CoreBluetooth.
 *
 * Manages the GATT connection lifecycle with an ESP32 DMX node:
 * connect, discover services/characteristics, read/write configuration,
 * and disconnect.
 *
 * All CoreBluetooth delegate callbacks are bridged to Kotlin coroutines
 * via [suspendCancellableCoroutine]. The implementation uses separate
 * delegate objects for [CBCentralManager] and [CBPeripheral] to handle
 * the asynchronous BLE GATT operations.
 *
 * Requires `NSBluetoothAlwaysUsageDescription` in Info.plist.
 */
@OptIn(ExperimentalForeignApi::class)
actual class BleProvisioner actual constructor() {

    private val _state = MutableStateFlow(ProvisioningState.IDLE)
    actual val state: StateFlow<ProvisioningState> = _state

    /** ChromaDMX GATT service UUID used for service discovery. */
    private val serviceUuid: CBUUID =
        CBUUID.UUIDWithString(GattServiceSpec.CHROMA_DMX.serviceUuid)

    /** Lookup from characteristic name to its CBUUID. */
    private val characteristicUuids: Map<String, CBUUID> =
        GattServiceSpec.CHROMA_DMX.characteristics.mapValues { (_, uuid) ->
            CBUUID.UUIDWithString(uuid)
        }

    /** Reverse lookup from CBUUID string to characteristic name. */
    private val uuidToName: Map<String, String> =
        GattServiceSpec.CHROMA_DMX.characteristics.entries.associate { (name, uuid) ->
            uuid.uppercase() to name
        }

    // --- Connection state ---

    /** The connected CBPeripheral, or null if not connected. */
    private var connectedPeripheral: CBPeripheral? = null

    /** Discovered characteristics keyed by their name (e.g. "node_name"). */
    private var discoveredCharacteristics: MutableMap<String, CBCharacteristic> = mutableMapOf()

    // --- Continuation slots for async delegate callbacks ---

    private var connectContinuation: CancellableContinuation<Boolean>? = null
    private var disconnectContinuation: CancellableContinuation<Unit>? = null
    private var serviceDiscoveryContinuation: CancellableContinuation<Boolean>? = null
    private var characteristicDiscoveryContinuation: CancellableContinuation<Boolean>? = null
    private var readContinuation: CancellableContinuation<NSData?>? = null
    private var writeContinuation: CancellableContinuation<Boolean>? = null

    // --- Delegates ---

    private val centralDelegate = CentralManagerDelegate()
    private val peripheralDelegate = PeripheralDelegate()

    /**
     * The CBCentralManager instance. Created with our delegate so that
     * state-change callbacks are received immediately.
     */
    private val centralManager = CBCentralManager(
        delegate = centralDelegate,
        queue = null // dispatch on main queue
    )

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    actual suspend fun connect(deviceId: String): Boolean {
        _state.value = ProvisioningState.CONNECTING
        discoveredCharacteristics.clear()

        try {
            // Step 1: Retrieve the peripheral by UUID
            val nsuuid = NSUUID(uUIDString = deviceId)
            val peripherals = centralManager.retrievePeripheralsWithIdentifiers(listOf(nsuuid))
            val peripheral = peripherals.firstOrNull() as? CBPeripheral
            if (peripheral == null) {
                _state.value = ProvisioningState.ERROR
                return false
            }

            peripheral.delegate = peripheralDelegate
            connectedPeripheral = peripheral

            // Step 2: Connect to the peripheral
            val connected = suspendCancellableCoroutine { cont ->
                connectContinuation = cont
                cont.invokeOnCancellation {
                    connectContinuation = null
                    centralManager.cancelPeripheralConnection(peripheral)
                }
                centralManager.connectPeripheral(peripheral, options = null)
            }
            if (!connected) {
                _state.value = ProvisioningState.ERROR
                connectedPeripheral = null
                return false
            }

            // Step 3: Discover the ChromaDMX GATT service
            val servicesFound = suspendCancellableCoroutine { cont ->
                serviceDiscoveryContinuation = cont
                cont.invokeOnCancellation { serviceDiscoveryContinuation = null }
                peripheral.discoverServices(listOf(serviceUuid))
            }
            if (!servicesFound) {
                _state.value = ProvisioningState.ERROR
                centralManager.cancelPeripheralConnection(peripheral)
                connectedPeripheral = null
                return false
            }

            // Step 4: Discover characteristics on the service
            val service = peripheral.services
                ?.filterIsInstance<CBService>()
                ?.firstOrNull { it.UUID.UUIDString.uppercase() == serviceUuid.UUIDString.uppercase() }
            if (service == null) {
                _state.value = ProvisioningState.ERROR
                centralManager.cancelPeripheralConnection(peripheral)
                connectedPeripheral = null
                return false
            }

            val characteristicsFound = suspendCancellableCoroutine { cont ->
                characteristicDiscoveryContinuation = cont
                cont.invokeOnCancellation { characteristicDiscoveryContinuation = null }
                peripheral.discoverCharacteristics(
                    characteristicUuids.values.toList(),
                    service
                )
            }
            if (!characteristicsFound) {
                _state.value = ProvisioningState.ERROR
                centralManager.cancelPeripheralConnection(peripheral)
                connectedPeripheral = null
                return false
            }

            return true
        } catch (e: Exception) {
            _state.value = ProvisioningState.ERROR
            connectedPeripheral?.let { centralManager.cancelPeripheralConnection(it) }
            connectedPeripheral = null
            return false
        }
    }

    actual suspend fun readConfig(): NodeConfig? {
        val peripheral = connectedPeripheral ?: return null
        _state.value = ProvisioningState.READING_CONFIG

        try {
            val name = readCharacteristicString(peripheral, "node_name") ?: return null
            val ssid = readCharacteristicString(peripheral, "wifi_ssid") ?: return null
            val password = readCharacteristicString(peripheral, "wifi_password") ?: return null
            val universeStr = readCharacteristicString(peripheral, "universe") ?: return null
            val dmxAddrStr = readCharacteristicString(peripheral, "dmx_start_address") ?: return null

            val universe = universeStr.toIntOrNull() ?: return null
            val dmxStartAddress = dmxAddrStr.toIntOrNull() ?: return null

            return NodeConfig(
                name = name,
                wifiSsid = ssid,
                wifiPassword = password,
                universe = universe,
                dmxStartAddress = dmxStartAddress
            )
        } catch (e: Exception) {
            _state.value = ProvisioningState.ERROR
            return null
        }
    }

    actual suspend fun writeConfig(config: NodeConfig): Boolean {
        val peripheral = connectedPeripheral ?: return false
        _state.value = ProvisioningState.WRITING_CONFIG

        try {
            if (!writeCharacteristicString(peripheral, "node_name", config.name)) return false
            if (!writeCharacteristicString(peripheral, "wifi_ssid", config.wifiSsid)) return false
            if (!writeCharacteristicString(peripheral, "wifi_password", config.wifiPassword)) return false
            if (!writeCharacteristicString(peripheral, "universe", config.universe.toString())) return false
            if (!writeCharacteristicString(peripheral, "dmx_start_address", config.dmxStartAddress.toString())) return false

            return true
        } catch (e: Exception) {
            _state.value = ProvisioningState.ERROR
            return false
        }
    }

    actual suspend fun disconnect() {
        val peripheral = connectedPeripheral ?: run {
            _state.value = ProvisioningState.IDLE
            return
        }

        try {
            suspendCancellableCoroutine { cont ->
                disconnectContinuation = cont
                cont.invokeOnCancellation { disconnectContinuation = null }
                centralManager.cancelPeripheralConnection(peripheral)
            }
        } catch (_: Exception) {
            // Ignore disconnect errors during cleanup
        } finally {
            connectedPeripheral = null
            discoveredCharacteristics.clear()
            _state.value = ProvisioningState.IDLE
        }
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    /**
     * Read a single characteristic value as a UTF-8 string.
     *
     * @param peripheral The connected CBPeripheral
     * @param name The characteristic name from [GattServiceSpec]
     * @return The string value, or null if the read failed
     */
    private suspend fun readCharacteristicString(
        peripheral: CBPeripheral,
        name: String
    ): String? {
        val characteristic = discoveredCharacteristics[name] ?: return null

        val data = suspendCancellableCoroutine { cont ->
            readContinuation = cont
            cont.invokeOnCancellation { readContinuation = null }
            peripheral.readValueForCharacteristic(characteristic)
        }

        return data?.toUtf8String()
    }

    /**
     * Write a UTF-8 string value to a characteristic.
     *
     * Uses [CBCharacteristicWriteWithResponse] to ensure the write is
     * acknowledged by the peripheral.
     *
     * @param peripheral The connected CBPeripheral
     * @param name The characteristic name from [GattServiceSpec]
     * @param value The string value to write
     * @return true if the write was acknowledged successfully
     */
    private suspend fun writeCharacteristicString(
        peripheral: CBPeripheral,
        name: String,
        value: String
    ): Boolean {
        val characteristic = discoveredCharacteristics[name] ?: return false
        val data = stringToNSData(value) ?: return false

        return suspendCancellableCoroutine { cont ->
            writeContinuation = cont
            cont.invokeOnCancellation { writeContinuation = null }
            peripheral.writeValue(data, characteristic, CBCharacteristicWriteWithResponse)
        }
    }

    /**
     * Convert an NSData value to a UTF-8 Kotlin String.
     */
    @OptIn(BetaInteropApi::class)
    private fun NSData.toUtf8String(): String? {
        return NSString.create(data = this, encoding = NSUTF8StringEncoding)?.toString()
    }

    /**
     * Convert a Kotlin String to NSData using UTF-8 encoding.
     */
    @OptIn(BetaInteropApi::class)
    private fun stringToNSData(value: String): NSData? {
        return NSString.create(string = value).dataUsingEncoding(NSUTF8StringEncoding)
    }

    /**
     * Map discovered [CBCharacteristic] instances to their names in
     * [GattServiceSpec.CHROMA_DMX] for convenient lookup.
     */
    private fun indexCharacteristics(characteristics: List<CBCharacteristic>) {
        for (characteristic in characteristics) {
            val uuidStr = characteristic.UUID.UUIDString.uppercase()
            val name = uuidToName[uuidStr]
            if (name != null) {
                discoveredCharacteristics[name] = characteristic
            }
        }
    }

    // ------------------------------------------------------------------
    // CBCentralManagerDelegate
    // ------------------------------------------------------------------

    /**
     * Delegate handling CBCentralManager connection lifecycle callbacks.
     *
     * **Must** extend [NSObject] for Kotlin/Native ObjC protocol conformance.
     */
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    private inner class CentralManagerDelegate : NSObject(), CBCentralManagerDelegateProtocol {

        override fun centralManagerDidUpdateState(central: CBCentralManager) {
            // State changes are monitored but we don't auto-start anything here;
            // connect() checks state inline.
        }

        /**
         * Called when a GATT connection to the peripheral succeeds.
         */
        override fun centralManager(
            central: CBCentralManager,
            didConnectPeripheral: CBPeripheral
        ) {
            connectContinuation?.resume(true)
            connectContinuation = null
        }

        /**
         * Called when a GATT connection attempt fails.
         */
        @ObjCSignatureOverride
        override fun centralManager(
            central: CBCentralManager,
            didFailToConnectPeripheral: CBPeripheral,
            error: platform.Foundation.NSError?
        ) {
            connectContinuation?.resume(false)
            connectContinuation = null
        }

        /**
         * Called when the peripheral disconnects (either requested or unexpected).
         */
        @ObjCSignatureOverride
        override fun centralManager(
            central: CBCentralManager,
            didDisconnectPeripheral: CBPeripheral,
            error: platform.Foundation.NSError?
        ) {
            disconnectContinuation?.resume(Unit)
            disconnectContinuation = null
        }
    }

    // ------------------------------------------------------------------
    // CBPeripheralDelegate
    // ------------------------------------------------------------------

    /**
     * Delegate handling CBPeripheral service/characteristic callbacks.
     *
     * **Must** extend [NSObject] for Kotlin/Native ObjC protocol conformance.
     */
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    private inner class PeripheralDelegate : NSObject(), CBPeripheralDelegateProtocol {

        /**
         * Called when service discovery completes (or fails).
         */
        override fun peripheral(
            peripheral: CBPeripheral,
            didDiscoverServices: platform.Foundation.NSError?
        ) {
            val success = didDiscoverServices == null && peripheral.services?.isNotEmpty() == true
            serviceDiscoveryContinuation?.resume(success)
            serviceDiscoveryContinuation = null
        }

        /**
         * Called when characteristic discovery completes for a service.
         */
        override fun peripheral(
            peripheral: CBPeripheral,
            didDiscoverCharacteristicsForService: CBService,
            error: platform.Foundation.NSError?
        ) {
            if (error == null) {
                val characteristics =
                    didDiscoverCharacteristicsForService.characteristics
                        ?.filterIsInstance<CBCharacteristic>()
                        ?: emptyList()
                indexCharacteristics(characteristics)
            }
            characteristicDiscoveryContinuation?.resume(error == null)
            characteristicDiscoveryContinuation = null
        }

        /**
         * Called when a characteristic read completes.
         */
        @ObjCSignatureOverride
        override fun peripheral(
            peripheral: CBPeripheral,
            didUpdateValueForCharacteristic: CBCharacteristic,
            error: platform.Foundation.NSError?
        ) {
            if (error != null) {
                readContinuation?.resume(null)
            } else {
                readContinuation?.resume(didUpdateValueForCharacteristic.value)
            }
            readContinuation = null
        }

        /**
         * Called when a characteristic write (with response) completes.
         */
        @ObjCSignatureOverride
        override fun peripheral(
            peripheral: CBPeripheral,
            didWriteValueForCharacteristic: CBCharacteristic,
            error: platform.Foundation.NSError?
        ) {
            writeContinuation?.resume(error == null)
            writeContinuation = null
        }
    }
}
