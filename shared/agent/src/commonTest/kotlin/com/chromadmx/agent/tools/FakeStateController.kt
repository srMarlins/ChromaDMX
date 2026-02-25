package com.chromadmx.agent.tools

import com.chromadmx.agent.controller.BeatStateSnapshot
import com.chromadmx.agent.controller.EngineStateSnapshot
import com.chromadmx.agent.controller.NetworkStateSnapshot
import com.chromadmx.agent.controller.StateController

/**
 * Fake [StateController] for testing state tools.
 */
class FakeStateController : StateController {
    var fakeEngineState = EngineStateSnapshot(
        isRunning = false,
        layerCount = 0,
        masterDimmer = 1.0f,
        fixtureCount = 0,
        fps = 0f,
        effectIds = emptyList()
    )

    var fakeBeatState = BeatStateSnapshot(
        bpm = 120.0f,
        beatPhase = 0f,
        barPhase = 0f,
        isRunning = false,
        source = "None"
    )

    var fakeNetworkState = NetworkStateSnapshot(
        nodeCount = 0,
        totalUniverses = 0,
        isOutputActive = false,
        protocol = "Art-Net",
        frameRate = 0
    )

    override fun getEngineState(): EngineStateSnapshot = fakeEngineState
    override fun getBeatState(): BeatStateSnapshot = fakeBeatState
    override fun getNetworkState(): NetworkStateSnapshot = fakeNetworkState
}
