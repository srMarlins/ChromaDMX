package com.chromadmx.agent

import com.chromadmx.agent.config.AgentConfig
import com.chromadmx.agent.scene.SceneStore
import com.chromadmx.agent.tools.FakeEngineController
import com.chromadmx.agent.tools.FakeFixtureController
import com.chromadmx.agent.tools.FakeNetworkController
import com.chromadmx.agent.tools.FakeStateController
import com.chromadmx.agent.tools.buildToolRegistry
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LightingAgentTest {
    private val engine = FakeEngineController()
    private val network = FakeNetworkController()
    private val fixture = FakeFixtureController()
    private val state = FakeStateController()
    private val sceneStore = SceneStore()

    private fun createAgent(apiKey: String = ""): LightingAgent {
        val registry = buildToolRegistry(engine, network, fixture, state, sceneStore)
        return LightingAgent(
            config = AgentConfig(apiKey = apiKey),
            toolRegistry = registry,
        )
    }

    @Test
    fun agentWithoutApiKeyIsNotAvailable() {
        val agent = createAgent()
        assertFalse(agent.isAvailable)
    }

    @Test
    fun agentWithApiKeyIsAvailable() {
        val agent = createAgent(apiKey = "test-key")
        assertTrue(agent.isAvailable)
    }

    @Test
    fun sendWithoutApiKeyReturnsUnavailable() = runTest {
        val agent = createAgent()
        val result = agent.send("set lights to red")
        assertContains(result, "unavailable")
    }

    @Test
    fun clearHistoryResetsConversation() = runTest {
        val agent = createAgent()
        agent.clearHistory()
        assertEquals(0, agent.conversationHistory.value.size)
    }

    @Test
    fun dispatchToolSetsEffect() = runTest {
        val agent = createAgent()
        val result = agent.dispatchTool("setEffect", """{"layer": 0, "effectId": "solid_color"}""")
        assertContains(result, "solid_color")
        assertEquals("solid_color", engine.lastSetEffectId)
    }

    @Test
    fun dispatchToolSetsMasterDimmer() = runTest {
        val agent = createAgent()
        val result = agent.dispatchTool("setMasterDimmer", """{"value": 0.5}""")
        assertContains(result, "0.5")
        assertEquals(0.5f, engine.lastMasterDimmer)
    }

    @Test
    fun dispatchToolGetEngineState() = runTest {
        val agent = createAgent()
        val result = agent.dispatchTool("getEngineState")
        assertContains(result, "Engine")
    }

    @Test
    fun dispatchToolUnknownReturnsError() = runTest {
        val agent = createAgent()
        val result = agent.dispatchTool("unknownTool")
        assertContains(result, "Unknown tool")
    }

    @Test
    fun toolRegistryHasAllTools() {
        val agent = createAgent()
        assertEquals(17, agent.toolNames.size)
    }

    @Test
    fun isProcessingStartsFalse() {
        val agent = createAgent()
        assertFalse(agent.isProcessing.value)
    }
}
