package com.chromadmx.agent

import com.chromadmx.agent.tools.FakeEngineController
import com.chromadmx.agent.tools.FakeStateController
import com.chromadmx.agent.controller.EngineStateSnapshot
import com.chromadmx.engine.effect.EffectRegistry
import com.chromadmx.engine.effect.EffectStack
import com.chromadmx.engine.preset.PresetLibrary
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SimulatedLightingAgentTest {
    private val engine = FakeEngineController()
    private val state = FakeStateController()
    private val effectRegistry = EffectRegistry()
    private val library = PresetLibrary(FakeFileStorage(), effectRegistry, EffectStack())

    private fun createAgent(): SimulatedLightingAgent = SimulatedLightingAgent(
        engineController = engine,
        stateController = state,
        presetLibrary = library,
        effectRegistry = effectRegistry,
    )

    // ── Availability ────────────────────────────────────────────────────

    @Test
    fun isAlwaysAvailable() {
        val agent = createAgent()
        assertTrue(agent.isAvailable)
    }

    @Test
    fun isProcessingStartsFalse() {
        val agent = createAgent()
        assertFalse(agent.isProcessing.value)
    }

    // ── Color commands ──────────────────────────────────────────────────

    @Test
    fun setBlueColor() {
        val agent = createAgent()
        val response = agent.processMessage("set the lights to blue")
        assertContains(response, "blue")
        assertContains(response, "#0000FF")
        assertEquals("solid-color", engine.lastSetEffectId)
        assertEquals(0, engine.lastSetEffectLayer)
        assertEquals(listOf("#0000FF"), engine.lastPalette)
    }

    @Test
    fun setRedColor() {
        val agent = createAgent()
        val response = agent.processMessage("make it red")
        assertContains(response, "red")
        assertContains(response, "#FF0000")
        assertEquals("solid-color", engine.lastSetEffectId)
    }

    @Test
    fun setGreenColor() {
        val agent = createAgent()
        val response = agent.processMessage("green please")
        assertContains(response, "green")
        assertContains(response, "#00FF00")
        assertEquals("solid-color", engine.lastSetEffectId)
    }

    @Test
    fun setWhiteColor() {
        val agent = createAgent()
        val response = agent.processMessage("I want white lights")
        assertContains(response, "white")
        assertContains(response, "#FFFFFF")
        assertEquals("solid-color", engine.lastSetEffectId)
    }

    @Test
    fun setWarmAmberColor() {
        val agent = createAgent()
        val response = agent.processMessage("set to warm amber")
        assertContains(response, "warm amber")
        assertContains(response, "#FFBF00")
        assertEquals("solid-color", engine.lastSetEffectId)
    }

    @Test
    fun setCyanColor() {
        val agent = createAgent()
        val response = agent.processMessage("cyan lighting")
        assertContains(response, "cyan")
        assertContains(response, "#00FFFF")
    }

    @Test
    fun setPurpleColor() {
        val agent = createAgent()
        val response = agent.processMessage("purple vibes")
        assertContains(response, "purple")
        assertContains(response, "#8000FF")
    }

    // ── Dimmer commands ─────────────────────────────────────────────────

    @Test
    fun dimToPercentage() {
        val agent = createAgent()
        val response = agent.processMessage("dim to 50%")
        assertContains(response, "50%")
        assertEquals(0.5f, engine.lastMasterDimmer)
    }

    @Test
    fun dimToPercentageWithoutPercent() {
        val agent = createAgent()
        val response = agent.processMessage("dim to 75")
        assertContains(response, "75%")
        assertEquals(0.75f, engine.lastMasterDimmer)
    }

    @Test
    fun dimWithoutValueSetsFiftyPercent() {
        val agent = createAgent()
        val response = agent.processMessage("dim the lights")
        assertContains(response, "50%")
        assertEquals(0.5f, engine.lastMasterDimmer)
    }

    @Test
    fun blackout() {
        val agent = createAgent()
        val response = agent.processMessage("blackout")
        assertContains(response, "0%")
        assertEquals(0.0f, engine.lastMasterDimmer)
    }

    @Test
    fun fullBrightness() {
        val agent = createAgent()
        val response = agent.processMessage("full brightness please")
        assertContains(response, "100%")
        assertEquals(1.0f, engine.lastMasterDimmer)
    }

    @Test
    fun brightnessKeyword() {
        val agent = createAgent()
        val response = agent.processMessage("set brightness to 80%")
        assertContains(response, "80%")
        assertEquals(0.8f, engine.lastMasterDimmer)
    }

    // ── Preset commands ─────────────────────────────────────────────────

    @Test
    fun loadPartyPreset() {
        val agent = createAgent()
        val response = agent.processMessage("let's party!")
        // Built-in presets are loaded at PresetLibrary construction
        assertContains(response, "Strobe Storm")
    }

    @Test
    fun loadStrobePreset() {
        val agent = createAgent()
        val response = agent.processMessage("strobe mode")
        assertContains(response, "Strobe Storm")
    }

    @Test
    fun loadChillPreset() {
        val agent = createAgent()
        val response = agent.processMessage("something chill")
        assertContains(response, "Sunset Sweep")
    }

    @Test
    fun loadAmbientPreset() {
        val agent = createAgent()
        val response = agent.processMessage("ambient mood")
        assertContains(response, "Sunset Sweep")
    }

    @Test
    fun loadRelaxPreset() {
        val agent = createAgent()
        val response = agent.processMessage("relax mode")
        assertContains(response, "Sunset Sweep")
    }

    @Test
    fun loadOceanPreset() {
        val agent = createAgent()
        val response = agent.processMessage("ocean vibes")
        assertContains(response, "Ocean Waves")
    }

    @Test
    fun loadRainbowPreset() {
        val agent = createAgent()
        val response = agent.processMessage("rainbow")
        assertContains(response, "Midnight Rainbow")
    }

    @Test
    fun loadNeonPreset() {
        val agent = createAgent()
        val response = agent.processMessage("neon mode")
        assertContains(response, "Neon Pulse")
    }

    @Test
    fun loadFireAndIcePreset() {
        val agent = createAgent()
        val response = agent.processMessage("fire and ice")
        assertContains(response, "Fire & Ice")
    }

    // ── Query commands ──────────────────────────────────────────────────

    @Test
    fun helpCommand() {
        val agent = createAgent()
        val response = agent.processMessage("help")
        assertContains(response, "COLORS")
        assertContains(response, "DIMMER")
        assertContains(response, "PRESETS")
        assertContains(response, "STATUS")
    }

    @Test
    fun whatCanYouDo() {
        val agent = createAgent()
        val response = agent.processMessage("what can you do?")
        assertContains(response, "COLORS")
    }

    @Test
    fun statusCommand() {
        val agent = createAgent()
        state.fakeEngineState = EngineStateSnapshot(
            isRunning = true,
            layerCount = 2,
            masterDimmer = 0.8f,
            fixtureCount = 4,
            fps = 40f,
            effectIds = listOf("solid-color", "strobe")
        )
        val response = agent.processMessage("status")
        assertContains(response, "running")
        assertContains(response, "80%")
        assertContains(response, "solid-color")
        assertContains(response, "4")
    }

    @Test
    fun listEffectsCommand() {
        val agent = createAgent()
        val response = agent.processMessage("list effects")
        // EffectRegistry is empty in test, but we still get a valid response
        assertContains(response, "effect")
    }

    @Test
    fun listPresetsCommand() {
        val agent = createAgent()
        val response = agent.processMessage("show presets")
        // Built-in presets are loaded
        assertContains(response, "Neon Pulse")
        assertContains(response, "Sunset Sweep")
    }

    // ── Creative scene commands ─────────────────────────────────────────

    @Test
    fun sunsetScene() {
        val agent = createAgent()
        val response = agent.processMessage("create a sunset")
        assertContains(response, "sunset")
        assertEquals("gradient-sweep-3d", engine.lastSetEffectId)
        assertEquals(0.8f, engine.lastMasterDimmer)
    }

    @Test
    fun energeticScene() {
        val agent = createAgent()
        val response = agent.processMessage("something energetic")
        assertContains(response, "chase")
        assertEquals("chase-3d", engine.lastSetEffectId)
        assertEquals(1.0f, engine.lastMasterDimmer)
    }

    @Test
    fun calmScene() {
        val agent = createAgent()
        val response = agent.processMessage("make it calm and peaceful")
        assertContains(response, "wave")
        assertEquals("wave-3d", engine.lastSetEffectId)
        assertEquals(0.5f, engine.lastMasterDimmer)
    }

    // ── Direct effect commands ───────────────────────────────────────────

    @Test
    fun gradientEffect() {
        val agent = createAgent()
        val response = agent.processMessage("apply gradient effect")
        assertContains(response, "Gradient Sweep")
        assertEquals("gradient-sweep-3d", engine.lastSetEffectId)
    }

    @Test
    fun chaseEffect() {
        val agent = createAgent()
        val response = agent.processMessage("start a chase")
        assertContains(response, "Chase")
        assertEquals("chase-3d", engine.lastSetEffectId)
    }

    // ── Save/create preset ───────────────────────────────────────────────

    @Test
    fun createPreset() {
        val agent = createAgent()
        val response = agent.processMessage("save as my cool scene")
        assertContains(response, "Saved")
        assertContains(response, "my_cool_scene")
    }

    // ── Unknown commands ────────────────────────────────────────────────

    @Test
    fun unknownCommand() {
        val agent = createAgent()
        val response = agent.processMessage("xyzzy plugh")
        assertContains(response, "didn't quite get")
        assertContains(response, "help")
    }

    // ── Conversation history ────────────────────────────────────────────

    @Test
    fun sendAddsToConversationHistory() = runTest {
        val agent = createAgent()
        val response = agent.send("help")
        val history = agent.conversationHistory.value
        assertEquals(2, history.size)
        assertEquals(ChatRole.USER, history[0].role)
        assertEquals("help", history[0].content)
        assertEquals(ChatRole.ASSISTANT, history[1].role)
        assertEquals(response, history[1].content)
    }

    @Test
    fun clearHistoryResetsConversation() = runTest {
        val agent = createAgent()
        agent.send("help")
        agent.clearHistory()
        assertEquals(0, agent.conversationHistory.value.size)
    }

    @Test
    fun isProcessingReturnsFalseAfterSend() = runTest {
        val agent = createAgent()
        agent.send("status")
        assertFalse(agent.isProcessing.value)
    }

    // ── Edge cases ──────────────────────────────────────────────────────

    @Test
    fun dimmerClampedTo100() {
        val agent = createAgent()
        val response = agent.processMessage("dim to 200%")
        assertContains(response, "100%")
        assertEquals(1.0f, engine.lastMasterDimmer)
    }

    @Test
    fun caseInsensitiveColorCommand() {
        val agent = createAgent()
        val response = agent.processMessage("BLUE LIGHTS")
        assertContains(response, "blue")
        assertEquals("solid-color", engine.lastSetEffectId)
    }

    @Test
    fun caseInsensitivePresetCommand() {
        val agent = createAgent()
        val response = agent.processMessage("PARTY TIME")
        assertContains(response, "Strobe Storm")
    }
}
