package com.chromadmx.tempo.link

/**
 * iOS actual for [LinkSession].
 *
 * Wraps Kotlin/Native cinterop calls to the Ableton Link SDK (via LinkKit
 * framework or raw C++ headers).
 *
 * ## cinterop Setup (not yet implemented)
 *
 * ### Option A: LinkKit ObjC Framework (recommended for iOS)
 *
 * LinkKit provides an Objective-C wrapper around Link that Kotlin/Native
 * handles natively via ObjC interop:
 *
 * 1. Add LinkKit.framework to the Xcode project (CocoaPods or SPM).
 * 2. Create `src/nativeInterop/cinterop/ableton_link.def`:
 *    ```
 *    language = Objective-C
 *    headers = ABLLink.h ABLLinkSettingsViewController.h
 *    headerFilter = ABL*
 *    linkerOpts = -framework LinkKit
 *    ```
 * 3. Configure in `build.gradle.kts`:
 *    ```kotlin
 *    kotlin {
 *        listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
 *            target.compilations["main"].cinterops {
 *                create("abletonLink") {
 *                    defFile = file("src/nativeInterop/cinterop/ableton_link.def")
 *                }
 *            }
 *        }
 *    }
 *    ```
 *
 * ### Option B: Raw C++ via cinterop
 *
 * If not using LinkKit, compile Link as a static C library with a thin C
 * wrapper (`link_c_api.h`) and reference that in the .def file.
 *
 * ## Usage pattern (once integrated)
 *
 * ```kotlin
 * actual class LinkSession actual constructor() : LinkSessionApi {
 *     private val ref = ABLLinkNew(120.0)  // Create Link session at 120 BPM
 *
 *     actual override fun enable() { ABLLinkSetActive(ref, true) }
 *     actual override fun disable() { ABLLinkSetActive(ref, false) }
 *
 *     actual override val bpm: Double get() {
 *         val state = ABLLinkCaptureAppSessionState(ref)
 *         return ABLLinkGetTempo(state)
 *     }
 *     // ... etc.
 * }
 * ```
 *
 * ## Current Status
 *
 * This is a **stub implementation**. All methods return safe defaults.
 * iOS compilation is not available on Windows (suppressed via
 * `kotlin.native.ignoreDisabledTargets=true`).
 */
actual class LinkSession actual constructor() : LinkSessionApi {

    private var _enabled = false

    actual override fun enable() {
        _enabled = true
        // TODO: ABLLinkSetActive(ref, true) when cinterop is configured
    }

    actual override fun disable() {
        _enabled = false
        // TODO: ABLLinkSetActive(ref, false) when cinterop is configured
    }

    actual override val isEnabled: Boolean
        get() = _enabled

    actual override val peerCount: Int
        get() = 0
        // TODO: ABLLinkGetNumPeers(ref) when cinterop is configured

    actual override val bpm: Double
        get() = 120.0
        // TODO: Capture from ABLLink session state when cinterop is configured

    actual override val beatPhase: Double
        get() = 0.0
        // TODO: ABLLinkGetBeatAtTime(state, hostTimeNanos, 1.0) % 1.0

    actual override val barPhase: Double
        get() = 0.0
        // TODO: ABLLinkGetBeatAtTime(state, hostTimeNanos, 4.0) % 4.0 / 4.0

    actual override fun requestBpm(bpm: Double) {
        // TODO: ABLLinkSetTempo(state, bpm, hostTimeNanos) when cinterop is configured
    }

    /**
     * Release native resources. No-op in stub mode.
     */
    actual override fun close() {
        _enabled = false
        // TODO: ABLLinkDelete(ref) when cinterop is configured
    }
}
