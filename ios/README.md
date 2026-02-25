# ChromaDMX iOS App

iOS platform shell for ChromaDMX, built with Compose Multiplatform and SwiftUI interop.

## Prerequisites

- macOS 14.0+ (Sonoma or later)
- Xcode 16.0+
- CocoaPods or Swift Package Manager (for dependency management)
- JDK 17+ (for Gradle/KMP builds)

## Architecture Overview

The iOS app uses a thin Swift shell that hosts the shared Compose Multiplatform UI:

```
ios/
├── ChromaDMX/
│   ├── ChromaDMXApp.swift          # SwiftUI App entry point
│   ├── ContentView.swift           # SwiftUI view hosting ComposeUIViewController
│   ├── Info.plist                  # App configuration
│   └── Assets.xcassets/            # App icon, colors, images
├── ChromaDMX.xcodeproj/           # Xcode project
└── README.md                      # This file
```

The bulk of the application logic and UI lives in the KMP shared module
(`shared/src/commonMain/` and `shared/src/iosMain/`). The Swift code is
a minimal shell that:

1. Initializes the KMP framework
2. Hosts the Compose UI in a `UIViewControllerRepresentable`
3. Provides native iOS views where needed (camera preview via AVFoundation, Metal rendering)

## Xcode Project Setup

### Step 1: Build the Shared Framework

From the project root on macOS, build the iOS framework:

```bash
# For simulator (development)
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# For device (release)
./gradlew :shared:linkReleaseFrameworkIosArm64

# For all iOS targets
./gradlew :shared:assembleSharedXCFramework
```

This produces `Shared.framework` (a static framework) in:
```
shared/build/bin/iosSimulatorArm64/debugFramework/Shared.framework
shared/build/bin/iosArm64/releaseFramework/Shared.framework
```

### Step 2: Create the Xcode Project

1. Open Xcode and create a new project:
   - Template: **iOS > App**
   - Product Name: **ChromaDMX**
   - Organization Identifier: `com.chromadmx`
   - Interface: **SwiftUI**
   - Language: **Swift**
   - Save to the `ios/` directory

2. Remove the auto-generated `ContentView.swift` body (we will replace it).

### Step 3: Integrate the Shared Framework

#### Option A: Direct Framework Embedding (Recommended for Development)

1. In Xcode, select the **ChromaDMX** target > **General** tab.
2. Under **Frameworks, Libraries, and Embedded Content**, click **+**.
3. Click **Add Other... > Add Files...** and navigate to:
   ```
   shared/build/bin/iosSimulatorArm64/debugFramework/Shared.framework
   ```
4. Set embed option to **Do Not Embed** (the framework is static).
5. In **Build Settings**, add the framework search path:
   ```
   FRAMEWORK_SEARCH_PATHS = $(SRCROOT)/../shared/build/bin/iosSimulatorArm64/debugFramework
   ```

#### Option B: Gradle-Xcode Integration (Recommended for CI/CD)

Add a Run Script build phase that builds the framework before compilation:

1. Select **ChromaDMX** target > **Build Phases**.
2. Add a **New Run Script Phase** (move it before "Compile Sources"):
   ```bash
   cd "$SRCROOT/.."
   ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
   ```
3. Set framework search path to the build output directory.

#### Option C: XCFramework for Multi-Architecture

For distribution or running on both simulator and device:

```bash
./gradlew :shared:assembleSharedXCFramework
```

This produces `shared/build/XCFrameworks/debug/Shared.xcframework` which
supports all architectures (arm64 device + arm64/x86_64 simulator).

### Step 4: Create the Swift App Code

#### ChromaDMXApp.swift

```swift
import SwiftUI
import Shared

@main
struct ChromaDMXApp: App {
    init() {
        // Initialize KMP shared module (Koin DI, platform services)
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

#### ComposeView.swift

```swift
import SwiftUI
import Shared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // No updates needed - Compose manages its own state
    }
}
```

### Step 5: Configure Required Frameworks

Add these frameworks in **Build Phases > Link Binary With Libraries**:

| Framework | Purpose |
|---|---|
| `Network.framework` | UDP socket for Art-Net/sACN DMX protocol |
| `AVFoundation.framework` | Camera access for spatial fixture mapping |
| `CoreMedia.framework` | Camera frame processing support |
| `CoreVideo.framework` | Video frame buffer access |
| `Metal.framework` | GPU venue visualization rendering |
| `MetalKit.framework` | Metal view integration |

### Step 6: Configure Info.plist

Add these required permission descriptions:

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
```

## Building and Running

### Simulator

```bash
# 1. Build the shared framework
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# 2. Open Xcode project
open ios/ChromaDMX.xcodeproj

# 3. Select an iOS Simulator target and press Cmd+R
```

### Physical Device

```bash
# 1. Build the shared framework for device
./gradlew :shared:linkDebugFrameworkIosArm64

# 2. In Xcode, select your physical device
# 3. Ensure signing is configured (Team, Bundle ID)
# 4. Press Cmd+R
```

### Common Build Issues

| Issue | Solution |
|---|---|
| "No such module 'Shared'" | Rebuild the framework: `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` |
| Framework architecture mismatch | Ensure you built for the correct target (simulatorArm64 vs arm64) |
| "Undefined symbols" linker errors | Verify the framework is added in Build Phases and search paths are correct |
| Compose UI not rendering | Ensure `.ignoresSafeArea(.all)` is set on the ComposeView |

## Project Structure (When Fully Set Up)

```
ios/
├── ChromaDMX/
│   ├── ChromaDMXApp.swift          # App entry point, KMP initialization
│   ├── ComposeView.swift           # UIViewControllerRepresentable wrapper
│   ├── NativeViews/
│   │   ├── CameraPreview.swift     # AVFoundation camera preview (SwiftUI)
│   │   └── MetalVenueView.swift    # Metal-based venue visualization
│   ├── Platform/
│   │   ├── UdpTransport.swift      # Network.framework UDP implementation (if needed natively)
│   │   └── Permissions.swift       # Camera, network permission handling
│   ├── Info.plist
│   └── Assets.xcassets/
│       ├── AppIcon.appiconset/
│       └── AccentColor.colorset/
├── ChromaDMX.xcodeproj/
└── README.md
```

## KMP Shared Code for iOS

The Kotlin-side iOS code lives in the shared module:

```
shared/src/iosMain/kotlin/com/chromadmx/
├── Platform.ios.kt                 # Platform name implementation
└── ios/
    ├── MainViewController.kt       # ComposeUIViewController entry point
    └── IosApp.kt                   # iOS initialization and configuration
```

Future iOS-specific Kotlin code (expect/actual implementations) will be added here:

```
shared/src/iosMain/kotlin/com/chromadmx/
├── networking/
│   └── IosUdpTransport.kt         # NWConnection-based UDP (via Kotlin/Native cinterop)
├── tempo/
│   └── IosAbletonLinkBridge.kt    # LinkKit cinterop wrapper
└── vision/
    └── IosCameraSource.kt          # AVFoundation frame capture bridge
```

## Notes

- The iOS app requires macOS + Xcode to build. The Kotlin-side iOS code
  (`shared/src/iosMain/`) can be written on any platform but can only be
  compiled on macOS.
- The `Shared.framework` is configured as **static** in `shared/build.gradle.kts`,
  which means it is linked into the app binary at build time (no framework
  embedding needed at runtime).
- Compose Multiplatform for iOS reached **Stable** status with version 1.8.0
  (May 2025). The project currently uses version 1.7.3 -- upgrade to 1.8+ is
  recommended when setting up the Xcode project.
