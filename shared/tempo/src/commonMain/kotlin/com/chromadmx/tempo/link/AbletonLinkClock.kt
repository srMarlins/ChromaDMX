package com.chromadmx.tempo.link

import com.chromadmx.tempo.clock.BeatClock
import kotlinx.coroutines.CoroutineScope

/**
 * Ableton Link-based beat clock that synchronizes BPM and phase across
 * devices on the same network.
 *
 * ## Bridging approach
 *
 * Ableton Link is a C++ library. To use it from Kotlin Multiplatform:
 *
 * ### Android (JNI)
 * 1. Build the Link C++ library as a shared library (.so) using CMake/ndk-build.
 * 2. Write a thin JNI C++ wrapper that exposes Link session operations:
 *    - `nativeCreateSession(bpm: Double): Long` → returns native pointer
 *    - `nativeEnable(ptr: Long, enabled: Boolean)`
 *    - `nativeCaptureTempo(ptr: Long): Double`
 *    - `nativeCapturePhase(ptr: Long, quantum: Double): Double`
 *    - `nativeDestroy(ptr: Long)`
 * 3. Kotlin `actual` class loads the .so and calls these via `external fun`.
 * 4. A coroutine loop polls phase/tempo at ~60fps and updates StateFlows.
 *
 * ### iOS (cinterop)
 * 1. Build Link as a static library (.a) or use the LinkKit framework.
 * 2. Create a .def file for Kotlin/Native cinterop pointing to the Link headers.
 * 3. Kotlin `actual` class calls the generated Kotlin/Native bindings directly.
 * 4. Same coroutine loop pattern as Android for phase updates.
 *
 * ### Shared interface
 * Both platforms expose the same [BeatClock] interface, so the effect engine
 * and UI layer are completely platform-agnostic.
 *
 * @param scope Coroutine scope for the phase polling loop.
 */
expect class AbletonLinkClock(scope: CoroutineScope) : BeatClock
