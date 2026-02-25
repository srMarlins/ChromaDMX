package com.chromadmx.tempo.link

import com.chromadmx.core.model.BeatState
import com.chromadmx.tempo.clock.BeatClock
import com.chromadmx.tempo.clock.BeatClockUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android actual for [AbletonLinkClock].
 *
 * ## Implementation plan (not yet implemented)
 *
 * 1. Load native library: `System.loadLibrary("ableton_link_jni")`
 * 2. JNI bridge to Ableton Link C++ SDK via external functions:
 *    - `external fun nativeCreateSession(bpm: Double): Long`
 *    - `external fun nativeEnable(ptr: Long, enabled: Boolean)`
 *    - `external fun nativeCaptureTempo(ptr: Long): Double`
 *    - `external fun nativeCapturePhase(ptr: Long, quantum: Double): Double`
 *    - `external fun nativeNumPeers(ptr: Long): Int`
 *    - `external fun nativeDestroy(ptr: Long)`
 * 3. Build Link via CMake in the android module's build.gradle.kts:
 *    ```
 *    android {
 *        externalNativeBuild {
 *            cmake { path = file("src/main/cpp/CMakeLists.txt") }
 *        }
 *    }
 *    ```
 * 4. Polling loop at ~60fps reads tempo and phase, updates StateFlows.
 */
actual class AbletonLinkClock actual constructor(
    private val scope: CoroutineScope
) : BeatClock {

    private val _bpm = MutableStateFlow(BeatClockUtils.DEFAULT_BPM)
    override val bpm: StateFlow<Float> = _bpm.asStateFlow()

    private val _beatPhase = MutableStateFlow(0f)
    override val beatPhase: StateFlow<Float> = _beatPhase.asStateFlow()

    private val _barPhase = MutableStateFlow(0f)
    override val barPhase: StateFlow<Float> = _barPhase.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _beatState = MutableStateFlow(BeatState.IDLE)
    override val beatState: StateFlow<BeatState> = _beatState.asStateFlow()

    override fun start() {
        throw NotImplementedError(
            "AbletonLinkClock Android actual is a stub. " +
                "JNI bridge to Ableton Link C++ SDK is not yet implemented."
        )
    }

    override fun stop() {
        throw NotImplementedError(
            "AbletonLinkClock Android actual is a stub. " +
                "JNI bridge to Ableton Link C++ SDK is not yet implemented."
        )
    }
}
