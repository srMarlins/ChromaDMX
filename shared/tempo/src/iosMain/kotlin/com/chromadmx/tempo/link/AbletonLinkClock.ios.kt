package com.chromadmx.tempo.link

import com.chromadmx.core.model.BeatState
import com.chromadmx.tempo.clock.BeatClock
import com.chromadmx.tempo.clock.BeatClockUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * iOS actual for [AbletonLinkClock].
 *
 * ## Implementation plan (not yet implemented)
 *
 * 1. Add Ableton Link as a dependency via LinkKit (CocoaPods or SPM) or
 *    build the C++ library directly.
 * 2. Create a cinterop .def file (e.g., `ableton_link.def`) that points
 *    to the Link C++ headers:
 *    ```
 *    headers = ABLLink.h ABLLinkSettingsViewController.h
 *    headerFilter = ABL*
 *    linkerOpts = -framework LinkKit
 *    ```
 * 3. Add cinterop configuration in build.gradle.kts:
 *    ```
 *    iosTarget.compilations["main"].cinterops {
 *        create("abletonLink") {
 *            defFile = file("src/nativeInterop/cinterop/ableton_link.def")
 *        }
 *    }
 *    ```
 * 4. Kotlin/Native code calls the generated C bindings directly.
 * 5. Same coroutine polling loop as Android for StateFlow updates.
 *
 * Note: iOS uses the Objective-C LinkKit wrapper rather than raw C++,
 * which simplifies cinterop since Kotlin/Native handles ObjC bridging natively.
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
            "AbletonLinkClock iOS actual is a stub. " +
                "cinterop bridge to Ableton Link (LinkKit) is not yet implemented."
        )
    }

    override fun stop() {
        throw NotImplementedError(
            "AbletonLinkClock iOS actual is a stub. " +
                "cinterop bridge to Ableton Link (LinkKit) is not yet implemented."
        )
    }
}
