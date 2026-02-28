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
echo '     $(SRCROOT)/../shared/build/bin/iosSimulatorArm64/debugFramework'
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
echo '   cd "$SRCROOT/.."'
echo "   ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64"
echo ""
echo "6. Set deployment target to iOS 17.0"
echo ""
echo "7. Build and run! (Cmd+R)"
