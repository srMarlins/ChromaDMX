# iOS Production Release Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Bring the iOS app to full feature parity with Android and prepare for App Store release.

**Architecture:** Hybrid approach — Kotlin/Native `actual` implementations for platform logic (UDP, BLE, camera, Link) using built-in `platform.*` bindings, plus a thin Swift app shell with native views for camera preview and Metal rendering. Compose Multiplatform provides the shared UI.

**Tech Stack:** Kotlin/Native, platform.Network (C API), platform.CoreBluetooth, platform.AVFoundation, LinkKit (cinterop), SwiftUI, Metal, Koin DI.

**Constraints:** iOS code cannot be compiled on Windows. All Kotlin/Swift files are written on Windows, then compiled and tested on macOS. Test verification commands are marked with `[MAC]`.

---

## Task 1: UDP Transport — Network.framework (C API)

**Files:**
- Modify: `shared/networking/src/iosMain/kotlin/com/chromadmx/networking/transport/UdpTransport.ios.kt`
- Reference: `shared/networking/src/androidMain/kotlin/com/chromadmx/networking/transport/UdpTransport.android.kt`
- Reference: `shared/networking/src/commonMain/kotlin/com/chromadmx/networking/transport/UdpTransport.kt`
- Reference: `shared/networking/src/commonMain/kotlin/com/chromadmx/networking/model/UdpPacket.kt`

**Context:** The `expect class PlatformUdpTransport` has 3 members: `send(data, address, port)`, `receive(buffer, timeoutMs): UdpPacket?`, and `close()`. The Android implementation uses `DatagramSocket` with `Dispatchers.IO`. Kotlin/Native has no `Dispatchers.IO` — use `Dispatchers.Default` or `newSingleThreadContext`. Network.framework's C API is available via `platform.Network.*` (functions like `nw_connection_create`, `nw_parameters_create_udp`, etc.). These are C-level functions, NOT the Swift `NWConnection` class.

**Step 1: Replace the stub with a Network.framework C API implementation**

```kotlin
package com.chromadmx.networking.transport

import com.chromadmx.networking.model.UdpPacket
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.Foundation.*
import platform.Network.*
import platform.posix.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalForeignApi::class)
actual class PlatformUdpTransport actual constructor() {

    private val queue = dispatch_queue_create("com.chromadmx.udp", null)
    private var listener: nw_listener_t? = null
    private val connections = mutableMapOf<String, nw_connection_t>()

    // Receiving state
    private var pendingPackets = ArrayDeque<UdpPacket>()
    private var pendingContinuation: CancellableContinuation<UdpPacket?>? = null

    actual suspend fun send(data: ByteArray, address: String, port: Int) {
        val key = "$address:$port"
        val connection = connections.getOrPut(key) {
            createConnection(address, port)
        }
        suspendCancellableCoroutine { cont ->
            data.usePinned { pinned ->
                val dispatchData = dispatch_data_create(
                    pinned.addressOf(0),
                    data.size.toULong(),
                    queue,
                    null
                )
                nw_connection_send(
                    connection,
                    dispatchData,
                    NW_CONNECTION_DEFAULT_MESSAGE_CONTEXT,
                    true, // isComplete
                    { error ->
                        if (error != null) {
                            cont.resumeWithException(
                                RuntimeException("UDP send failed")
                            )
                        } else {
                            cont.resume(Unit)
                        }
                    }
                )
            }
        }
    }

    actual suspend fun receive(buffer: ByteArray, timeoutMs: Long): UdpPacket? {
        // If we already have a queued packet, return it immediately
        pendingPackets.removeFirstOrNull()?.let { return it }

        // Otherwise wait for one with timeout
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                pendingContinuation = cont
                cont.invokeOnCancellation { pendingContinuation = null }
            }
        }
    }

    actual fun close() {
        listener?.let { nw_listener_cancel(it) }
        listener = null
        connections.values.forEach { nw_connection_cancel(it) }
        connections.clear()
    }

    /**
     * Start a UDP listener on the given port to receive incoming packets.
     * Call this before [receive] to set up the receive path.
     */
    fun startListening(port: UShort) {
        val params = nw_parameters_create_secure_udp(
            NW_PARAMETERS_DISABLE_PROTOCOL, // no DTLS
            NW_PARAMETERS_DEFAULT_CONFIGURATION
        )
        val listener = nw_listener_create_with_port(port.toString(), params)
            ?: throw RuntimeException("Failed to create UDP listener on port $port")

        nw_listener_set_new_connection_handler(listener) { incomingConnection ->
            if (incomingConnection != null) {
                scheduleReceive(incomingConnection)
                nw_connection_set_queue(incomingConnection, queue)
                nw_connection_start(incomingConnection)
            }
        }
        nw_listener_set_queue(listener, queue)
        nw_listener_start(listener)
        this.listener = listener
    }

    private fun createConnection(address: String, port: Int): nw_connection_t {
        val endpoint = nw_endpoint_create_host(address, port.toString())
        val params = nw_parameters_create_secure_udp(
            NW_PARAMETERS_DISABLE_PROTOCOL,
            NW_PARAMETERS_DEFAULT_CONFIGURATION
        )
        val connection = nw_connection_create(endpoint, params)
            ?: throw RuntimeException("Failed to create UDP connection to $address:$port")
        nw_connection_set_queue(connection, queue)
        nw_connection_start(connection)
        return connection
    }

    private fun scheduleReceive(connection: nw_connection_t) {
        nw_connection_receive_message(connection) { content, _, _, error ->
            if (error == null && content != null) {
                val size = dispatch_data_get_size(content).toInt()
                if (size > 0) {
                    val data = ByteArray(size)
                    data.usePinned { pinned ->
                        dispatch_data_apply(content) { _, offset, buffer, bufferSize ->
                            memcpy(
                                pinned.addressOf(offset.toInt()),
                                buffer,
                                bufferSize
                            )
                            true
                        }
                    }

                    // Extract source address from connection metadata
                    val endpoint = nw_connection_copy_endpoint(connection)
                    val host = endpoint?.let {
                        nw_endpoint_get_hostname(it)?.toKString()
                    } ?: ""
                    val port = endpoint?.let {
                        nw_endpoint_get_port(it).toInt()
                    } ?: 0

                    val packet = UdpPacket(data, host, port)

                    // Deliver to waiting coroutine or queue
                    val cont = pendingContinuation
                    if (cont != null) {
                        pendingContinuation = null
                        cont.resume(packet)
                    } else {
                        pendingPackets.addLast(packet)
                    }
                }
            }
            // Schedule next receive
            scheduleReceive(connection)
        }
    }
}
```

> **Note:** The Network.framework C API uses `nw_*` function prefixes. `dispatch_queue_create` provides the dispatch queue for callbacks. `dispatch_data_create` wraps byte arrays for sending. This code will need refinement during Mac compilation — the C API signatures may need casts or `@Suppress` annotations depending on how Kotlin/Native maps the types.

**Step 2: Commit**

```bash
git add shared/networking/src/iosMain/kotlin/com/chromadmx/networking/transport/UdpTransport.ios.kt
git commit -m "feat(ios): implement UDP transport via Network.framework C API"
```

**Step 3: `[MAC]` Compile and verify**

```bash
./gradlew :shared:networking:compileKotlinIosSimulatorArm64
```

Expected: BUILD SUCCESS. Fix any type mapping issues from the C API bindings.

---

## Task 2: BLE Scanner — CoreBluetooth

**Files:**
- Modify: `shared/networking/src/iosMain/kotlin/com/chromadmx/networking/ble/BleScanner.ios.kt`
- Reference: `shared/networking/src/commonMain/kotlin/com/chromadmx/networking/ble/BleScanner.kt`
- Reference: `shared/networking/src/commonMain/kotlin/com/chromadmx/networking/ble/BleModels.kt`

**Context:** `CBCentralManager` and delegates are available via `platform.CoreBluetooth.*`. Delegate classes must extend `NSObject()` and implement `CBCentralManagerDelegateProtocol`. The ChromaDMX service UUID is `4368726f-6d61-444d-5800-000000000001`. On iOS, `BleNode.deviceId` is the peripheral's UUID (not MAC address).

**Step 1: Replace the stub with CoreBluetooth implementation**

```kotlin
package com.chromadmx.networking.ble

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.CoreBluetooth.*
import platform.Foundation.NSNumber
import platform.Foundation.NSUUID
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
actual class BleScanner actual constructor() : NSObject(), CBCentralManagerDelegateProtocol {

    private val _discoveredNodes = MutableStateFlow<List<BleNode>>(emptyList())
    actual val discoveredNodes: StateFlow<List<BleNode>> = _discoveredNodes

    private val _isScanning = MutableStateFlow(false)
    actual val isScanning: StateFlow<Boolean> = _isScanning

    private val centralManager = CBCentralManager(delegate = this, queue = null)
    private val serviceUUID = CBUUID.UUIDWithString(GattServiceSpec.CHROMA_DMX.serviceUuid)
    private val nodesMap = mutableMapOf<String, BleNode>()

    actual fun startScan() {
        if (centralManager.state == CBManagerStatePoweredOn) {
            centralManager.scanForPeripheralsWithServices(
                serviceUUIDs = listOf(serviceUUID),
                options = mapOf(
                    CBCentralManagerScanOptionAllowDuplicatesKey to true
                )
            )
            _isScanning.value = true
        }
    }

    actual fun stopScan() {
        centralManager.stopScan()
        _isScanning.value = false
    }

    // --- CBCentralManagerDelegateProtocol ---

    override fun centralManagerDidUpdateState(central: CBCentralManager) {
        // If BLE just became available and we were trying to scan, start now
        if (central.state == CBManagerStatePoweredOn && _isScanning.value) {
            startScan()
        }
    }

    override fun centralManager(
        central: CBCentralManager,
        didDiscoverPeripheral: CBPeripheral,
        advertisementData: Map<Any?, *>,
        RSSI: NSNumber
    ) {
        val deviceId = didDiscoverPeripheral.identifier.UUIDString
        val name = didDiscoverPeripheral.name
        val rssi = RSSI.intValue

        // Check if the advertisement contains the provisioned flag
        val isProvisioned = false // Will be determined after GATT connection

        val node = BleNode(
            deviceId = deviceId,
            name = name,
            rssi = rssi,
            isProvisioned = isProvisioned
        )
        nodesMap[deviceId] = node
        _discoveredNodes.value = nodesMap.values.toList()
    }
}
```

**Step 2: Commit**

```bash
git add shared/networking/src/iosMain/kotlin/com/chromadmx/networking/ble/BleScanner.ios.kt
git commit -m "feat(ios): implement BLE scanner via CoreBluetooth CBCentralManager"
```

**Step 3: `[MAC]` Compile and verify**

```bash
./gradlew :shared:networking:compileKotlinIosSimulatorArm64
```

Expected: BUILD SUCCESS. The `CBCentralManagerDelegateProtocol` method signatures must exactly match what Kotlin/Native generates from the ObjC headers — adjust parameter names if the compiler complains about missing override.

---

## Task 3: BLE Provisioner — CoreBluetooth GATT

**Files:**
- Modify: `shared/networking/src/iosMain/kotlin/com/chromadmx/networking/ble/BleProvisioner.ios.kt`
- Reference: `shared/networking/src/commonMain/kotlin/com/chromadmx/networking/ble/BleProvisioner.kt`
- Reference: `shared/networking/src/commonMain/kotlin/com/chromadmx/networking/ble/BleModels.kt`

**Context:** Uses `CBPeripheral` + `CBPeripheralDelegateProtocol` for GATT operations. Must also use `CBCentralManager` to initiate the connection. Characteristic UUIDs come from `GattServiceSpec.CHROMA_DMX.characteristics`. All delegate callbacks are asynchronous — bridge to coroutines with `suspendCancellableCoroutine`.

**Step 1: Replace the stub with CoreBluetooth GATT implementation**

```kotlin
package com.chromadmx.networking.ble

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.CoreBluetooth.*
import platform.Foundation.*
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalForeignApi::class)
actual class BleProvisioner actual constructor() : NSObject(),
    CBCentralManagerDelegateProtocol,
    CBPeripheralDelegateProtocol {

    private val _state = MutableStateFlow(ProvisioningState.IDLE)
    actual val state: StateFlow<ProvisioningState> = _state

    private val centralManager = CBCentralManager(delegate = this, queue = null)
    private var peripheral: CBPeripheral? = null
    private var chromaService: CBService? = null
    private val characteristics = mutableMapOf<String, CBCharacteristic>()

    // Continuation holders for async delegate callbacks
    private var connectContinuation: ((Boolean) -> Unit)? = null
    private var discoverServicesContinuation: ((Boolean) -> Unit)? = null
    private var discoverCharsContinuation: ((Boolean) -> Unit)? = null
    private var readContinuation: ((NSData?) -> Unit)? = null
    private var writeContinuation: ((Boolean) -> Unit)? = null

    private val serviceUUID = CBUUID.UUIDWithString(GattServiceSpec.CHROMA_DMX.serviceUuid)

    actual suspend fun connect(deviceId: String): Boolean {
        _state.value = ProvisioningState.CONNECTING

        // Retrieve peripheral by UUID
        val uuid = NSUUID(UUIDString = deviceId)
        val peripherals = centralManager.retrievePeripheralsWithIdentifiers(listOf(uuid))
        val target = peripherals.firstOrNull() as? CBPeripheral ?: run {
            _state.value = ProvisioningState.ERROR
            return false
        }

        peripheral = target
        target.delegate = this

        // Connect
        val connected = suspendCoroutine { cont ->
            connectContinuation = { cont.resume(it) }
            centralManager.connectPeripheral(target, options = null)
        }
        if (!connected) {
            _state.value = ProvisioningState.ERROR
            return false
        }

        // Discover ChromaDMX service
        val servicesFound = suspendCoroutine { cont ->
            discoverServicesContinuation = { cont.resume(it) }
            target.discoverServices(listOf(serviceUUID))
        }
        if (!servicesFound) {
            _state.value = ProvisioningState.ERROR
            return false
        }

        // Discover characteristics
        val charsFound = suspendCoroutine { cont ->
            discoverCharsContinuation = { cont.resume(it) }
            chromaService?.let { target.discoverCharacteristics(null, it) }
                ?: run { cont.resume(false) }
        }
        if (!charsFound) {
            _state.value = ProvisioningState.ERROR
            return false
        }

        return true
    }

    actual suspend fun readConfig(): NodeConfig? {
        _state.value = ProvisioningState.READING_CONFIG
        val target = peripheral ?: return null

        val spec = GattServiceSpec.CHROMA_DMX.characteristics
        val name = readCharacteristic(target, spec["node_name"]!!) ?: return null
        val ssid = readCharacteristic(target, spec["wifi_ssid"]!!) ?: return null
        val password = readCharacteristic(target, spec["wifi_password"]!!) ?: return null
        val universe = readCharacteristic(target, spec["universe"]!!) ?: return null
        val startAddr = readCharacteristic(target, spec["dmx_start_address"]!!) ?: return null

        return NodeConfig(
            name = name,
            wifiSsid = ssid,
            wifiPassword = password,
            universe = universe.toIntOrNull() ?: 0,
            dmxStartAddress = startAddr.toIntOrNull() ?: 1
        )
    }

    actual suspend fun writeConfig(config: NodeConfig): Boolean {
        _state.value = ProvisioningState.WRITING_CONFIG
        val target = peripheral ?: return false

        val spec = GattServiceSpec.CHROMA_DMX.characteristics
        if (!writeCharacteristic(target, spec["node_name"]!!, config.name)) return false
        if (!writeCharacteristic(target, spec["wifi_ssid"]!!, config.wifiSsid)) return false
        if (!writeCharacteristic(target, spec["wifi_password"]!!, config.wifiPassword)) return false
        if (!writeCharacteristic(target, spec["universe"]!!, config.universe.toString())) return false
        if (!writeCharacteristic(target, spec["dmx_start_address"]!!, config.dmxStartAddress.toString())) return false

        _state.value = ProvisioningState.SUCCESS
        return true
    }

    actual suspend fun disconnect() {
        peripheral?.let { centralManager.cancelPeripheralConnection(it) }
        peripheral = null
        chromaService = null
        characteristics.clear()
        _state.value = ProvisioningState.IDLE
    }

    // --- Helpers ---

    private suspend fun readCharacteristic(peripheral: CBPeripheral, uuid: String): String? {
        val char = characteristics[uuid] ?: return null
        val data = suspendCoroutine { cont ->
            readContinuation = { cont.resume(it) }
            peripheral.readValueForCharacteristic(char)
        }
        return data?.let {
            NSString.create(data = it, encoding = NSUTF8StringEncoding) as? String
        }
    }

    private suspend fun writeCharacteristic(
        peripheral: CBPeripheral,
        uuid: String,
        value: String
    ): Boolean {
        val char = characteristics[uuid] ?: return false
        val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return false
        return suspendCoroutine { cont ->
            writeContinuation = { cont.resume(it) }
            peripheral.writeValue(data, char, CBCharacteristicWriteWithResponse)
        }
    }

    // --- CBCentralManagerDelegateProtocol ---

    override fun centralManagerDidUpdateState(central: CBCentralManager) {}

    override fun centralManager(
        central: CBCentralManager,
        didConnectPeripheral: CBPeripheral
    ) {
        connectContinuation?.invoke(true)
        connectContinuation = null
    }

    override fun centralManager(
        central: CBCentralManager,
        didFailToConnectPeripheral: CBPeripheral,
        error: NSError?
    ) {
        connectContinuation?.invoke(false)
        connectContinuation = null
    }

    // --- CBPeripheralDelegateProtocol ---

    override fun peripheral(peripheral: CBPeripheral, didDiscoverServices: NSError?) {
        chromaService = peripheral.services?.firstOrNull {
            (it as? CBService)?.UUID == serviceUUID
        } as? CBService
        discoverServicesContinuation?.invoke(chromaService != null)
        discoverServicesContinuation = null
    }

    override fun peripheral(
        peripheral: CBPeripheral,
        didDiscoverCharacteristicsForService: CBService,
        error: NSError?
    ) {
        didDiscoverCharacteristicsForService.characteristics?.forEach { char ->
            val cbChar = char as CBCharacteristic
            characteristics[cbChar.UUID.UUIDString] = cbChar
        }
        discoverCharsContinuation?.invoke(error == null)
        discoverCharsContinuation = null
    }

    override fun peripheral(
        peripheral: CBPeripheral,
        didUpdateValueForCharacteristic: CBCharacteristic,
        error: NSError?
    ) {
        readContinuation?.invoke(didUpdateValueForCharacteristic.value)
        readContinuation = null
    }

    override fun peripheral(
        peripheral: CBPeripheral,
        didWriteValueForCharacteristic: CBCharacteristic,
        error: NSError?
    ) {
        writeContinuation?.invoke(error == null)
        writeContinuation = null
    }
}
```

**Step 2: Commit**

```bash
git add shared/networking/src/iosMain/kotlin/com/chromadmx/networking/ble/BleProvisioner.ios.kt
git commit -m "feat(ios): implement BLE provisioner via CoreBluetooth GATT"
```

**Step 3: `[MAC]` Compile and verify**

```bash
./gradlew :shared:networking:compileKotlinIosSimulatorArm64
```

Expected: BUILD SUCCESS. Watch for ObjC delegate method name mapping — Kotlin/Native generates parameter labels from ObjC selectors, so `didDiscoverPeripheral:` becomes the second parameter name in the Kotlin override.

---

## Task 4: Camera Source — AVFoundation

**Files:**
- Modify: `shared/vision/src/iosMain/kotlin/com/chromadmx/vision/camera/CameraSource.ios.kt`
- Reference: `shared/vision/src/commonMain/kotlin/com/chromadmx/vision/camera/CameraSource.kt`
- Reference: `shared/vision/src/commonMain/kotlin/com/chromadmx/vision/camera/GrayscaleFrame.kt`
- Reference: `android/app/src/main/kotlin/com/chromadmx/android/CameraFrameCapture.kt` (Android equivalent)

**Context:** `AVCaptureSession`, `AVCaptureVideoDataOutput`, etc. are available via `platform.AVFoundation.*`. The delegate protocol is `AVCaptureVideoDataOutputSampleBufferDelegateProtocol`. Extract the Y (luminance) plane from the `CMSampleBuffer` → `CVPixelBuffer` and normalize to `[0.0, 1.0]` FloatArray. Target resolution: 320x240.

**Step 1: Replace the stub with AVFoundation implementation**

```kotlin
package com.chromadmx.vision.camera

import kotlinx.cinterop.*
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.*
import platform.CoreMedia.*
import platform.CoreVideo.*
import platform.Foundation.*
import platform.darwin.NSObject
import platform.darwin.dispatch_queue_create
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class PlatformCameraSource : NSObject(),
    AVCaptureVideoDataOutputSampleBufferDelegateProtocol {

    private val captureSession = AVCaptureSession()
    private val processingQueue = dispatch_queue_create("com.chromadmx.camera", null)

    private var latestFrame: GrayscaleFrame? = null
    private var frameContinuation: ((GrayscaleFrame) -> Unit)? = null

    private var isConfigured = false

    actual suspend fun captureFrame(): GrayscaleFrame {
        if (!isConfigured) configureCaptureSession()
        if (!captureSession.isRunning()) captureSession.startRunning()

        // Return latest frame or wait for next one
        latestFrame?.let { return it }
        return suspendCancellableCoroutine { cont ->
            frameContinuation = { frame ->
                cont.resume(frame)
            }
            cont.invokeOnCancellation { frameContinuation = null }
        }
    }

    actual fun startPreview() {
        if (!isConfigured) configureCaptureSession()
        captureSession.startRunning()
    }

    actual fun stopPreview() {
        captureSession.stopRunning()
    }

    private fun configureCaptureSession() {
        captureSession.beginConfiguration()
        captureSession.sessionPreset = AVCaptureSessionPreset352x288 // Closest to 320x240

        // Input: back camera
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) ?: return
        val input = AVCaptureDeviceInput.deviceInputWithDevice(device, error = null) ?: return
        if (captureSession.canAddInput(input)) {
            captureSession.addInput(input)
        }

        // Output: video data
        val output = AVCaptureVideoDataOutput().apply {
            videoSettings = mapOf(
                kCVPixelBufferPixelFormatTypeKey to kCVPixelFormatType_420YpCbCr8BiPlanarFullRange
            )
            alwaysDiscardsLateVideoFrames = true
            setSampleBufferDelegate(this@PlatformCameraSource, queue = processingQueue)
        }
        if (captureSession.canAddOutput(output)) {
            captureSession.addOutput(output)
        }

        captureSession.commitConfiguration()
        isConfigured = true
    }

    // --- AVCaptureVideoDataOutputSampleBufferDelegateProtocol ---

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputSampleBuffer: CMSampleBufferRef?,
        fromConnection: AVCaptureConnection
    ) {
        val sampleBuffer = didOutputSampleBuffer ?: return
        val pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) ?: return

        CVPixelBufferLockBaseAddress(pixelBuffer, kCVPixelBufferLock_ReadOnly.toULong())

        val yPlane = CVPixelBufferGetBaseAddressOfPlane(pixelBuffer, 0u)
        val width = CVPixelBufferGetWidthOfPlane(pixelBuffer, 0u).toInt()
        val height = CVPixelBufferGetHeightOfPlane(pixelBuffer, 0u).toInt()
        val bytesPerRow = CVPixelBufferGetBytesPerRowOfPlane(pixelBuffer, 0u).toInt()

        if (yPlane != null && width > 0 && height > 0) {
            val pixels = FloatArray(width * height)
            val bytePtr = yPlane.reinterpret<UByteVar>()

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val byte = bytePtr[y * bytesPerRow + x]
                    pixels[y * width + x] = byte.toFloat() / 255f
                }
            }

            val frame = GrayscaleFrame(pixels, width, height)
            latestFrame = frame

            // Deliver to waiting coroutine
            frameContinuation?.let { callback ->
                frameContinuation = null
                callback(frame)
            }
        }

        CVPixelBufferUnlockBaseAddress(pixelBuffer, kCVPixelBufferLock_ReadOnly.toULong())
    }
}
```

**Step 2: Commit**

```bash
git add shared/vision/src/iosMain/kotlin/com/chromadmx/vision/camera/CameraSource.ios.kt
git commit -m "feat(ios): implement camera source via AVFoundation grayscale extraction"
```

**Step 3: `[MAC]` Compile and verify**

```bash
./gradlew :shared:vision:compileKotlinIosSimulatorArm64
```

Expected: BUILD SUCCESS. The `kCVPixelBufferPixelFormatTypeKey` and `kCVPixelFormatType_420YpCbCr8BiPlanarFullRange` constants are from `platform.CoreVideo.*`.

---

## Task 5: LinkSession — LinkKit cinterop

**Files:**
- Create: `shared/tempo/src/nativeInterop/cinterop/ableton_link.def`
- Modify: `shared/tempo/build.gradle.kts` (add cinterop configuration)
- Modify: `shared/tempo/src/iosMain/kotlin/com/chromadmx/tempo/link/LinkSession.ios.kt`
- Reference: `shared/tempo/src/commonMain/kotlin/com/chromadmx/tempo/link/LinkSessionApi.kt`

**Context:** LinkKit provides ObjC headers (`ABLLink.h`, `ABLLinkSettingsViewController.h`). The C API uses `ABLLinkRef` as an opaque pointer. Functions: `ABLLinkNew(bpm)`, `ABLLinkDelete(ref)`, `ABLLinkSetActive(ref, bool)`, `ABLLinkIsEnabled(ref)`, `ABLLinkGetNumPeers(ref)`, `ABLLinkCaptureAppSessionState(ref)`, `ABLLinkGetTempo(state)`, `ABLLinkGetBeatAtTime(state, hostTime, quantum)`, `ABLLinkSetTempo(state, bpm, hostTime)`, `ABLLinkCommitAppSessionState(ref, state)`. The LinkKit.xcframework must be downloaded from https://github.com/Ableton/LinkKit/releases and placed at `ios/Frameworks/LinkKit.xcframework/`.

**Step 1: Create the cinterop definition file**

Create `shared/tempo/src/nativeInterop/cinterop/ableton_link.def`:

```
language = Objective-C
headers = ABLLink.h ABLLinkSettingsViewController.h
headerFilter = ABL*
linkerOpts = -framework LinkKit
```

**Step 2: Add cinterop configuration to tempo build.gradle.kts**

Modify `shared/tempo/build.gradle.kts`:

```kotlin
plugins {
    id("chromadmx.kmp.library")
}

kotlin {
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { target ->
        target.compilations["main"].cinterops {
            create("abletonLink") {
                defFile = file("src/nativeInterop/cinterop/ableton_link.def")
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":shared:core"))
        }
    }
}
```

> **Note:** The iOS targets (`iosX64`, `iosArm64`, `iosSimulatorArm64`) are already defined by the `chromadmx.kmp.library` convention plugin. Redeclaring them here is safe — Kotlin merges the target config. The `cinterops` block is the addition.

**Step 3: Replace the LinkSession stub with real implementation**

```kotlin
package com.chromadmx.tempo.link

import abletonLink.* // Generated by cinterop from the .def file
import kotlinx.cinterop.*
import platform.Foundation.NSProcessInfo

@OptIn(ExperimentalForeignApi::class)
actual class LinkSession actual constructor() : LinkSessionApi {

    private val ref: ABLLinkRef = ABLLinkNew(120.0)
    private var _enabled = false

    actual override fun enable() {
        ABLLinkSetActive(ref, true)
        _enabled = true
    }

    actual override fun disable() {
        ABLLinkSetActive(ref, false)
        _enabled = false
    }

    actual override val isEnabled: Boolean
        get() = ABLLinkIsEnabled(ref)

    actual override val peerCount: Int
        get() = ABLLinkGetNumPeers(ref).toInt()

    actual override val bpm: Double
        get() {
            val state = ABLLinkCaptureAppSessionState(ref)
            return ABLLinkGetTempo(state)
        }

    actual override val beatPhase: Double
        get() {
            val state = ABLLinkCaptureAppSessionState(ref)
            val hostTime = hostTimeNanos()
            return ABLLinkGetBeatAtTime(state, hostTime, 1.0) % 1.0
        }

    actual override val barPhase: Double
        get() {
            val state = ABLLinkCaptureAppSessionState(ref)
            val hostTime = hostTimeNanos()
            val beatInBar = ABLLinkGetBeatAtTime(state, hostTime, 4.0) % 4.0
            return beatInBar / 4.0
        }

    actual override fun requestBpm(bpm: Double) {
        val state = ABLLinkCaptureAppSessionState(ref)
        val hostTime = hostTimeNanos()
        ABLLinkSetTempo(state, bpm, hostTime)
        ABLLinkCommitAppSessionState(ref, state)
    }

    actual override fun close() {
        ABLLinkSetActive(ref, false)
        ABLLinkDelete(ref)
    }

    private fun hostTimeNanos(): ULong {
        // mach_absolute_time() gives the host time in Mach time units
        return mach_absolute_time()
    }
}
```

> **Note:** The exact generated import path depends on how Kotlin/Native processes the cinterop. It may be `abletonLink.*` or `cinterop.abletonLink.*`. The `ABLLinkRef` type, `ABLLinkCaptureAppSessionState`, etc. must match the generated bindings. **This task requires the LinkKit headers to be present on the Mac at build time.** The `mach_absolute_time()` function is available from `platform.darwin.*`.

**Step 4: Commit**

```bash
git add shared/tempo/src/nativeInterop/cinterop/ableton_link.def
git add shared/tempo/build.gradle.kts
git add shared/tempo/src/iosMain/kotlin/com/chromadmx/tempo/link/LinkSession.ios.kt
git commit -m "feat(ios): implement Ableton Link session via LinkKit cinterop"
```

**Step 5: `[MAC]` Download LinkKit and compile**

```bash
# Download LinkKit from GitHub releases
cd ios && mkdir -p Frameworks
# Download latest LinkKit.xcframework from https://github.com/Ableton/LinkKit/releases
# Place at ios/Frameworks/LinkKit.xcframework/

./gradlew :shared:tempo:compileKotlinIosSimulatorArm64
```

Expected: BUILD SUCCESS. If cinterop header resolution fails, update the `.def` file with explicit `compilerOpts` pointing to the LinkKit header directory.

---

## Task 6: Koin DI — Complete iOS Module + IosApp.initialize()

**Files:**
- Modify: `shared/src/iosMain/kotlin/com/chromadmx/di/IosDiModule.kt`
- Modify: `shared/src/iosMain/kotlin/com/chromadmx/ios/IosApp.kt`
- Reference: `android/app/src/main/kotlin/com/chromadmx/android/ChromaDMXApp.kt`
- Reference: `shared/src/commonMain/kotlin/com/chromadmx/di/ChromaDiModule.kt`

**Context:** The Android app registers `FileStorage`, `DriverFactory`, `BleScanner`, `BleProvisioner`, and `AgentConfig` in its platform module, then starts Koin with `chromaDiModule + agentModule + androidPlatformModule + uiModule`. The iOS equivalent must do the same.

**Step 1: Complete IosDiModule with all platform registrations**

```kotlin
package com.chromadmx.di

import com.chromadmx.agent.config.AgentConfig
import com.chromadmx.agent.config.ApiKeyProvider
import com.chromadmx.core.db.DriverFactory
import com.chromadmx.core.persistence.FileStorage
import com.chromadmx.core.persistence.IosFileStorage
import com.chromadmx.networking.ble.BleProvisioner
import com.chromadmx.networking.ble.BleScanner
import org.koin.core.module.Module
import org.koin.dsl.module

val iosPlatformModule: Module = module {
    single<FileStorage> { IosFileStorage() }
    single { DriverFactory() }
    single { BleScanner() }
    single { BleProvisioner() }
    single {
        val provider = ApiKeyProvider()
        val googleKey = provider.getGoogleKey() ?: ""
        val anthropicKey = provider.getAnthropicKey() ?: ""
        when {
            googleKey.isNotBlank() -> AgentConfig(apiKey = googleKey, modelId = "gemini_2_5_flash")
            anthropicKey.isNotBlank() -> AgentConfig(apiKey = anthropicKey, modelId = "sonnet_4_5")
            else -> AgentConfig()
        }
    }
}
```

**Step 2: Complete IosApp.initialize() with Koin startup**

```kotlin
package com.chromadmx.ios

import com.chromadmx.agent.di.agentModule
import com.chromadmx.di.chromaDiModule
import com.chromadmx.di.iosPlatformModule
import com.chromadmx.ui.di.uiModule
import org.koin.core.context.startKoin

object IosApp {

    fun initialize() {
        startKoin {
            modules(chromaDiModule, agentModule, iosPlatformModule, uiModule)
        }
    }

    fun greeting(): String = com.chromadmx.Greeting().greet()
}
```

**Step 3: Commit**

```bash
git add shared/src/iosMain/kotlin/com/chromadmx/di/IosDiModule.kt
git add shared/src/iosMain/kotlin/com/chromadmx/ios/IosApp.kt
git commit -m "feat(ios): complete Koin DI module and app initialization"
```

**Step 4: `[MAC]` Compile full shared framework**

```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

Expected: BUILD SUCCESS. This validates all iOS actual implementations resolve and Koin wiring compiles.

---

## Task 7: Delete duplicate stub file

**Files:**
- Delete: `shared/src/iosMain/kotlin/com/chromadmx/platform/IosUdpTransport.kt`
- Delete: `shared/src/iosMain/kotlin/com/chromadmx/platform/IosCameraSource.kt`
- Delete: `shared/src/iosMain/kotlin/com/chromadmx/platform/IosAbletonLinkBridge.kt`

**Context:** These files in `shared/src/iosMain/kotlin/com/chromadmx/platform/` are old stubs that predate the module-level `actual` implementations. They are NOT the `actual` classes — those live in `shared/networking/src/iosMain/`, `shared/vision/src/iosMain/`, and `shared/tempo/src/iosMain/`. These stub files will cause confusion and potential compile conflicts. Delete them.

**Step 1: Delete the old stubs**

```bash
git rm shared/src/iosMain/kotlin/com/chromadmx/platform/IosUdpTransport.kt
git rm shared/src/iosMain/kotlin/com/chromadmx/platform/IosCameraSource.kt
git rm shared/src/iosMain/kotlin/com/chromadmx/platform/IosAbletonLinkBridge.kt
```

**Step 2: Commit**

```bash
git commit -m "chore(ios): remove obsolete platform stubs replaced by actual implementations"
```

---

## Task 8: Swift App Shell — Entry Point

**Files:**
- Create: `ios/ChromaDMX/ChromaDMXApp.swift`
- Create: `ios/ChromaDMX/ComposeView.swift`
- Create: `ios/ChromaDMX/Info.plist`
- Create: `ios/ChromaDMX/ChromaDMX.entitlements`

**Context:** The Swift shell is minimal. `ChromaDMXApp.swift` is the `@main` entry that calls `IosApp.shared.initialize()`. `ComposeView.swift` wraps the Kotlin `MainViewController()` in a `UIViewControllerRepresentable`. These are text files that will be compiled on the Mac.

**Step 1: Create ChromaDMXApp.swift**

```swift
import SwiftUI
import Shared

@main
struct ChromaDMXApp: App {
    init() {
        IosApp.shared.initialize()
    }

    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea(.all)
        }
    }
}
```

**Step 2: Create ComposeView.swift**

```swift
import SwiftUI
import Shared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // Compose manages its own state — no updates needed
    }
}
```

**Step 3: Create Info.plist**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleName</key>
    <string>ChromaDMX</string>
    <key>CFBundleDisplayName</key>
    <string>ChromaDMX</string>
    <key>CFBundleIdentifier</key>
    <string>com.chromadmx.ChromaDMX</string>
    <key>CFBundleVersion</key>
    <string>1</string>
    <key>CFBundleShortVersionString</key>
    <string>0.1.0</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleExecutable</key>
    <string>$(EXECUTABLE_NAME)</string>
    <key>LSRequiresIPhoneOS</key>
    <true/>
    <key>UILaunchScreen</key>
    <dict/>
    <key>UISupportedInterfaceOrientations</key>
    <array>
        <string>UIInterfaceOrientationPortrait</string>
        <string>UIInterfaceOrientationLandscapeLeft</string>
        <string>UIInterfaceOrientationLandscapeRight</string>
    </array>
    <key>UIRequiresFullScreen</key>
    <false/>

    <!-- Permissions -->
    <key>NSCameraUsageDescription</key>
    <string>ChromaDMX uses the camera to detect and map fixture positions in your venue.</string>
    <key>NSLocalNetworkUsageDescription</key>
    <string>ChromaDMX communicates with DMX lighting nodes on your local network.</string>
    <key>NSBonjourServices</key>
    <array>
        <string>_artnet._udp</string>
        <string>_sacn._udp</string>
    </array>
    <key>NSBluetoothAlwaysUsageDescription</key>
    <string>ChromaDMX uses Bluetooth to discover and configure ESP32 DMX nodes.</string>
</dict>
</plist>
```

**Step 4: Create ChromaDMX.entitlements**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>com.apple.developer.networking.multicast</key>
    <true/>
</dict>
</plist>
```

**Step 5: Commit**

```bash
git add ios/ChromaDMX/ChromaDMXApp.swift
git add ios/ChromaDMX/ComposeView.swift
git add ios/ChromaDMX/Info.plist
git add ios/ChromaDMX/ChromaDMX.entitlements
git commit -m "feat(ios): add Swift app shell — entry point, ComposeView, Info.plist, entitlements"
```

---

## Task 9: Swift Native Views — Camera Preview

**Files:**
- Create: `ios/ChromaDMX/NativeViews/CameraPreview.swift`

**Context:** This provides the live camera preview in the UI (the Kotlin `PlatformCameraSource` only does frame analysis — it doesn't render a preview). Uses `AVCaptureVideoPreviewLayer` inside a `UIViewController`, wrapped for SwiftUI via `UIViewControllerRepresentable`.

**Step 1: Create CameraPreview.swift**

```swift
import SwiftUI
import AVFoundation

struct CameraPreview: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> CameraPreviewController {
        CameraPreviewController()
    }

    func updateUIViewController(_ uiViewController: CameraPreviewController, context: Context) {}
}

class CameraPreviewController: UIViewController {
    private let captureSession = AVCaptureSession()
    private var previewLayer: AVCaptureVideoPreviewLayer?

    override func viewDidLoad() {
        super.viewDidLoad()
        configureCaptureSession()
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        previewLayer?.frame = view.bounds
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            self?.captureSession.startRunning()
        }
    }

    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        captureSession.stopRunning()
    }

    private func configureCaptureSession() {
        captureSession.beginConfiguration()
        captureSession.sessionPreset = .medium

        guard let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back),
              let input = try? AVCaptureDeviceInput(device: device),
              captureSession.canAddInput(input) else {
            captureSession.commitConfiguration()
            return
        }

        captureSession.addInput(input)

        let layer = AVCaptureVideoPreviewLayer(session: captureSession)
        layer.videoGravity = .resizeAspectFill
        layer.frame = view.bounds
        view.layer.addSublayer(layer)
        previewLayer = layer

        captureSession.commitConfiguration()
    }
}
```

**Step 2: Commit**

```bash
git add ios/ChromaDMX/NativeViews/CameraPreview.swift
git commit -m "feat(ios): add AVFoundation camera preview native view"
```

---

## Task 10: Swift Native Views — Metal Venue Renderer

**Files:**
- Create: `ios/ChromaDMX/NativeViews/MetalVenueView.swift`

**Context:** Mirrors Android's `VenueRenderer.kt` which is currently a scaffold (empty `onDrawFrame`). This is a Metal-based renderer that clears to a dark background. Fixture dot rendering will be added later as the venue visualization feature is built out.

**Step 1: Create MetalVenueView.swift**

```swift
import SwiftUI
import MetalKit

struct MetalVenueView: UIViewRepresentable {
    func makeUIView(context: Context) -> MTKView {
        let view = MTKView()
        view.device = MTLCreateSystemDefaultDevice()
        view.clearColor = MTLClearColor(red: 0.05, green: 0.05, blue: 0.08, alpha: 1.0)
        view.delegate = context.coordinator
        view.preferredFramesPerSecond = 60
        view.enableSetNeedsDisplay = false
        view.isPaused = false
        return view
    }

    func updateUIView(_ uiView: MTKView, context: Context) {}

    func makeCoordinator() -> VenueRenderer {
        VenueRenderer()
    }
}

class VenueRenderer: NSObject, MTKViewDelegate {
    private var commandQueue: MTLCommandQueue?

    override init() {
        super.init()
    }

    func mtkView(_ view: MTKView, drawableSizeWillChange size: CGSize) {
        // Handle resize if needed
    }

    func draw(in view: MTKView) {
        guard let device = view.device else { return }
        if commandQueue == nil {
            commandQueue = device.makeCommandQueue()
        }

        guard let drawable = view.currentDrawable,
              let descriptor = view.currentRenderPassDescriptor,
              let commandBuffer = commandQueue?.makeCommandBuffer(),
              let encoder = commandBuffer.makeRenderCommandEncoder(descriptor: descriptor) else {
            return
        }

        // Scaffold: just clear to dark background
        // TODO: Render fixture dots as colored circles
        // TODO: Render wireframe venue outline

        encoder.endEncoding()
        commandBuffer.present(drawable)
        commandBuffer.commit()
    }
}
```

**Step 2: Commit**

```bash
git add ios/ChromaDMX/NativeViews/MetalVenueView.swift
git commit -m "feat(ios): add Metal venue renderer scaffold (matches Android VenueRenderer)"
```

---

## Task 11: Swift Permissions Helper

**Files:**
- Create: `ios/ChromaDMX/Platform/Permissions.swift`

**Context:** iOS requires runtime permission requests for camera, BLE, and local network. This helper centralizes permission checking and requesting so the app can check status before attempting to use these features.

**Step 1: Create Permissions.swift**

```swift
import AVFoundation
import CoreBluetooth

enum PermissionStatus {
    case granted
    case denied
    case notDetermined
}

class PermissionsHelper {
    static func cameraStatus() -> PermissionStatus {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized: return .granted
        case .denied, .restricted: return .denied
        case .notDetermined: return .notDetermined
        @unknown default: return .notDetermined
        }
    }

    static func requestCamera(completion: @escaping (Bool) -> Void) {
        AVCaptureDevice.requestAccess(for: .video) { granted in
            DispatchQueue.main.async {
                completion(granted)
            }
        }
    }

    // Note: BLE permissions are handled by CBCentralManager automatically.
    // When CBCentralManager is initialized, iOS prompts for BLE access.
    // Local network access is prompted automatically on first network use.
}
```

**Step 2: Commit**

```bash
git add ios/ChromaDMX/Platform/Permissions.swift
git commit -m "feat(ios): add permissions helper for camera, BLE, network"
```

---

## Task 12: App Icon Assets

**Files:**
- Create: `ios/ChromaDMX/Assets.xcassets/Contents.json`
- Create: `ios/ChromaDMX/Assets.xcassets/AppIcon.appiconset/Contents.json`
- Create: `ios/ChromaDMX/Assets.xcassets/AccentColor.colorset/Contents.json`

**Context:** Xcode requires an `Assets.xcassets` catalog with at least an `AppIcon` set and `AccentColor`. Since iOS 17+, only a single 1024x1024 icon is needed (Xcode auto-generates all sizes). We'll create the catalog structure; the actual 1024x1024 image can be added later.

**Step 1: Create asset catalog structure**

`ios/ChromaDMX/Assets.xcassets/Contents.json`:
```json
{
  "info" : {
    "author" : "xcode",
    "version" : 1
  }
}
```

`ios/ChromaDMX/Assets.xcassets/AppIcon.appiconset/Contents.json`:
```json
{
  "images" : [
    {
      "filename" : "AppIcon.png",
      "idiom" : "universal",
      "platform" : "ios",
      "size" : "1024x1024"
    }
  ],
  "info" : {
    "author" : "xcode",
    "version" : 1
  }
}
```

`ios/ChromaDMX/Assets.xcassets/AccentColor.colorset/Contents.json`:
```json
{
  "colors" : [
    {
      "color" : {
        "color-space" : "srgb",
        "components" : {
          "alpha" : "1.000",
          "blue" : "0.976",
          "green" : "0.318",
          "red" : "0.463"
        }
      },
      "idiom" : "universal"
    }
  ],
  "info" : {
    "author" : "xcode",
    "version" : 1
  }
}
```

> **Note:** AccentColor uses ChromaDMX's brand purple (#7651F9 → R:0.463, G:0.318, B:0.976). The actual AppIcon.png (1024x1024) needs to be created/added separately.

**Step 2: Commit**

```bash
git add ios/ChromaDMX/Assets.xcassets/
git commit -m "feat(ios): add asset catalog with app icon placeholder and accent color"
```

---

## Task 13: Privacy Manifest

**Files:**
- Create: `ios/ChromaDMX/PrivacyInfo.xcprivacy`

**Context:** Apple requires a privacy manifest for all apps submitted to the App Store (enforced since spring 2024). This declares what data the app accesses and why.

**Step 1: Create PrivacyInfo.xcprivacy**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>NSPrivacyTracking</key>
    <false/>
    <key>NSPrivacyTrackingDomains</key>
    <array/>
    <key>NSPrivacyCollectedDataTypes</key>
    <array/>
    <key>NSPrivacyAccessedAPITypes</key>
    <array>
        <dict>
            <key>NSPrivacyAccessedAPIType</key>
            <string>NSPrivacyAccessedAPICategoryFileTimestamp</string>
            <key>NSPrivacyAccessedAPITypeReasons</key>
            <array>
                <string>C617.1</string>
            </array>
        </dict>
        <dict>
            <key>NSPrivacyAccessedAPIType</key>
            <string>NSPrivacyAccessedAPICategoryDiskSpace</string>
            <key>NSPrivacyAccessedAPITypeReasons</key>
            <array>
                <string>E174.1</string>
            </array>
        </dict>
    </array>
</dict>
</plist>
```

**Step 2: Commit**

```bash
git add ios/ChromaDMX/PrivacyInfo.xcprivacy
git commit -m "feat(ios): add privacy manifest for App Store compliance"
```

---

## Task 14: iOS Setup Script for Mac

**Files:**
- Create: `ios/setup-ios.sh`

**Context:** Since the Xcode project must be created on a Mac, this script automates the process. It builds the shared framework, creates the Xcode project using `xcodegen` (or provides manual instructions), and links everything together.

**Step 1: Create setup-ios.sh**

```bash
#!/bin/bash
set -euo pipefail

echo "=== ChromaDMX iOS Setup ==="
echo ""

# Check prerequisites
command -v xcodebuild >/dev/null 2>&1 || { echo "Error: Xcode CLI tools not installed. Run: xcode-select --install"; exit 1; }
command -v java >/dev/null 2>&1 || { echo "Error: JDK not installed. Install JDK 17+."; exit 1; }

cd "$(dirname "$0")/.."

echo "1. Building shared framework for iOS Simulator..."
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

echo ""
echo "2. Building shared framework for iOS Device..."
./gradlew :shared:linkReleaseFrameworkIosArm64

echo ""
echo "3. Building XCFramework (all architectures)..."
./gradlew :shared:assembleSharedXCFramework

echo ""
echo "=== Framework built successfully ==="
echo ""
echo "Simulator framework: shared/build/bin/iosSimulatorArm64/debugFramework/Shared.framework"
echo "Device framework:    shared/build/bin/iosArm64/releaseFramework/Shared.framework"
echo "XCFramework:         shared/build/XCFrameworks/debug/Shared.xcframework"
echo ""
echo "=== Next Steps ==="
echo ""
echo "1. Open Xcode and create a new iOS App project:"
echo "   - Product Name: ChromaDMX"
echo "   - Organization: com.chromadmx"
echo "   - Interface: SwiftUI"
echo "   - Save to: ios/"
echo ""
echo "2. Replace the generated Swift files with the ones in ios/ChromaDMX/"
echo ""
echo "3. Add the Shared framework:"
echo "   - Target > General > Frameworks, Libraries, and Embedded Content"
echo "   - Add Shared.framework (Do Not Embed — it's static)"
echo "   - Build Settings > Framework Search Paths:"
echo "     \$(SRCROOT)/../shared/build/bin/iosSimulatorArm64/debugFramework"
echo ""
echo "4. Add system frameworks in Build Phases > Link Binary With Libraries:"
echo "   - Network.framework"
echo "   - AVFoundation.framework"
echo "   - CoreMedia.framework"
echo "   - CoreVideo.framework"
echo "   - Metal.framework"
echo "   - MetalKit.framework"
echo "   - CoreBluetooth.framework"
echo ""
echo "5. Add a Run Script build phase (before Compile Sources):"
echo "   cd \"\$SRCROOT/..\""
echo "   ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64"
echo ""
echo "6. Set deployment target to iOS 17.0"
echo ""
echo "7. Build and run! (Cmd+R)"
```

**Step 2: Make executable and commit**

```bash
chmod +x ios/setup-ios.sh
git add ios/setup-ios.sh
git commit -m "feat(ios): add Mac setup script for Xcode project creation"
```

---

## Task 15: `[MAC]` Full Build Verification

**This task runs entirely on macOS.**

**Step 1: Run the setup script**

```bash
cd ios && ./setup-ios.sh
```

Expected: All three framework builds succeed.

**Step 2: Create Xcode project following the script's instructions**

Open Xcode → New Project → iOS App → ChromaDMX → Save to `ios/`.

**Step 3: Replace Swift files and add framework**

Follow steps 2-6 from the setup script output.

**Step 4: Build for simulator**

```bash
xcodebuild -project ios/ChromaDMX.xcodeproj -scheme ChromaDMX -destination 'platform=iOS Simulator,name=iPhone 16' build
```

Expected: BUILD SUCCEEDED.

**Step 5: Run on simulator**

Open Xcode, select iPhone 16 simulator, press Cmd+R.

Expected: App launches, Compose UI renders, ChromaDMX main screen appears.

**Step 6: Commit Xcode project**

```bash
cd ios && git add ChromaDMX.xcodeproj/
git commit -m "feat(ios): add Xcode project for ChromaDMX iOS app"
```

---

## Task 16: `[MAC]` Integration Testing

**This task runs entirely on macOS with a physical iOS device (preferred) or simulator.**

**Step 1: Test Compose UI rendering**

- Launch app on simulator
- Verify all screens render: Setup, Stage, Settings, Chat
- Verify navigation between screens works
- Verify pixel art design system renders correctly

**Step 2: Test UDP transport (requires local network)**

- Run the app on a device connected to Wi-Fi
- Verify local network permission prompt appears
- If an Art-Net node is available, verify discovery works

**Step 3: Test BLE (requires physical device)**

- Run on a physical iPhone
- Verify BLE permission prompt appears
- If an ESP32 node is available, verify scanning discovers it

**Step 4: Test camera**

- Run on physical device or simulator (sim has limited camera support)
- Verify camera permission prompt appears
- Verify camera preview displays (if on device)

**Step 5: Test Ableton Link (if LinkKit integrated)**

- Verify Link session enables/disables
- If another Link app is on the network, verify peer count > 0

**Step 6: Create integration test issue**

Document any issues found as GitHub issues for follow-up fixes.

---

## Summary

| Task | Component | Platform | LOC est. |
|------|-----------|----------|----------|
| 1 | UDP Transport | Kotlin/Native | ~120 |
| 2 | BLE Scanner | Kotlin/Native | ~60 |
| 3 | BLE Provisioner | Kotlin/Native | ~180 |
| 4 | Camera Source | Kotlin/Native | ~100 |
| 5 | LinkSession | Kotlin/Native + cinterop | ~60 |
| 6 | Koin DI + IosApp | Kotlin/Native | ~40 |
| 7 | Delete old stubs | cleanup | -120 |
| 8 | Swift app shell | Swift | ~60 |
| 9 | Camera preview | Swift | ~80 |
| 10 | Metal renderer | Swift | ~70 |
| 11 | Permissions | Swift | ~30 |
| 12 | Asset catalog | JSON | ~30 |
| 13 | Privacy manifest | XML | ~30 |
| 14 | Setup script | Bash | ~50 |
| 15 | Build verification | Mac | — |
| 16 | Integration testing | Mac | — |
| **Total** | | | **~790** |

**Dependencies:**
- Tasks 1-5 are independent (can be parallelized)
- Task 6 depends on Tasks 1-5 (needs all actual classes to import)
- Task 7 can run anytime after Task 6
- Tasks 8-14 are independent of Tasks 1-7 (Swift files don't import Kotlin directly)
- Task 15 depends on all prior tasks
- Task 16 depends on Task 15
