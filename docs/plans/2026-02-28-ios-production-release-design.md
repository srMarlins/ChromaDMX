# iOS Production Release — Full Feature Parity Design

**Date:** 2026-02-28
**Status:** Draft
**Goal:** Get the iOS app to full feature parity with Android and ready for App Store release.

## Approach

Hybrid: Kotlin/Native for shared platform logic (UDP, BLE, Link) via cinterop, Swift for native views requiring UIKit/Metal (camera preview, venue renderer). Thin SwiftUI app shell hosting Compose Multiplatform UI.

## Current State

- **Complete:** Compose UI (all screens), SQLDelight database, file storage, platform detection, API key provider
- **Stubbed:** UDP transport, BLE scanner, BLE provisioner, camera source, Ableton Link
- **Missing:** Xcode project, Swift app shell, native views, App Store assets

## 1. Xcode Project & App Shell

### Target
- **iOS 17.0+** (simplifies multicast entitlement, modern APIs)
- **Xcode 16.0+**, macOS 14.0+ (Sonoma)

### Swift Files (5 total)

| File | Purpose | LOC est. |
|------|---------|----------|
| `ChromaDMXApp.swift` | `@main` entry, calls `IosApp.shared.initialize()` | ~20 |
| `ComposeView.swift` | `UIViewControllerRepresentable` wrapping `MainViewController()` | ~15 |
| `CameraPreview.swift` | `AVCaptureSession` + grayscale frame extraction → Kotlin callback | ~150 |
| `MetalVenueView.swift` | `MTKView` renderer for fixture visualization | ~200 |
| `Permissions.swift` | Runtime permission requests (camera, local network, BLE) | ~60 |

### Info.plist

```xml
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
```

### Entitlements

- `com.apple.developer.multicast` — required for Ableton Link peer discovery on iOS 17+
- `com.apple.developer.networking.networkextension` — if needed for advanced network access

### Build Configuration

- Static `Shared.framework` linked via Gradle build phase
- Framework search path: `$(SRCROOT)/../shared/build/bin/iosSimulatorArm64/debugFramework`
- XCFramework for release: `./gradlew :shared:assembleSharedXCFramework`

### Required System Frameworks

| Framework | Purpose |
|-----------|---------|
| `Network.framework` | UDP transport for Art-Net/sACN |
| `AVFoundation.framework` | Camera capture |
| `CoreMedia.framework` | Frame processing |
| `CoreVideo.framework` | Pixel buffer access |
| `Metal.framework` | GPU venue rendering |
| `MetalKit.framework` | Metal view integration |
| `CoreBluetooth.framework` | BLE scanning and provisioning |

## 2. Kotlin/Native Platform Implementations

### A. PlatformUdpTransport — Network.framework

**File:** `shared/networking/src/iosMain/kotlin/.../transport/UdpTransport.ios.kt`

Uses Apple's Network.framework via Kotlin/Native cinterop (`platform.Network` or custom `.def`).

**API mapping:**
- `NWConnection` with `.udp` protocol for send/receive
- `NWParameters.createUDP()` for configuration
- `NWListener` for receiving broadcast/multicast Art-Net packets
- `nw_connection_send()` / `nw_connection_receive_message()` for datagram I/O

**Interface contract:**
```kotlin
actual class PlatformUdpTransport actual constructor() {
    actual suspend fun send(data: ByteArray, address: String, port: Int)
    actual suspend fun receive(buffer: ByteArray, timeoutMs: Long): UdpPacket?
    actual fun close()
}
```

**Implementation notes:**
- Dispatch queue: dedicated serial queue for network callbacks
- Bridge Network.framework callbacks to Kotlin coroutines via `suspendCancellableCoroutine`
- Connection pooling: reuse connections per destination (Art-Net nodes)

### B. BleScanner — CoreBluetooth

**File:** `shared/networking/src/iosMain/kotlin/.../ble/BleScanner.ios.kt`

Uses `CBCentralManager` via Kotlin/Native's built-in `platform.CoreBluetooth` bindings.

**Implementation:**
- Kotlin class adopts `CBCentralManagerDelegateProtocol` via `NSObject` inheritance
- `startScan()` → `centralManager.scanForPeripherals(withServices: [chromaServiceCBUUID])`
- `didDiscoverPeripheral` callback → update `_discoveredNodes` StateFlow
- `stopScan()` → `centralManager.stopScan()`
- Service UUID: `4368726f-6d61-444d-5800-000000000001`

### C. BleProvisioner — CoreBluetooth

**File:** `shared/networking/src/iosMain/kotlin/.../ble/BleProvisioner.ios.kt`

Uses `CBPeripheral` + `CBPeripheralDelegateProtocol`.

**State machine:**
```
IDLE → CONNECTING → READING_CONFIG → WRITING_CONFIG → VERIFYING → SUCCESS
                                                                → ERROR
```

**GATT characteristics** (from `GattServiceSpec.CHROMA_DMX`):
- `node_name`, `wifi_ssid`, `wifi_password`, `universe`, `dmx_start_address`
- Read/write via `peripheral.readValue(for:)` and `peripheral.writeValue(_:for:type:)`

### D. PlatformCameraSource — AVFoundation (Kotlin/Native bridge)

**File:** `shared/vision/src/iosMain/kotlin/.../camera/CameraSource.ios.kt`

The Kotlin side creates and manages the `AVCaptureSession` via Kotlin/Native's built-in `platform.AVFoundation` bindings.

**Implementation:**
- Configure `AVCaptureSession` with `AVCaptureDevice.defaultDevice(withMediaType: .video)`
- Add `AVCaptureVideoDataOutput` with pixel format `kCVPixelFormatType_420YpCbCr8BiPlanarFullRange`
- Implement `AVCaptureVideoDataOutputSampleBufferDelegateProtocol`
- Extract Y plane from `CVPixelBuffer`, normalize to `FloatArray` [0.0, 1.0]
- Analysis resolution: 320×240

**Swift CameraPreview** provides the live preview UIView — the Kotlin side only does frame analysis.

### E. LinkSession — LinkKit cinterop

**File:** `shared/tempo/src/iosMain/kotlin/.../link/LinkSession.ios.kt`

**Cinterop setup:**
```
// shared/tempo/src/nativeInterop/cinterop/ableton_link.def
language = Objective-C
headers = ABLLink.h ABLLinkSettingsViewController.h
headerFilter = ABL*
linkerOpts = -framework LinkKit
```

**Gradle config** (in `shared/tempo/build.gradle.kts`):
```kotlin
kotlin {
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.compilations["main"].cinterops {
            create("abletonLink") {
                defFile = file("src/nativeInterop/cinterop/ableton_link.def")
            }
        }
    }
}
```

**API mapping:**
| Kotlin | LinkKit C API |
|--------|--------------|
| `enable()` | `ABLLinkSetActive(ref, true)` |
| `disable()` | `ABLLinkSetActive(ref, false)` |
| `bpm` | `ABLLinkGetTempo(sessionState)` |
| `beatPhase` | `ABLLinkGetBeatAtTime(state, hostTime, 1.0) % 1.0` |
| `barPhase` | `ABLLinkGetBeatAtTime(state, hostTime, 4.0) % 4.0 / 4.0` |
| `peerCount` | `ABLLinkGetNumPeers(ref)` |
| `requestBpm()` | `ABLLinkSetTempo(state, bpm, hostTime)` |
| `close()` | `ABLLinkDelete(ref)` |

**Dependency:** LinkKit.xcframework must be downloaded from [Ableton releases](https://github.com/Ableton/LinkKit/releases) and placed in `ios/Frameworks/`.

### F. Koin DI — IosDiModule + IosApp.initialize()

**IosDiModule.kt** — register all iOS platform implementations:
```kotlin
val iosPlatformModule: Module = module {
    single<FileStorage> { IosFileStorage() }
    single { DriverFactory() }
    single { BleScanner() }
    single { BleProvisioner() }
}
```

**IosApp.initialize()** — start Koin:
```kotlin
fun initialize() {
    startKoin {
        modules(chromaDiModule, tempoModule, agentModule, iosPlatformModule, uiModule)
    }
}
```

## 3. Swift Native Views

### CameraPreview.swift

```swift
struct CameraPreview: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> CameraViewController {
        CameraViewController()
    }
    func updateUIViewController(_ vc: CameraViewController, context: Context) {}
}

class CameraViewController: UIViewController {
    private let captureSession = AVCaptureSession()
    // Configure back camera, add video data output
    // Live preview via AVCaptureVideoPreviewLayer
}
```

This provides the live camera preview in the UI. The Kotlin-side `PlatformCameraSource` handles frame analysis independently.

### MetalVenueView.swift

```swift
struct MetalVenueView: UIViewRepresentable {
    func makeUIView(context: Context) -> MTKView { ... }
    func updateUIView(_ view: MTKView, context: Context) { ... }
}

class VenueRenderer: NSObject, MTKViewDelegate {
    func draw(in view: MTKView) {
        // Render fixture dots as colored circles
        // Dark background (0.05, 0.05, 0.08)
        // Wireframe venue outline
    }
}
```

Mirrors Android's `VenueRenderer.kt` scaffold. Both platforms currently have empty renderers — this maintains parity.

## 4. App Store Production Requirements

| Requirement | Details |
|-------------|---------|
| Bundle ID | `com.chromadmx.ChromaDMX` |
| App icon | 1024×1024 source in `Assets.xcassets` (all sizes auto-generated) |
| Launch screen | SwiftUI launch screen with pixel-art branding |
| Privacy manifest | `PrivacyInfo.xcprivacy` (camera, local network, BLE data collection) |
| Signing | Apple Developer account, automatic signing |
| Deployment target | iOS 17.0 |
| Version | 0.1.0 (matching Android) |

## 5. Development Strategy

### What can be done on Windows
- Write all Kotlin/Native `iosMain` implementations
- Write all Swift source files (as text)
- Create cinterop `.def` files
- Write iOS setup script for Mac
- Create file/directory structure for Xcode project

### What requires macOS
- Create actual `.xcodeproj` (Xcode CLI: `xcodebuild`)
- Compile `Shared.framework`
- Link frameworks and verify builds
- Run on simulator/device
- Download and integrate LinkKit.xcframework
- App Store submission

### Verification plan
1. Write all code on Windows
2. On Mac: run `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64`
3. Create Xcode project, add framework + Swift files
4. Build and run on iOS Simulator
5. Test each platform implementation individually
6. Full integration test on physical device

## 6. Estimated Scope

| Component | Kotlin LOC | Swift LOC |
|-----------|-----------|-----------|
| UDP Transport (Network.framework) | ~200 | — |
| BLE Scanner | ~150 | — |
| BLE Provisioner | ~200 | — |
| Camera Source | ~120 | ~150 |
| LinkSession (cinterop) | ~80 | — |
| Venue Renderer | — | ~200 |
| App Shell | — | ~100 |
| Koin/DI setup | ~30 | — |
| Permissions | — | ~60 |
| **Total** | **~780** | **~510** |

Total: ~1,290 lines of new code across Kotlin/Native and Swift.
