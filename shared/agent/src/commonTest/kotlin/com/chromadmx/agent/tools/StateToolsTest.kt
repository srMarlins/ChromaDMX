package com.chromadmx.agent.tools

import com.chromadmx.agent.controller.BeatStateSnapshot
import com.chromadmx.agent.controller.EngineStateSnapshot
import com.chromadmx.agent.controller.NetworkStateSnapshot
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains

class StateToolsTest {
    private val controller = FakeStateController()

    @Test
    fun getEngineStateReturnsInfo() = runTest {
        controller.fakeEngineState = EngineStateSnapshot(
            isRunning = true,
            layerCount = 3,
            masterDimmer = 0.8f,
            fixtureCount = 12,
            fps = 59.5f,
            effectIds = listOf("solid_color", "rainbow_sweep_3d", "strobe")
        )
        val tool = GetEngineStateTool(controller)
        val result = tool.execute(GetEngineStateTool.Args())
        assertContains(result, "running")
        assertContains(result, "3 layers")
        assertContains(result, "12 fixtures")
        assertContains(result, "59.5")
    }

    @Test
    fun getEngineStateStoppedReturnsInfo() = runTest {
        controller.fakeEngineState = EngineStateSnapshot(
            isRunning = false,
            layerCount = 0,
            masterDimmer = 1.0f,
            fixtureCount = 0,
            fps = 0f,
            effectIds = emptyList()
        )
        val tool = GetEngineStateTool(controller)
        val result = tool.execute(GetEngineStateTool.Args())
        assertContains(result, "stopped")
    }

    @Test
    fun getBeatStateReturnsInfo() = runTest {
        controller.fakeBeatState = BeatStateSnapshot(
            bpm = 128.0f,
            beatPhase = 0.5f,
            barPhase = 0.25f,
            isRunning = true,
            source = "TapTempo"
        )
        val tool = GetBeatStateTool(controller)
        val result = tool.execute(GetBeatStateTool.Args())
        assertContains(result, "128.0")
        assertContains(result, "TapTempo")
        assertContains(result, "running")
    }

    @Test
    fun getBeatStateStoppedReturnsInfo() = runTest {
        controller.fakeBeatState = BeatStateSnapshot(
            bpm = 120.0f,
            beatPhase = 0f,
            barPhase = 0f,
            isRunning = false,
            source = "None"
        )
        val tool = GetBeatStateTool(controller)
        val result = tool.execute(GetBeatStateTool.Args())
        assertContains(result, "stopped")
    }

    @Test
    fun getNetworkStateReturnsInfo() = runTest {
        controller.fakeNetworkState = NetworkStateSnapshot(
            nodeCount = 3,
            totalUniverses = 5,
            isOutputActive = true,
            protocol = "Art-Net",
            frameRate = 40
        )
        val tool = GetNetworkStateTool(controller)
        val result = tool.execute(GetNetworkStateTool.Args())
        assertContains(result, "3 nodes")
        assertContains(result, "5 universes")
        assertContains(result, "Art-Net")
        assertContains(result, "active")
    }

    @Test
    fun getNetworkStateInactiveReturnsInfo() = runTest {
        controller.fakeNetworkState = NetworkStateSnapshot(
            nodeCount = 0,
            totalUniverses = 0,
            isOutputActive = false,
            protocol = "Art-Net",
            frameRate = 0
        )
        val tool = GetNetworkStateTool(controller)
        val result = tool.execute(GetNetworkStateTool.Args())
        assertContains(result, "inactive")
    }
}
