# Agent Module (Koog) Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build the AI-powered lighting director co-pilot using JetBrains Koog SDK with 17 class-based tools across 4 categories, session management, pre-generation workflow, and full test coverage.

**Architecture:** Koog agent with class-based tools (`SimpleTool<Args>`) for full KMP compatibility (no reflection). Agent runs on background coroutine, tools produce deterministic state changes on the engine/networking layers. Anthropic Claude as the LLM provider, with graceful offline degradation.

**Tech Stack:** JetBrains Koog 0.6.3, kotlinx.serialization, kotlinx.coroutines, Koin DI, kotlin.test

---

## Reference Files

These existing files are critical context for this module:

- **Effect engine:** `shared/engine/src/commonMain/kotlin/com/chromadmx/engine/pipeline/EffectEngine.kt`
- **Effect stack:** `shared/engine/src/commonMain/kotlin/com/chromadmx/engine/effect/EffectStack.kt`
- **Effect registry:** `shared/engine/src/commonMain/kotlin/com/chromadmx/engine/effect/EffectRegistry.kt`
- **SpatialEffect interface:** `shared/engine/src/commonMain/kotlin/com/chromadmx/engine/effect/SpatialEffect.kt`
- **Networking discovery:** `shared/networking/src/commonMain/kotlin/com/chromadmx/networking/discovery/NodeDiscovery.kt`
- **DMX output:** `shared/networking/src/commonMain/kotlin/com/chromadmx/networking/output/DmxOutputService.kt`
- **DmxNode model:** `shared/networking/src/commonMain/kotlin/com/chromadmx/networking/model/DmxNode.kt`
- **Core models:** `shared/core/src/commonMain/kotlin/com/chromadmx/core/model/` (Color, Vec3, BeatState, Fixture, BlendMode, DMXUniverse)
- **EffectParams:** `shared/core/src/commonMain/kotlin/com/chromadmx/core/EffectParams.kt`
- **BeatClock interface:** `shared/tempo/src/commonMain/kotlin/com/chromadmx/tempo/clock/BeatClock.kt`
- **Simulation Koin module (DI pattern):** `shared/simulation/src/commonMain/kotlin/com/chromadmx/simulation/di/SimulationModule.kt`
- **Version catalog:** `gradle/libs.versions.toml`
- **Settings:** `settings.gradle.kts`
- **Convention plugin pattern:** `build-logic/convention/src/main/kotlin/KmpLibraryConventionPlugin.kt`

---

### Task 1: Module Scaffold & Koog Dependencies

**Files:**
- Create: `shared/agent/build.gradle.kts`
- Create: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/.gitkeep` (placeholder)
- Create: `shared/agent/src/commonTest/kotlin/com/chromadmx/agent/.gitkeep` (placeholder)
- Create: `shared/agent/src/androidMain/kotlin/com/chromadmx/agent/.gitkeep` (placeholder)
- Create: `shared/agent/src/iosMain/kotlin/com/chromadmx/agent/.gitkeep` (placeholder)
- Modify: `gradle/libs.versions.toml` — add Koog version and library entries
- Modify: `settings.gradle.kts` — add `include(":shared:agent")`

**Step 1: Add Koog to version catalog**

In `gradle/libs.versions.toml`, add under `[versions]`:
```toml
koog = "0.6.3"
```

Add under `[libraries]`:
```toml
# Koog AI Agent
koog-agents = { module = "ai.koog:koog-agents", version.ref = "koog" }
koog-agents-core = { module = "ai.koog:agents-core", version.ref = "koog" }
koog-agents-tools = { module = "ai.koog:agents-tools", version.ref = "koog" }
koog-agents-ext = { module = "ai.koog:agents-ext", version.ref = "koog" }
koog-executor-anthropic = { module = "ai.koog:prompt-executor-anthropic-client", version.ref = "koog" }
koog-agents-test = { module = "ai.koog:agents-test", version.ref = "koog" }
```

**Step 2: Add Maven repository for Koog nightly (if not on Maven Central)**

In `settings.gradle.kts`, inside `dependencyResolutionManagement.repositories`, add:
```kotlin
maven("https://packages.jetbrains.team/maven/p/grazi/grazie-platform-public")
```

**Step 3: Add module to settings**

In `settings.gradle.kts`, add:
```kotlin
include(":shared:agent")
```

**Step 4: Create build.gradle.kts**

Create `shared/agent/build.gradle.kts`:
```kotlin
plugins {
    id("chromadmx.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":shared:core"))
            implementation(project(":shared:engine"))
            implementation(project(":shared:networking"))
            implementation(project(":shared:tempo"))
            implementation(libs.koog.agents.core)
            implementation(libs.koog.agents.tools)
            implementation(libs.koog.agents.ext)
            implementation(libs.koog.executor.anthropic)
        }

        commonTest.dependencies {
            implementation(libs.koog.agents.test)
        }
    }
}

android {
    namespace = "com.chromadmx.agent"
}
```

**Step 5: Create source directories and verify build compiles**

Run: `./gradlew :shared:agent:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL (empty module compiles)

**Step 6: Commit**

```bash
git add shared/agent/ gradle/libs.versions.toml settings.gradle.kts
git commit -m "Scaffold shared:agent module with Koog dependencies (#9)"
```

---

### Task 2: Agent Configuration & API Key Abstraction

**Files:**
- Create: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/config/AgentConfig.kt`
- Create: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/config/ApiKeyProvider.kt`
- Create: `shared/agent/src/androidMain/kotlin/com/chromadmx/agent/config/ApiKeyProvider.android.kt`
- Create: `shared/agent/src/iosMain/kotlin/com/chromadmx/agent/config/ApiKeyProvider.ios.kt`
- Create: `shared/agent/src/commonTest/kotlin/com/chromadmx/agent/config/AgentConfigTest.kt`

**Step 1: Write test for AgentConfig**

```kotlin
package com.chromadmx.agent.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AgentConfigTest {
    @Test
    fun defaultConfigHasReasonableDefaults() {
        val config = AgentConfig()
        assertEquals(30, config.maxIterations)
        assertEquals(0.7f, config.temperature)
        assertFalse(config.isAvailable)
    }

    @Test
    fun configWithApiKeyIsAvailable() {
        val config = AgentConfig(apiKey = "test-key-123")
        assertTrue(config.isAvailable)
    }

    @Test
    fun configWithBlankApiKeyIsNotAvailable() {
        val config = AgentConfig(apiKey = "  ")
        assertFalse(config.isAvailable)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :shared:agent:testDebugUnitTest --tests "*.AgentConfigTest" -v`
Expected: FAIL (class not found)

**Step 3: Implement AgentConfig**

`AgentConfig.kt`:
```kotlin
package com.chromadmx.agent.config

data class AgentConfig(
    val apiKey: String = "",
    val maxIterations: Int = 30,
    val temperature: Float = 0.7f,
) {
    val isAvailable: Boolean get() = apiKey.isNotBlank()
}
```

**Step 4: Implement expect/actual ApiKeyProvider**

`ApiKeyProvider.kt` (commonMain):
```kotlin
package com.chromadmx.agent.config

expect class ApiKeyProvider {
    fun getAnthropicKey(): String?
}
```

`ApiKeyProvider.android.kt` (androidMain):
```kotlin
package com.chromadmx.agent.config

actual class ApiKeyProvider {
    actual fun getAnthropicKey(): String? {
        return System.getenv("ANTHROPIC_API_KEY")
    }
}
```

`ApiKeyProvider.ios.kt` (iosMain):
```kotlin
package com.chromadmx.agent.config

actual class ApiKeyProvider {
    actual fun getAnthropicKey(): String? {
        // iOS: read from environment or bundled config
        return platform.Foundation.NSProcessInfo.processInfo
            .environment["ANTHROPIC_API_KEY"] as? String
    }
}
```

**Step 5: Run tests**

Run: `./gradlew :shared:agent:testDebugUnitTest --tests "*.AgentConfigTest"`
Expected: PASS

**Step 6: Commit**

```bash
git add shared/agent/
git commit -m "Add AgentConfig and ApiKeyProvider expect/actual (#9)"
```

---

### Task 3: Scene Tools (7 tools)

**Files:**
- Create: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/tools/SceneTools.kt`
- Create: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/model/Scene.kt`
- Create: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/scene/SceneStore.kt`
- Create: `shared/agent/src/commonTest/kotlin/com/chromadmx/agent/tools/SceneToolsTest.kt`
- Create: `shared/agent/src/commonTest/kotlin/com/chromadmx/agent/scene/SceneStoreTest.kt`

**Step 1: Write SceneStore tests**

Test that SceneStore can save and load scenes. A Scene is a named snapshot of effect stack state (layers, params, master dimmer, color palette, tempo multiplier).

```kotlin
package com.chromadmx.agent.scene

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SceneStoreTest {
    @Test
    fun saveAndLoadScene() {
        val store = SceneStore()
        val scene = Scene(
            name = "Techno Pulse",
            layers = listOf(
                Scene.LayerConfig(effectId = "radial_pulse_3d", params = mapOf("speed" to 1.5f), blendMode = "ADDITIVE", opacity = 0.8f)
            ),
            masterDimmer = 0.9f,
            colorPalette = listOf("#FF0000", "#0000FF"),
            tempoMultiplier = 1.0f
        )
        store.save(scene)
        val loaded = store.load("Techno Pulse")
        assertNotNull(loaded)
        assertEquals("Techno Pulse", loaded.name)
        assertEquals(1, loaded.layers.size)
        assertEquals(0.9f, loaded.masterDimmer)
    }

    @Test
    fun loadNonexistentReturnsNull() {
        val store = SceneStore()
        assertNull(store.load("nope"))
    }

    @Test
    fun listScenesReturnsAllNames() {
        val store = SceneStore()
        store.save(Scene(name = "A"))
        store.save(Scene(name = "B"))
        val names = store.list()
        assertTrue(names.contains("A"))
        assertTrue(names.contains("B"))
        assertEquals(2, names.size)
    }

    @Test
    fun saveOverwritesExisting() {
        val store = SceneStore()
        store.save(Scene(name = "X", masterDimmer = 0.5f))
        store.save(Scene(name = "X", masterDimmer = 1.0f))
        assertEquals(1.0f, store.load("X")!!.masterDimmer)
        assertEquals(1, store.list().size)
    }
}
```

**Step 2: Run test to verify failure, then implement Scene and SceneStore**

`Scene.kt`:
```kotlin
package com.chromadmx.agent.scene

import kotlinx.serialization.Serializable

@Serializable
data class Scene(
    val name: String,
    val layers: List<LayerConfig> = emptyList(),
    val masterDimmer: Float = 1.0f,
    val colorPalette: List<String> = emptyList(),
    val tempoMultiplier: Float = 1.0f,
) {
    @Serializable
    data class LayerConfig(
        val effectId: String = "",
        val params: Map<String, Float> = emptyMap(),
        val blendMode: String = "NORMAL",
        val opacity: Float = 1.0f,
    )
}
```

`SceneStore.kt`:
```kotlin
package com.chromadmx.agent.scene

class SceneStore {
    private val scenes = mutableMapOf<String, Scene>()

    fun save(scene: Scene) { scenes[scene.name] = scene }
    fun load(name: String): Scene? = scenes[name]
    fun list(): List<String> = scenes.keys.toList()
    fun delete(name: String): Boolean = scenes.remove(name) != null
}
```

**Step 3: Write SceneTools tests**

Test each of the 7 scene tools individually. The tools delegate to an `EngineController` interface that wraps the real engine APIs:

```kotlin
package com.chromadmx.agent.tools

import com.chromadmx.agent.scene.Scene
import com.chromadmx.agent.scene.SceneStore
import com.chromadmx.core.model.BlendMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertContains

class SceneToolsTest {
    // Fake engine controller for testing
    private val controller = FakeEngineController()
    private val sceneStore = SceneStore()

    @Test
    fun setEffectAppliesEffectToLayer() {
        val tool = SetEffectTool(controller)
        val result = tool.executeBlocking(SetEffectTool.Args(layer = 0, effectId = "solid_color", params = mapOf("r" to 1.0f)))
        assertContains(result, "solid_color")
        assertEquals("solid_color", controller.lastSetEffectId)
    }

    @Test
    fun setBlendModeUpdatesLayer() {
        val tool = SetBlendModeTool(controller)
        val result = tool.executeBlocking(SetBlendModeTool.Args(layer = 0, mode = "ADDITIVE"))
        assertContains(result, "ADDITIVE")
    }

    @Test
    fun setMasterDimmerClampsValue() {
        val tool = SetMasterDimmerTool(controller)
        tool.executeBlocking(SetMasterDimmerTool.Args(value = 1.5f))
        assertEquals(1.0f, controller.lastMasterDimmer)
    }

    @Test
    fun setColorPaletteStoresColors() {
        val tool = SetColorPaletteTool(controller)
        tool.executeBlocking(SetColorPaletteTool.Args(colors = listOf("#FF0000", "#00FF00")))
        assertEquals(2, controller.lastPalette.size)
    }

    @Test
    fun setTempoMultiplierUpdates() {
        val tool = SetTempoMultiplierTool(controller)
        tool.executeBlocking(SetTempoMultiplierTool.Args(multiplier = 2.0f))
        assertEquals(2.0f, controller.lastTempoMultiplier)
    }

    @Test
    fun createSceneSavesToStore() {
        val tool = CreateSceneTool(controller, sceneStore)
        tool.executeBlocking(CreateSceneTool.Args(name = "Test Scene"))
        val saved = sceneStore.load("Test Scene")
        assertTrue(saved != null)
    }

    @Test
    fun loadSceneRestoresFromStore() {
        sceneStore.save(Scene(name = "Saved", masterDimmer = 0.5f))
        val tool = LoadSceneTool(controller, sceneStore)
        val result = tool.executeBlocking(LoadSceneTool.Args(name = "Saved"))
        assertContains(result, "Saved")
    }

    @Test
    fun loadSceneReturnsErrorForMissing() {
        val tool = LoadSceneTool(controller, sceneStore)
        val result = tool.executeBlocking(LoadSceneTool.Args(name = "Nope"))
        assertContains(result, "not found")
    }
}
```

**Step 4: Implement EngineController interface and FakeEngineController**

Create `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/controller/EngineController.kt`:
```kotlin
package com.chromadmx.agent.controller

import com.chromadmx.agent.scene.Scene

interface EngineController {
    fun setEffect(layer: Int, effectId: String, params: Map<String, Float>)
    fun setBlendMode(layer: Int, mode: String)
    fun setMasterDimmer(value: Float)
    fun setColorPalette(colors: List<String>)
    fun setTempoMultiplier(multiplier: Float)
    fun captureScene(): Scene
    fun applyScene(scene: Scene)
}
```

Create `shared/agent/src/commonTest/kotlin/com/chromadmx/agent/tools/FakeEngineController.kt`:
```kotlin
package com.chromadmx.agent.tools

import com.chromadmx.agent.controller.EngineController
import com.chromadmx.agent.scene.Scene

class FakeEngineController : EngineController {
    var lastSetEffectId: String = ""
    var lastMasterDimmer: Float = 1.0f
    var lastPalette: List<String> = emptyList()
    var lastTempoMultiplier: Float = 1.0f
    var lastBlendMode: String = "NORMAL"
    var lastAppliedScene: Scene? = null

    override fun setEffect(layer: Int, effectId: String, params: Map<String, Float>) {
        lastSetEffectId = effectId
    }
    override fun setBlendMode(layer: Int, mode: String) { lastBlendMode = mode }
    override fun setMasterDimmer(value: Float) { lastMasterDimmer = value.coerceIn(0f, 1f) }
    override fun setColorPalette(colors: List<String>) { lastPalette = colors }
    override fun setTempoMultiplier(multiplier: Float) { lastTempoMultiplier = multiplier }
    override fun captureScene(): Scene = Scene(name = "capture", masterDimmer = lastMasterDimmer)
    override fun applyScene(scene: Scene) { lastAppliedScene = scene; lastMasterDimmer = scene.masterDimmer }
}
```

**Step 5: Implement the 7 scene tools as SimpleTool classes**

Each tool follows this pattern (example for SetEffectTool):

```kotlin
package com.chromadmx.agent.tools

import com.chromadmx.agent.controller.EngineController
import com.chromadmx.agent.scene.SceneStore
import kotlinx.serialization.Serializable

class SetEffectTool(private val controller: EngineController) {
    @Serializable
    data class Args(val layer: Int, val effectId: String, val params: Map<String, Float> = emptyMap())

    fun executeBlocking(args: Args): String {
        controller.setEffect(args.layer, args.effectId, args.params)
        return "Applied effect '${args.effectId}' to layer ${args.layer}"
    }
}

// Similarly for SetBlendModeTool, SetMasterDimmerTool, SetColorPaletteTool,
// SetTempoMultiplierTool, CreateSceneTool, LoadSceneTool
```

NOTE: These are the core logic classes. The Koog `SimpleTool` wrappers will be created in Task 7 when we wire the agent together. This keeps the business logic testable without Koog dependencies.

**Step 6: Run all tests, verify pass, commit**

Run: `./gradlew :shared:agent:testDebugUnitTest`
Expected: ALL PASS

```bash
git add shared/agent/
git commit -m "Add Scene tools with SceneStore and EngineController (#9)"
```

---

### Task 4: Network Tools (4 tools)

**Files:**
- Create: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/controller/NetworkController.kt`
- Create: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/tools/NetworkTools.kt`
- Create: `shared/agent/src/commonTest/kotlin/com/chromadmx/agent/tools/NetworkToolsTest.kt`
- Create: `shared/agent/src/commonTest/kotlin/com/chromadmx/agent/tools/FakeNetworkController.kt`

**Step 1: Write NetworkController interface**

```kotlin
package com.chromadmx.agent.controller

import com.chromadmx.agent.model.NodeStatusResult
import com.chromadmx.agent.model.DiagnosticResult

interface NetworkController {
    suspend fun scanNetwork(): List<NodeSummary>
    suspend fun getNodeStatus(nodeId: String): NodeStatusResult?
    suspend fun configureNode(nodeId: String, universe: Int, startAddress: Int): Boolean
    suspend fun diagnoseConnection(nodeId: String): DiagnosticResult?
}

data class NodeSummary(val id: String, val ip: String, val name: String, val universes: List<Int>)
```

**Step 2: Write tests for each network tool**

Tests follow same pattern as SceneTools — use FakeNetworkController, assert tool output strings contain expected info and controller was called correctly.

**Step 3: Implement ScanNetworkTool, GetNodeStatusTool, ConfigureNodeTool, DiagnoseConnectionTool**

Each tool wraps the NetworkController suspend functions. Same pattern as scene tools — business logic classes first, Koog wrappers in Task 7.

**Step 4: Run tests, commit**

```bash
git commit -m "Add Network tools: scan, status, configure, diagnose (#9)"
```

---

### Task 5: Fixture Tools (3 tools)

**Files:**
- Create: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/controller/FixtureController.kt`
- Create: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/tools/FixtureTools.kt`
- Create: `shared/agent/src/commonTest/kotlin/com/chromadmx/agent/tools/FixtureToolsTest.kt`
- Create: `shared/agent/src/commonTest/kotlin/com/chromadmx/agent/tools/FakeFixtureController.kt`

**Step 1: Write FixtureController interface**

```kotlin
package com.chromadmx.agent.controller

import com.chromadmx.core.model.Fixture3D

interface FixtureController {
    fun listFixtures(): List<Fixture3D>
    fun fireFixture(fixtureId: String, colorHex: String): Boolean
    fun setFixtureGroup(groupName: String, fixtureIds: List<String>): Boolean
}
```

**Step 2: Write tests → implement tools → run tests → commit**

```bash
git commit -m "Add Fixture tools: list, fire, group (#9)"
```

---

### Task 6: State Tools (3 tools)

**Files:**
- Create: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/controller/StateController.kt`
- Create: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/tools/StateTools.kt`
- Create: `shared/agent/src/commonTest/kotlin/com/chromadmx/agent/tools/StateToolsTest.kt`

**Step 1: Write StateController interface**

```kotlin
package com.chromadmx.agent.controller

interface StateController {
    fun getEngineState(): EngineStateSnapshot
    fun getBeatState(): BeatStateSnapshot
    fun getNetworkState(): NetworkStateSnapshot
}
```

State snapshots are serializable data classes that summarize current state as strings for the LLM.

**Step 2: Write tests → implement tools → run tests → commit**

```bash
git commit -m "Add State tools: engine, beat, network state queries (#9)"
```

---

### Task 7: Koog Agent Wiring & Tool Registry

**Files:**
- Create: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/LightingAgent.kt`
- Create: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/tools/KoogToolAdapters.kt`
- Create: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/AgentSystemPrompt.kt`
- Create: `shared/agent/src/commonTest/kotlin/com/chromadmx/agent/LightingAgentTest.kt`

**Step 1: Create Koog tool adapters**

Wrap each business logic tool class in a Koog `SimpleTool<Args>`:

```kotlin
package com.chromadmx.agent.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable

class KoogSetEffectTool(private val inner: SetEffectTool) : SimpleTool<KoogSetEffectTool.Args>(
    argsSerializer = Args.serializer(),
    name = "setEffect",
    description = "Apply a spatial effect to a layer with optional parameters"
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Layer index (0-based)")
        val layer: Int,
        @property:LLMDescription("Effect ID from the registry (e.g., 'solid_color', 'rainbow_sweep_3d')")
        val effectId: String,
        @property:LLMDescription("Effect parameters as key-value pairs")
        val params: Map<String, Float> = emptyMap()
    )

    override suspend fun execute(args: Args): String {
        return inner.executeBlocking(SetEffectTool.Args(args.layer, args.effectId, args.params))
    }
}
// ... similar for all 17 tools
```

**Step 2: Create system prompt**

```kotlin
package com.chromadmx.agent

object AgentSystemPrompt {
    val PROMPT = """
        You are a lighting director co-pilot for live electronic music events.
        You control DMX lighting fixtures through tools. You understand lighting design,
        color theory, music genres, and DMX networking.

        When asked to create a mood or scene, translate the creative intent into specific
        effect parameters: color palettes, movement speeds, spatial patterns, and beat
        synchronization settings.

        When troubleshooting, use diagnostic tools to identify issues before suggesting
        fixes. Always explain what you're doing and why.

        Available effects: solid_color, gradient_sweep_3d, rainbow_sweep_3d, strobe,
        chase_3d, wave_3d, radial_pulse_3d, perlin_noise_3d, particle_burst_3d.

        Blend modes: NORMAL, ADDITIVE, MULTIPLY, OVERLAY.
    """.trimIndent()
}
```

**Step 3: Create LightingAgent**

```kotlin
package com.chromadmx.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import com.chromadmx.agent.config.AgentConfig
import com.chromadmx.agent.controller.*
import com.chromadmx.agent.scene.SceneStore
import com.chromadmx.agent.tools.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LightingAgent(
    private val config: AgentConfig,
    private val engineController: EngineController,
    private val networkController: NetworkController,
    private val fixtureController: FixtureController,
    private val stateController: StateController,
    private val sceneStore: SceneStore,
) {
    private val _conversationHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val conversationHistory: StateFlow<List<ChatMessage>> = _conversationHistory.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    suspend fun send(userMessage: String): String {
        if (!config.isAvailable) return "Agent unavailable — no API key configured."

        _isProcessing.value = true
        _conversationHistory.value += ChatMessage(role = "user", content = userMessage)

        return try {
            val response = executeAgentQuery(userMessage)
            _conversationHistory.value += ChatMessage(role = "assistant", content = response)
            response
        } catch (e: Exception) {
            val error = "Error: ${e.message}"
            _conversationHistory.value += ChatMessage(role = "assistant", content = error)
            error
        } finally {
            _isProcessing.value = false
        }
    }

    private suspend fun executeAgentQuery(message: String): String {
        // Build Koog agent with all tools and execute
        // Implementation creates AIAgent with tool registry and runs the query
        TODO("Wire Koog AIAgent here")
    }

    fun clearHistory() {
        _conversationHistory.value = emptyList()
    }
}

data class ChatMessage(val role: String, val content: String, val toolCalls: List<String> = emptyList())
```

**Step 4: Write tests that verify agent construction, unavailable mode, conversation tracking**

**Step 5: Run tests, commit**

```bash
git commit -m "Wire Koog agent with tool registry and system prompt (#9)"
```

---

### Task 8: Pre-generation Workflow

**Files:**
- Create: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/pregen/PreGenerationService.kt`
- Create: `shared/agent/src/commonTest/kotlin/com/chromadmx/agent/pregen/PreGenerationServiceTest.kt`

**Step 1: Write tests for batch scene generation**

```kotlin
class PreGenerationServiceTest {
    @Test
    fun generateScenesCreatesRequestedCount() { ... }

    @Test
    fun generatedScenesAreSavedToStore() { ... }

    @Test
    fun generationProgressUpdatesFlow() { ... }

    @Test
    fun cancelStopsGeneration() { ... }
}
```

**Step 2: Implement PreGenerationService**

Orchestrates batch "Generate N scenes for [genre]" requests. Sends prompts to agent, collects createScene tool calls, saves results to SceneStore.

```kotlin
class PreGenerationService(
    private val agent: LightingAgent,
    private val sceneStore: SceneStore,
) {
    private val _progress = MutableStateFlow(PreGenProgress())
    val progress: StateFlow<PreGenProgress> = _progress.asStateFlow()

    suspend fun generate(genre: String, count: Int): List<Scene> { ... }
    fun cancel() { ... }
}

data class PreGenProgress(val current: Int = 0, val total: Int = 0, val isRunning: Boolean = false)
```

**Step 3: Run tests, commit**

```bash
git commit -m "Add PreGenerationService for batch scene creation (#9)"
```

---

### Task 9: Real EngineController Implementation

**Files:**
- Create: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/controller/RealEngineController.kt`
- Create: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/controller/RealNetworkController.kt`
- Create: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/controller/RealFixtureController.kt`
- Create: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/controller/RealStateController.kt`

**Step 1: Implement RealEngineController**

Bridges from EngineController interface to the actual EffectEngine, EffectStack, EffectRegistry. Reads/writes real engine state.

**Step 2: Implement RealNetworkController**

Bridges from NetworkController to NodeDiscovery and DmxOutputService.

**Step 3: Implement RealFixtureController and RealStateController**

**Step 4: Commit**

```bash
git commit -m "Add real controller implementations bridging to engine/network (#9)"
```

---

### Task 10: Koin DI Module

**Files:**
- Create: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/di/AgentModule.kt`

**Step 1: Create Koin module**

Follow the pattern from `shared/simulation/src/commonMain/kotlin/com/chromadmx/simulation/di/SimulationModule.kt`:

```kotlin
package com.chromadmx.agent.di

import com.chromadmx.agent.LightingAgent
import com.chromadmx.agent.config.AgentConfig
import com.chromadmx.agent.config.ApiKeyProvider
import com.chromadmx.agent.controller.*
import com.chromadmx.agent.pregen.PreGenerationService
import com.chromadmx.agent.scene.SceneStore
import org.koin.core.module.Module
import org.koin.dsl.module

val agentModule: Module = module {
    single { ApiKeyProvider() }
    single { AgentConfig(apiKey = get<ApiKeyProvider>().getAnthropicKey() ?: "") }
    single { SceneStore() }
    single<EngineController> { RealEngineController(get(), get(), get()) }
    single<NetworkController> { RealNetworkController(get()) }
    single<FixtureController> { RealFixtureController(get()) }
    single<StateController> { RealStateController(get(), get(), get()) }
    single { LightingAgent(get(), get(), get(), get(), get(), get()) }
    single { PreGenerationService(get(), get()) }
}
```

**Step 2: Commit**

```bash
git commit -m "Add Koin agentModule for DI wiring (#9)"
```

---

### Task 11: Integration Tests with Mocked LLM

**Files:**
- Create: `shared/agent/src/commonTest/kotlin/com/chromadmx/agent/integration/AgentIntegrationTest.kt`

**Step 1: Write integration tests using fake controllers and (if Koog test utils support it) mocked LLM responses**

Tests verify:
- Tool dispatch: "set the lights to red" → triggers SetEffectTool
- Multi-tool sequences: "create a techno scene" → sets effect + palette + saves scene
- Error handling: bad tool args produce graceful error messages
- Offline mode: no API key → graceful "unavailable" response
- State queries: "what's the current BPM?" → triggers getBeatState tool

If Koog `agents-test` doesn't support mock LLM, test at the tool dispatch level — call tools directly and verify results.

**Step 2: Run full test suite**

Run: `./gradlew :shared:agent:testDebugUnitTest`
Expected: ALL PASS

**Step 3: Commit**

```bash
git commit -m "Add agent integration tests with mocked tool dispatch (#9)"
```

---

### Task 12: Wire Agent Module into Shared Umbrella

**Files:**
- Modify: `shared/build.gradle.kts` — add `api(project(":shared:agent"))`

**Step 1: Add agent dependency to shared umbrella**

In `shared/build.gradle.kts`, add to `commonMain.dependencies`:
```kotlin
api(project(":shared:agent"))
```

**Step 2: Verify full build**

Run: `./gradlew :shared:agent:testDebugUnitTest :shared:core:testDebugUnitTest :shared:engine:testDebugUnitTest :shared:networking:testDebugUnitTest`
Expected: ALL PASS

**Step 3: Commit**

```bash
git commit -m "Wire shared:agent into umbrella module (#9)"
```
