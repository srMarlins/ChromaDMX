package com.chromadmx.platform

/**
 * iOS Ableton Link bridge stub for tempo synchronization.
 *
 * When implemented, this will use Kotlin/Native cinterop to wrap the
 * Ableton Link C++ library (LinkKit for iOS) for real-time tempo
 * synchronization with DJ software (Traktor, rekordbox, Serato, Ableton).
 *
 * LinkKit for iOS is distributed as a static library and requires a
 * cinterop definition to bridge into Kotlin/Native.
 *
 * The cinterop definition:
 * ```
 * // src/nativeInterop/cinterop/link.def
 * headers = ABLLink.h ABLLinkSettingsViewController.h
 * staticLibraries = libABLLink.a
 * libraryPaths = libs/ios
 * ```
 *
 * Ableton Link provides:
 * - Shared tempo (BPM) across all connected peers
 * - Beat phase (position within the current beat, 0.0 to 1.0)
 * - Bar phase (position within the current bar, 0.0 to 1.0)
 * - Start/stop synchronization
 *
 * On iOS, Link can discover peers via:
 * - Wi-Fi (same network as DMX nodes -- ideal setup)
 * - Bluetooth (peer-to-peer, no router needed)
 *
 * Note: LinkKit for iOS includes a settings UI (ABLLinkSettingsViewController)
 * that should be presented as a native iOS view for Link enable/disable
 * and connection status display.
 */
class IosAbletonLinkBridge {
    // TODO: Implement when shared/tempo module defines the expect BeatClock interface
    //
    // Expected interface:
    //   actual class IosAbletonLinkBridge : BeatClock {
    //       override val bpm: StateFlow<Float>
    //       override val beatPhase: StateFlow<Float>
    //       override val barPhase: StateFlow<Float>
    //       override val elapsed: StateFlow<Long>
    //       override fun start()
    //       override fun stop()
    //   }
}
