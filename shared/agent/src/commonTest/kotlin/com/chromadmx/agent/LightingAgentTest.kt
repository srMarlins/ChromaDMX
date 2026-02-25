package com.chromadmx.agent

import com.chromadmx.agent.config.AgentConfig
import com.chromadmx.agent.scene.SceneStore
import com.chromadmx.agent.tools.FakeEngineController
import com.chromadmx.agent.tools.FakeFixtureController
import com.chromadmx.agent.tools.FakeNetworkController
import com.chromadmx.agent.tools.FakeStateController
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.chromadmx.agent.ChatRole

class LightingAgentTest {
    private fun createAgent(apiKey: String = ""): LightingAgent {
        return LightingAgent(
            config = AgentConfig(apiKey = apiKey),
            engineController = FakeEngineController(),
            networkController = FakeNetworkController(),
            fixtureController = FakeFixtureController(),
            stateController = FakeStateController(),
            sceneStore = SceneStore()
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
    fun sendWithApiKeyReturnsPendingMessage() = runTest {
        val agent = createAgent(apiKey = "test-key")
        val result = agent.send("set lights to red")
        assertContains(result, "pending")
    }

    @Test
    fun sendTracksConversationHistory() = runTest {
        val agent = createAgent(apiKey = "test-key")
        agent.send("hello")
        val history = agent.conversationHistory.value
        assertEquals(2, history.size)
        assertEquals(ChatRole.USER, history[0].role)
        assertEquals("hello", history[0].content)
        assertEquals(ChatRole.ASSISTANT, history[1].role)
    }

    @Test
    fun clearHistoryResetsConversation() = runTest {
        val agent = createAgent(apiKey = "test-key")
        agent.send("hello")
        assertEquals(2, agent.conversationHistory.value.size)
        agent.clearHistory()
        assertEquals(0, agent.conversationHistory.value.size)
    }

    @Test
    fun dispatchToolSetsEffect() = runTest {
        val engineController = FakeEngineController()
        val agent = LightingAgent(
            config = AgentConfig(),
            engineController = engineController,
            networkController = FakeNetworkController(),
            fixtureController = FakeFixtureController(),
            stateController = FakeStateController(),
            sceneStore = SceneStore()
        )
        val result = agent.dispatchTool("setEffect", """{"layer": 0, "effectId": "solid_color"}""")
        assertContains(result, "solid_color")
        assertEquals("solid_color", engineController.lastSetEffectId)
    }

    @Test
    fun dispatchToolSetsMasterDimmer() = runTest {
        val engineController = FakeEngineController()
        val agent = LightingAgent(
            config = AgentConfig(),
            engineController = engineController,
            networkController = FakeNetworkController(),
            fixtureController = FakeFixtureController(),
            stateController = FakeStateController(),
            sceneStore = SceneStore()
        )
        val result = agent.dispatchTool("setMasterDimmer", """{"value": 0.5}""")
        assertContains(result, "0.5")
        assertEquals(0.5f, engineController.lastMasterDimmer)
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
