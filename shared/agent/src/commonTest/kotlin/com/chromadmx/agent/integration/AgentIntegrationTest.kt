package com.chromadmx.agent.integration

import com.chromadmx.agent.LightingAgent
import com.chromadmx.agent.config.AgentConfig
import com.chromadmx.agent.controller.BeatStateSnapshot
import com.chromadmx.agent.controller.EngineStateSnapshot
import com.chromadmx.agent.controller.NetworkStateSnapshot
import com.chromadmx.agent.controller.NodeSummary
import com.chromadmx.agent.model.DiagnosticResult
import com.chromadmx.agent.model.NodeStatusResult
import com.chromadmx.agent.scene.ScenePreset
import com.chromadmx.agent.scene.EffectLayerConfig
import com.chromadmx.agent.scene.SceneStore
import com.chromadmx.agent.tools.FakeEngineController
import com.chromadmx.agent.tools.FakeFixtureController
import com.chromadmx.agent.tools.FakeNetworkController
import com.chromadmx.agent.tools.FakeStateController
import com.chromadmx.agent.tools.buildToolRegistry
import com.chromadmx.core.model.Fixture
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests that verify end-to-end tool dispatch through the
 * [LightingAgent]. Tests call tools via [LightingAgent.dispatchTool]
 * and verify that the fake controllers receive the correct commands.
 */
class AgentIntegrationTest {
    private val engineController = FakeEngineController()
    private val networkController = FakeNetworkController()
    private val fixtureController = FakeFixtureController()
    private val stateController = FakeStateController()
    private val sceneStore = SceneStore()

    private val agent = LightingAgent(
        config = AgentConfig(),
        toolRegistry = buildToolRegistry(
            engineController = engineController,
            networkController = networkController,
            fixtureController = fixtureController,
            stateController = stateController,
            sceneStore = sceneStore,
        ),
    )

    // ---- ScenePreset tool dispatch ----

    @Test
    fun setEffectViaJsonDispatch() = runTest {
        val result = agent.dispatchTool(
            "setEffect",
            """{"layer": 0, "effectId": "solid_color", "params": {"r": 1.0, "g": 0.0, "b": 0.0}}"""
        )
        assertContains(result, "solid_color")
        assertEquals("solid_color", engineController.lastSetEffectId)
        assertEquals(0, engineController.lastSetEffectLayer)
    }

    @Test
    fun setBlendModeViaJsonDispatch() = runTest {
        val result = agent.dispatchTool("setBlendMode", """{"layer": 1, "mode": "ADDITIVE"}""")
        assertContains(result, "ADDITIVE")
        assertEquals("ADDITIVE", engineController.lastBlendMode)
    }

    @Test
    fun setMasterDimmerViaJsonDispatch() = runTest {
        val result = agent.dispatchTool("setMasterDimmer", """{"value": 0.75}""")
        assertContains(result, "0.75")
        assertEquals(0.75f, engineController.lastMasterDimmer)
    }

    @Test
    fun setColorPaletteViaJsonDispatch() = runTest {
        val result = agent.dispatchTool(
            "setColorPalette",
            """{"colors": ["#FF0000", "#00FF00", "#0000FF"]}"""
        )
        assertContains(result, "3 colors")
        assertEquals(3, engineController.lastPalette.size)
    }

    @Test
    fun setTempoMultiplierViaJsonDispatch() = runTest {
        val result = agent.dispatchTool("setTempoMultiplier", """{"multiplier": 2.0}""")
        assertContains(result, "2.0")
        assertEquals(2.0f, engineController.lastTempoMultiplier)
    }

    // ---- ScenePreset save/load workflow ----

    @Test
    fun createAndLoadSceneWorkflow() = runTest {
        // Create a scene
        val createResult = agent.dispatchTool("createScene", """{"name": "My ScenePreset"}""")
        assertContains(createResult, "My ScenePreset")
        assertNotNull(sceneStore.load("My ScenePreset"))

        // Load the scene
        val loadResult = agent.dispatchTool("loadScene", """{"name": "My ScenePreset"}""")
        assertContains(loadResult, "My ScenePreset")
    }

    @Test
    fun loadNonexistentSceneReturnsError() = runTest {
        val result = agent.dispatchTool("loadScene", """{"name": "NoSuchScene"}""")
        assertContains(result, "not found")
    }

    // ---- Network tool dispatch ----

    @Test
    fun scanNetworkViaDispatch() = runTest {
        networkController.nodes = listOf(
            NodeSummary("n1", "192.168.1.100", "Node A", listOf(0, 1))
        )
        val result = agent.dispatchTool("scanNetwork")
        assertContains(result, "1 nodes")
        assertContains(result, "Node A")
    }

    @Test
    fun getNodeStatusViaDispatch() = runTest {
        networkController.nodeStatus = NodeStatusResult(
            id = "n1", name = "Node A", ip = "192.168.1.100",
            universes = listOf(0), isOnline = true
        )
        val result = agent.dispatchTool("getNodeStatus", """{"nodeId": "n1"}""")
        assertContains(result, "Node A")
        assertContains(result, "online")
    }

    @Test
    fun configureNodeViaDispatch() = runTest {
        networkController.configureResult = true
        val result = agent.dispatchTool(
            "configureNode",
            """{"nodeId": "n1", "universe": 2, "startAddress": 1}"""
        )
        assertContains(result, "Configured")
        assertEquals("n1", networkController.lastConfiguredNodeId)
        assertEquals(2, networkController.lastConfiguredUniverse)
    }

    @Test
    fun diagnoseConnectionViaDispatch() = runTest {
        networkController.diagnosticResult = DiagnosticResult(
            nodeId = "n1", latencyMs = 1.5f, packetLossPercent = 0.0f,
            isReachable = true, details = "OK"
        )
        val result = agent.dispatchTool("diagnoseConnection", """{"nodeId": "n1"}""")
        assertContains(result, "reachable")
        assertContains(result, "1.5")
    }

    // ---- Fixture tool dispatch ----

    @Test
    fun listFixturesViaDispatch() = runTest {
        fixtureController.fixtures = listOf(
            Fixture3D(
                fixture = Fixture("fix-1", "Par Can", 1, 6, 0),
                position = Vec3(1f, 2f, 3f)
            )
        )
        val result = agent.dispatchTool("listFixtures")
        assertContains(result, "1 fixtures")
        assertContains(result, "Par Can")
    }

    @Test
    fun fireFixtureViaDispatch() = runTest {
        fixtureController.fireResult = true
        val result = agent.dispatchTool(
            "fireFixture",
            """{"fixtureId": "fix-1", "colorHex": "#FF0000"}"""
        )
        assertContains(result, "fix-1")
        assertEquals("fix-1", fixtureController.lastFiredFixtureId)
    }

    @Test
    fun setFixtureGroupViaDispatch() = runTest {
        fixtureController.groupResult = true
        val result = agent.dispatchTool(
            "setFixtureGroup",
            """{"groupName": "front", "fixtureIds": ["fix-1", "fix-2"]}"""
        )
        assertContains(result, "front")
        assertEquals("front", fixtureController.lastGroupName)
    }

    // ---- State tool dispatch ----

    @Test
    fun getEngineStateViaDispatch() = runTest {
        stateController.fakeEngineState = EngineStateSnapshot(
            isRunning = true, layerCount = 2, masterDimmer = 0.9f,
            fixtureCount = 8, fps = 60f,
            effectIds = listOf("solid_color", "strobe")
        )
        val result = agent.dispatchTool("getEngineState")
        assertContains(result, "running")
        assertContains(result, "2 layers")
    }

    @Test
    fun getBeatStateViaDispatch() = runTest {
        stateController.fakeBeatState = BeatStateSnapshot(
            bpm = 140f, beatPhase = 0.3f, barPhase = 0.1f,
            isRunning = true, source = "TapTempo"
        )
        val result = agent.dispatchTool("getBeatState")
        assertContains(result, "140.0")
        assertContains(result, "TapTempo")
    }

    @Test
    fun getNetworkStateViaDispatch() = runTest {
        stateController.fakeNetworkState = NetworkStateSnapshot(
            nodeCount = 2, totalUniverses = 4, isOutputActive = true,
            protocol = "Art-Net", frameRate = 40
        )
        val result = agent.dispatchTool("getNetworkState")
        assertContains(result, "2 nodes")
        assertContains(result, "4 universes")
    }

    // ---- Error handling ----

    @Test
    fun unknownToolReturnsError() = runTest {
        val result = agent.dispatchTool("nonexistentTool")
        assertContains(result, "Unknown tool")
    }

    @Test
    fun malformedJsonReturnsError() = runTest {
        val result = agent.dispatchTool("setEffect", "not valid json")
        assertContains(result, "Error")
    }

    // ---- Offline mode ----

    @Test
    fun sendWithoutApiKeyReturnsUnavailable() = runTest {
        val result = agent.send("set lights to red")
        assertContains(result, "unavailable")
    }

    @Test
    fun toolsWorkWithoutApiKey() = runTest {
        // Tools should work even without API key (direct dispatch)
        val result = agent.dispatchTool("getEngineState")
        assertContains(result, "Engine")
    }

    // ---- Multi-step workflow ----

    @Test
    fun multiToolSceneCreationWorkflow() = runTest {
        // Simulate what the LLM would do: set up effects, then save scene
        agent.dispatchTool("setEffect", """{"layer": 0, "effectId": "solid_color", "params": {"r": 1.0}}""")
        agent.dispatchTool("setMasterDimmer", """{"value": 0.8}""")
        agent.dispatchTool("setColorPalette", """{"colors": ["#FF0000", "#0000FF"]}""")
        agent.dispatchTool("setTempoMultiplier", """{"multiplier": 1.5}""")
        val result = agent.dispatchTool("createScene", """{"name": "Red Pulse"}""")

        assertContains(result, "Red Pulse")
        assertNotNull(sceneStore.load("Red Pulse"))
        assertEquals("solid_color", engineController.lastSetEffectId)
        assertEquals(0.8f, engineController.lastMasterDimmer)
        assertEquals(2, engineController.lastPalette.size)
        assertEquals(1.5f, engineController.lastTempoMultiplier)
    }
}
