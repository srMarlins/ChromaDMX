# Koog Agent Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace stubbed LLM integration with real Koog SDK wiring — ReAct strategy, class-based tools, Anthropic executor, history compression, event tracking.

**Architecture:** 17 tools extend Koog's `SimpleTool<Args>` with `@LLMDescription` annotations. A custom ReAct strategy graph with history compression wraps them in an `AIAgent` connected via `simpleAnthropicExecutor`. Event handlers bridge tool lifecycle events to StateFlow for UI observability. Offline mode preserved — tools work without LLM.

**Tech Stack:** Koog SDK 0.6.3, Kotlin Multiplatform, Anthropic Claude Sonnet 4.5, kotlinx.serialization, Koin DI, coroutines + StateFlow.

---

### Task 1: Update Dependencies

**Files:**
- Modify: `gradle/libs.versions.toml:34-93`
- Modify: `shared/agent/build.gradle.kts:1-19`

**Step 1: Clean up version catalog**

In `gradle/libs.versions.toml`, replace the Koog library entries. The `koog-agents` umbrella bundles everything except `agents-ext`. Remove granular entries that are subsumed.

Replace lines 87-93:
```toml
# Koog AI Agent
koog-agents = { module = "ai.koog:koog-agents", version.ref = "koog" }
koog-agents-ext = { module = "ai.koog:agents-ext", version.ref = "koog" }
```

**Step 2: Add Koog dependencies to agent module**

In `shared/agent/build.gradle.kts`, add the Koog deps to `commonMain.dependencies`:
```kotlin
plugins {
    id("chromadmx.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":shared:core"))
            implementation(project(":shared:engine"))
            implementation(project(":shared:networking"))
            implementation(project(":shared:tempo"))
            implementation(libs.kotlinx.atomicfu)
            implementation(libs.koog.agents)
            implementation(libs.koog.agents.ext)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "com.chromadmx.agent"
}
```

Note: the `kotlin-serialization` plugin is required because Koog's `SimpleTool<Args>` uses `@Serializable` args with serializer references.

**Step 3: Verify the build compiles**

Run: `./gradlew :shared:agent:compileKotlinAndroid --no-daemon`
Expected: BUILD SUCCESSFUL (dependencies resolve from Maven Central)

**Step 4: Commit**

```bash
git add gradle/libs.versions.toml shared/agent/build.gradle.kts
git commit -m "feat(agent): add Koog SDK dependencies (koog-agents + agents-ext)"
```

---

### Task 2: Migrate Scene Tools to Koog SimpleTool

**Files:**
- Modify: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/tools/SceneTools.kt`
- Modify: `shared/agent/src/commonTest/kotlin/com/chromadmx/agent/tools/SceneToolsTest.kt`

**Step 1: Rewrite SceneTools.kt**

All 7 tools in this file become Koog `SimpleTool<Args>` subclasses. Each tool gets:
- `name` matching the existing tool name string
- `description` that the LLM reads to decide when to use it
- `@property:LLMDescription` on every `Args` field
- `override suspend fun execute(args): String`

```kotlin
package com.chromadmx.agent.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import com.chromadmx.agent.controller.EngineController
import com.chromadmx.agent.scene.SceneStore
import kotlinx.serialization.Serializable

class SetEffectTool(private val controller: EngineController) : SimpleTool<SetEffectTool.Args>(
    argsSerializer = Args.serializer(),
    name = "setEffect",
    description = "Apply a spatial lighting effect to a layer. Effects: solid_color, gradient_sweep_3d, rainbow_sweep_3d, strobe, chase_3d, wave_3d, radial_pulse_3d, perlin_noise_3d, particle_burst_3d."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Layer index (0-based) to apply the effect to")
        val layer: Int,
        @property:LLMDescription("Effect ID from the registry (e.g. solid_color, strobe, wave_3d)")
        val effectId: String,
        @property:LLMDescription("Optional effect parameters as name-value pairs (e.g. speed=2.0, intensity=0.8)")
        val params: Map<String, Float> = emptyMap()
    )

    override suspend fun execute(args: Args): String {
        val success = controller.setEffect(args.layer, args.effectId, args.params)
        if (!success) return "Effect '${args.effectId}' not found in registry."
        return "Applied effect '${args.effectId}' to layer ${args.layer}" +
            if (args.params.isNotEmpty()) " with params ${args.params}" else ""
    }
}

class SetBlendModeTool(private val controller: EngineController) : SimpleTool<SetBlendModeTool.Args>(
    argsSerializer = Args.serializer(),
    name = "setBlendMode",
    description = "Set the blend mode for a layer. Modes: NORMAL, ADDITIVE, MULTIPLY, OVERLAY."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Layer index (0-based)")
        val layer: Int,
        @property:LLMDescription("Blend mode: NORMAL, ADDITIVE, MULTIPLY, or OVERLAY")
        val mode: String
    )

    override suspend fun execute(args: Args): String {
        controller.setBlendMode(args.layer, args.mode)
        return "Set blend mode for layer ${args.layer} to ${args.mode}"
    }
}

class SetMasterDimmerTool(private val controller: EngineController) : SimpleTool<SetMasterDimmerTool.Args>(
    argsSerializer = Args.serializer(),
    name = "setMasterDimmer",
    description = "Set the master dimmer level. 0.0 = blackout, 1.0 = full brightness."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Dimmer level from 0.0 (blackout) to 1.0 (full brightness)")
        val value: Float
    )

    override suspend fun execute(args: Args): String {
        val clamped = args.value.coerceIn(0f, 1f)
        controller.setMasterDimmer(clamped)
        return "Set master dimmer to $clamped"
    }
}

class SetColorPaletteTool(private val controller: EngineController) : SimpleTool<SetColorPaletteTool.Args>(
    argsSerializer = Args.serializer(),
    name = "setColorPalette",
    description = "Set the active color palette used by effects. Provide hex color strings."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("List of hex color strings (e.g. #FF0000, #00FF00, #0000FF)")
        val colors: List<String>
    )

    override suspend fun execute(args: Args): String {
        controller.setColorPalette(args.colors)
        return "Set color palette to ${args.colors.size} colors: ${args.colors.joinToString(", ")}"
    }
}

class SetTempoMultiplierTool(private val controller: EngineController) : SimpleTool<SetTempoMultiplierTool.Args>(
    argsSerializer = Args.serializer(),
    name = "setTempoMultiplier",
    description = "Set the tempo multiplier for beat-synced effects. 1.0 = normal speed, 2.0 = double speed, 0.5 = half speed."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Tempo multiplier (e.g. 0.5 for half speed, 2.0 for double)")
        val multiplier: Float
    )

    override suspend fun execute(args: Args): String {
        controller.setTempoMultiplier(args.multiplier)
        return "Set tempo multiplier to ${args.multiplier}x"
    }
}

class CreateSceneTool(
    private val controller: EngineController,
    private val sceneStore: SceneStore
) : SimpleTool<CreateSceneTool.Args>(
    argsSerializer = Args.serializer(),
    name = "createScene",
    description = "Capture the current engine state (effects, dimmer, palette, tempo) and save it as a named scene for later recall."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Name for the saved scene (e.g. 'dark_techno', 'warm_ambient')")
        val name: String
    )

    override suspend fun execute(args: Args): String {
        val scene = controller.captureScene().copy(name = args.name)
        sceneStore.save(scene)
        return "Created scene '${args.name}' with ${scene.layers.size} layers"
    }
}

class LoadSceneTool(
    private val controller: EngineController,
    private val sceneStore: SceneStore
) : SimpleTool<LoadSceneTool.Args>(
    argsSerializer = Args.serializer(),
    name = "loadScene",
    description = "Load a previously saved scene by name and apply it to the engine."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Name of the scene to load")
        val name: String
    )

    override suspend fun execute(args: Args): String {
        val scene = sceneStore.load(args.name)
            ?: return "Scene '${args.name}' not found. Available: ${sceneStore.list().joinToString(", ")}"
        controller.applyScene(scene)
        return "Loaded scene '${args.name}' with ${scene.layers.size} layers, dimmer=${scene.masterDimmer}"
    }
}
```

**Step 2: Update SceneToolsTest.kt**

The tool `execute` methods are now `suspend`. Update all test calls to use `runTest` and add `suspend` where needed. The test logic stays the same — just wrap in `runTest` and await.

```kotlin
package com.chromadmx.agent.tools

import com.chromadmx.agent.scene.SceneStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class SceneToolsTest {
    private val engine = FakeEngineController()
    private val sceneStore = SceneStore()

    @Test
    fun setEffectAppliesEffect() = runTest {
        val tool = SetEffectTool(engine)
        val result = tool.execute(SetEffectTool.Args(layer = 0, effectId = "solid_color"))
        assertContains(result, "solid_color")
        assertEquals("solid_color", engine.lastSetEffectId)
    }

    @Test
    fun setEffectReturnsErrorForUnknownEffect() = runTest {
        val engine = FakeEngineController().apply { nextSetEffectResult = false }
        val tool = SetEffectTool(engine)
        val result = tool.execute(SetEffectTool.Args(layer = 0, effectId = "unknown"))
        assertContains(result, "not found")
    }

    @Test
    fun setBlendModeSetsMode() = runTest {
        val tool = SetBlendModeTool(engine)
        val result = tool.execute(SetBlendModeTool.Args(layer = 1, mode = "ADDITIVE"))
        assertContains(result, "ADDITIVE")
        assertEquals("ADDITIVE", engine.lastBlendMode)
    }

    @Test
    fun setMasterDimmerClamps() = runTest {
        val tool = SetMasterDimmerTool(engine)
        val result = tool.execute(SetMasterDimmerTool.Args(value = 1.5f))
        assertContains(result, "1.0")
    }

    @Test
    fun setMasterDimmerClampsNegative() = runTest {
        val tool = SetMasterDimmerTool(engine)
        val result = tool.execute(SetMasterDimmerTool.Args(value = -0.5f))
        assertContains(result, "0.0")
    }

    @Test
    fun setColorPaletteSetsColors() = runTest {
        val tool = SetColorPaletteTool(engine)
        val result = tool.execute(SetColorPaletteTool.Args(colors = listOf("#FF0000", "#00FF00")))
        assertContains(result, "2 colors")
    }

    @Test
    fun setTempoMultiplierSetsValue() = runTest {
        val tool = SetTempoMultiplierTool(engine)
        val result = tool.execute(SetTempoMultiplierTool.Args(multiplier = 2.0f))
        assertContains(result, "2.0")
    }

    @Test
    fun createSceneSavesToStore() = runTest {
        val tool = CreateSceneTool(engine, sceneStore)
        val result = tool.execute(CreateSceneTool.Args(name = "my_scene"))
        assertContains(result, "my_scene")
        assertEquals("my_scene", sceneStore.list().first())
    }

    @Test
    fun loadSceneApplies() = runTest {
        CreateSceneTool(engine, sceneStore).execute(CreateSceneTool.Args(name = "test"))
        val result = LoadSceneTool(engine, sceneStore).execute(LoadSceneTool.Args(name = "test"))
        assertContains(result, "Loaded scene")
    }

    @Test
    fun loadSceneNotFoundReturnsError() = runTest {
        val tool = LoadSceneTool(engine, sceneStore)
        val result = tool.execute(LoadSceneTool.Args(name = "nonexistent"))
        assertContains(result, "not found")
    }
}
```

Note: `FakeEngineController` needs a `nextSetEffectResult` field. Add to `FakeEngineController.kt`:
```kotlin
var nextSetEffectResult: Boolean = true

override fun setEffect(layer: Int, effectId: String, params: Map<String, Float>): Boolean {
    lastSetEffectId = effectId
    lastSetEffectLayer = layer
    lastSetEffectParams = params
    return nextSetEffectResult
}
```

**Step 3: Run tests**

Run: `./gradlew :shared:agent:testDebugUnitTest --tests "com.chromadmx.agent.tools.SceneToolsTest" --no-daemon`
Expected: All 10 tests PASS

**Step 4: Commit**

```bash
git add shared/agent/src/
git commit -m "feat(agent): migrate scene tools to Koog SimpleTool"
```

---

### Task 3: Migrate Network Tools to Koog SimpleTool

**Files:**
- Modify: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/tools/NetworkTools.kt`
- Modify: `shared/agent/src/commonTest/kotlin/com/chromadmx/agent/tools/NetworkToolsTest.kt`

**Step 1: Rewrite NetworkTools.kt**

`ScanNetworkTool` has no args — use Koog's `Tool.NoArgs` pattern. The others get `SimpleTool<Args>`.

```kotlin
package com.chromadmx.agent.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import com.chromadmx.agent.controller.NetworkController
import kotlinx.serialization.Serializable

class ScanNetworkTool(private val controller: NetworkController) : SimpleTool<ScanNetworkTool.Args>(
    argsSerializer = Args.serializer(),
    name = "scanNetwork",
    description = "Scan the local network for DMX Art-Net/sACN nodes. Returns a list of discovered nodes with their IPs and universes."
) {
    @Serializable
    class Args

    override suspend fun execute(args: Args): String {
        val nodes = controller.scanNetwork()
        if (nodes.isEmpty()) {
            return "Found 0 nodes on the network. Check that nodes are powered on and on the same subnet."
        }
        val listing = nodes.joinToString("\n") { node ->
            "  - ${node.name} (${node.ip}) [universes: ${node.universes.joinToString(",")}]"
        }
        return "Found ${nodes.size} nodes:\n$listing"
    }
}

class GetNodeStatusTool(private val controller: NetworkController) : SimpleTool<GetNodeStatusTool.Args>(
    argsSerializer = Args.serializer(),
    name = "getNodeStatus",
    description = "Get detailed status of a specific DMX node including online status, firmware version, and packet count."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("The node ID to query (from scanNetwork results)")
        val nodeId: String
    )

    override suspend fun execute(args: Args): String {
        val status = controller.getNodeStatus(args.nodeId)
            ?: return "Node '${args.nodeId}' not found. Run scanNetwork first."
        val onlineStr = if (status.isOnline) "online" else "offline"
        return "Node '${status.name}' (${status.ip}): $onlineStr, " +
            "universes=${status.universes}, firmware=${status.firmwareVersion}, " +
            "packets_sent=${status.packetsSent}"
    }
}

class ConfigureNodeTool(private val controller: NetworkController) : SimpleTool<ConfigureNodeTool.Args>(
    argsSerializer = Args.serializer(),
    name = "configureNode",
    description = "Configure a DMX node's universe assignment and start address."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("The node ID to configure")
        val nodeId: String,
        @property:LLMDescription("Universe number to assign (0-32767)")
        val universe: Int,
        @property:LLMDescription("DMX start address (1-512)")
        val startAddress: Int
    )

    override suspend fun execute(args: Args): String {
        val success = controller.configureNode(args.nodeId, args.universe, args.startAddress)
        return if (success) {
            "Configured node '${args.nodeId}': universe=${args.universe}, startAddress=${args.startAddress}"
        } else {
            "Failed to configure node '${args.nodeId}'. Check that the node is online and accessible."
        }
    }
}

class DiagnoseConnectionTool(private val controller: NetworkController) : SimpleTool<DiagnoseConnectionTool.Args>(
    argsSerializer = Args.serializer(),
    name = "diagnoseConnection",
    description = "Run a diagnostic test on a DMX node's connection. Reports latency, packet loss, and reachability."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("The node ID to diagnose")
        val nodeId: String
    )

    override suspend fun execute(args: Args): String {
        val result = controller.diagnoseConnection(args.nodeId)
            ?: return "Node '${args.nodeId}' not found. Run scanNetwork first."
        val reachableStr = if (result.isReachable) "reachable" else "unreachable"
        return "Diagnostic for '${args.nodeId}': $reachableStr, " +
            "latency=${result.latencyMs}ms, packetLoss=${result.packetLossPercent}%. " +
            result.details
    }
}
```

**Step 2: Update NetworkToolsTest.kt**

Wrap all test calls in `runTest`, update for `suspend execute`. Same pattern as Task 2.

**Step 3: Run tests**

Run: `./gradlew :shared:agent:testDebugUnitTest --tests "com.chromadmx.agent.tools.NetworkToolsTest" --no-daemon`
Expected: All 10 tests PASS

**Step 4: Commit**

```bash
git add shared/agent/src/
git commit -m "feat(agent): migrate network tools to Koog SimpleTool"
```

---

### Task 4: Migrate Fixture Tools to Koog SimpleTool

**Files:**
- Modify: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/tools/FixtureTools.kt`
- Modify: `shared/agent/src/commonTest/kotlin/com/chromadmx/agent/tools/FixtureToolsTest.kt`

**Step 1: Rewrite FixtureTools.kt**

```kotlin
package com.chromadmx.agent.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import com.chromadmx.agent.controller.FixtureController
import kotlinx.serialization.Serializable

class ListFixturesTool(private val controller: FixtureController) : SimpleTool<ListFixturesTool.Args>(
    argsSerializer = Args.serializer(),
    name = "listFixtures",
    description = "List all known DMX fixtures with their 3D positions, channels, universes, and group assignments."
) {
    @Serializable
    class Args

    override suspend fun execute(args: Args): String {
        val fixtures = controller.listFixtures()
        if (fixtures.isEmpty()) {
            return "0 fixtures configured. Use vision mapping or manual setup to add fixtures."
        }
        val listing = fixtures.joinToString("\n") { f ->
            val pos = f.position
            "  - ${f.fixture.name} (${f.fixture.fixtureId}): " +
                "pos=(${pos.x}, ${pos.y}, ${pos.z}), " +
                "ch=${f.fixture.channelStart}-${f.fixture.channelStart + f.fixture.channelCount - 1}, " +
                "universe=${f.fixture.universeId}" +
                (f.groupId?.let { ", group=$it" } ?: "")
        }
        return "${fixtures.size} fixtures:\n$listing"
    }
}

class FireFixtureTool(private val controller: FixtureController) : SimpleTool<FireFixtureTool.Args>(
    argsSerializer = Args.serializer(),
    name = "fireFixture",
    description = "Flash a single fixture with a specific color for identification purposes. Helps identify which physical fixture corresponds to which ID."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("The fixture ID to flash")
        val fixtureId: String,
        @property:LLMDescription("Hex color to flash (e.g. #FF0000 for red, #FFFFFF for white)")
        val colorHex: String
    )

    override suspend fun execute(args: Args): String {
        val success = controller.fireFixture(args.fixtureId, args.colorHex)
        return if (success) {
            "Identified fixture '${args.fixtureId}' with color ${args.colorHex}"
        } else {
            "Fixture '${args.fixtureId}' not found. Check fixture ID."
        }
    }
}

class SetFixtureGroupTool(private val controller: FixtureController) : SimpleTool<SetFixtureGroupTool.Args>(
    argsSerializer = Args.serializer(),
    name = "setFixtureGroup",
    description = "Assign one or more fixtures to a named group for batch control (e.g. 'stage_left', 'wash_lights')."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Name for the fixture group")
        val groupName: String,
        @property:LLMDescription("List of fixture IDs to include in the group")
        val fixtureIds: List<String>
    )

    override suspend fun execute(args: Args): String {
        val success = controller.setFixtureGroup(args.groupName, args.fixtureIds)
        return if (success) {
            "Created group '${args.groupName}' with ${args.fixtureIds.size} fixtures"
        } else {
            "Failed to create group '${args.groupName}'. Check fixture IDs."
        }
    }
}
```

**Step 2: Update FixtureToolsTest.kt** — wrap in `runTest`, same pattern.

**Step 3: Run tests**

Run: `./gradlew :shared:agent:testDebugUnitTest --tests "com.chromadmx.agent.tools.FixtureToolsTest" --no-daemon`
Expected: All 7 tests PASS

**Step 4: Commit**

```bash
git add shared/agent/src/
git commit -m "feat(agent): migrate fixture tools to Koog SimpleTool"
```

---

### Task 5: Migrate State Tools to Koog SimpleTool

**Files:**
- Modify: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/tools/StateTools.kt`
- Modify: `shared/agent/src/commonTest/kotlin/com/chromadmx/agent/tools/StateToolsTest.kt`

**Step 1: Rewrite StateTools.kt**

All three state tools have no args (they read current state).

```kotlin
package com.chromadmx.agent.tools

import ai.koog.agents.core.tools.SimpleTool
import com.chromadmx.agent.controller.StateController
import kotlinx.serialization.Serializable

class GetEngineStateTool(private val controller: StateController) : SimpleTool<GetEngineStateTool.Args>(
    argsSerializer = Args.serializer(),
    name = "getEngineState",
    description = "Get the current effect engine state: running status, active layers, fixture count, master dimmer, FPS, and active effect IDs."
) {
    @Serializable
    class Args

    override suspend fun execute(args: Args): String {
        val state = controller.getEngineState()
        val runStr = if (state.isRunning) "running" else "stopped"
        return "Engine: $runStr, ${state.layerCount} layers, " +
            "${state.fixtureCount} fixtures, dimmer=${state.masterDimmer}, " +
            "fps=${state.fps}. Active effects: ${state.effectIds.joinToString(", ").ifEmpty { "none" }}"
    }
}

class GetBeatStateTool(private val controller: StateController) : SimpleTool<GetBeatStateTool.Args>(
    argsSerializer = Args.serializer(),
    name = "getBeatState",
    description = "Get the current beat/tempo state: BPM, beat phase, bar phase, running status, and tempo source."
) {
    @Serializable
    class Args

    override suspend fun execute(args: Args): String {
        val state = controller.getBeatState()
        val runStr = if (state.isRunning) "running" else "stopped"
        return "Beat: $runStr, ${state.bpm} BPM, " +
            "beatPhase=${state.beatPhase}, barPhase=${state.barPhase}, " +
            "source=${state.source}"
    }
}

class GetNetworkStateTool(private val controller: StateController) : SimpleTool<GetNetworkStateTool.Args>(
    argsSerializer = Args.serializer(),
    name = "getNetworkState",
    description = "Get the current DMX network state: node count, universes, output status, protocol, and frame rate."
) {
    @Serializable
    class Args

    override suspend fun execute(args: Args): String {
        val state = controller.getNetworkState()
        val outputStr = if (state.isOutputActive) "active" else "inactive"
        return "Network: ${state.nodeCount} nodes, ${state.totalUniverses} universes, " +
            "output=$outputStr, protocol=${state.protocol}, frameRate=${state.frameRate}Hz"
    }
}
```

**Step 2: Update StateToolsTest.kt** — wrap in `runTest`, same pattern.

**Step 3: Run tests**

Run: `./gradlew :shared:agent:testDebugUnitTest --tests "com.chromadmx.agent.tools.StateToolsTest" --no-daemon`
Expected: All 6 tests PASS

**Step 4: Commit**

```bash
git add shared/agent/src/
git commit -m "feat(agent): migrate state tools to Koog SimpleTool"
```

---

### Task 6: Replace Custom ToolRegistry with Koog ToolRegistry Factory

**Files:**
- Rewrite: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/tools/ToolRegistry.kt`

**Step 1: Replace the custom ToolRegistry class**

The old class had manual JSON dispatch. Replace with a factory function that builds a Koog `ToolRegistry`:

```kotlin
package com.chromadmx.agent.tools

import ai.koog.agents.core.tools.ToolRegistry
import com.chromadmx.agent.controller.EngineController
import com.chromadmx.agent.controller.FixtureController
import com.chromadmx.agent.controller.NetworkController
import com.chromadmx.agent.controller.StateController
import com.chromadmx.agent.scene.SceneStore

/**
 * Build the Koog ToolRegistry containing all 17 lighting agent tools.
 *
 * Tools are registered with the Koog SDK's type-safe system — the LLM
 * sees tool names, descriptions, and parameter schemas automatically.
 */
fun buildToolRegistry(
    engineController: EngineController,
    networkController: NetworkController,
    fixtureController: FixtureController,
    stateController: StateController,
    sceneStore: SceneStore,
): ToolRegistry {
    return ToolRegistry {
        // Scene/Effect tools
        tool(SetEffectTool(engineController))
        tool(SetBlendModeTool(engineController))
        tool(SetMasterDimmerTool(engineController))
        tool(SetColorPaletteTool(engineController))
        tool(SetTempoMultiplierTool(engineController))

        // Scene management
        tool(CreateSceneTool(engineController, sceneStore))
        tool(LoadSceneTool(engineController, sceneStore))

        // Network tools
        tool(ScanNetworkTool(networkController))
        tool(GetNodeStatusTool(networkController))
        tool(ConfigureNodeTool(networkController))
        tool(DiagnoseConnectionTool(networkController))

        // Fixture tools
        tool(ListFixturesTool(fixtureController))
        tool(FireFixtureTool(fixtureController))
        tool(SetFixtureGroupTool(fixtureController))

        // State tools
        tool(GetEngineStateTool(stateController))
        tool(GetBeatStateTool(stateController))
        tool(GetNetworkStateTool(stateController))
    }
}
```

**Step 2: Verify build**

Run: `./gradlew :shared:agent:compileKotlinAndroid --no-daemon`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared/agent/src/commonMain/kotlin/com/chromadmx/agent/tools/ToolRegistry.kt
git commit -m "feat(agent): replace custom ToolRegistry with Koog ToolRegistry factory"
```

---

### Task 7: Update AgentConfig and AgentSystemPrompt

**Files:**
- Modify: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/config/AgentConfig.kt`
- Modify: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/AgentSystemPrompt.kt`
- Modify: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/ChatMessage.kt`

**Step 1: Extend AgentConfig**

```kotlin
package com.chromadmx.agent.config

/**
 * Configuration for the AI lighting agent.
 *
 * @property apiKey    The Anthropic API key. When blank, the agent operates in offline mode.
 * @property modelId   Which Anthropic model to use. Options: "haiku_4_5", "sonnet_4_5", "opus_4_6".
 * @property maxIterations Maximum tool-calling iterations per request.
 * @property temperature LLM sampling temperature (0.0 = deterministic, 1.0 = creative).
 * @property historyCompressionThreshold Message count before triggering history compression.
 */
data class AgentConfig(
    val apiKey: String = "",
    val modelId: String = "sonnet_4_5",
    val maxIterations: Int = 30,
    val temperature: Float = 0.7f,
    val historyCompressionThreshold: Int = 50,
) {
    /** Whether the agent has a valid API key and can make LLM requests. */
    val isAvailable: Boolean get() = apiKey.isNotBlank()
}
```

**Step 2: Enhance AgentSystemPrompt for Koog prompt DSL**

```kotlin
package com.chromadmx.agent

/**
 * System prompt for the AI lighting director agent.
 *
 * Defines the agent's persona, capabilities, and behavior guidelines.
 * Used by the Koog AIAgent as the system prompt.
 */
object AgentSystemPrompt {
    val PROMPT = """
        You are a lighting director co-pilot for live electronic music events.
        You control DMX lighting fixtures through tools. You understand lighting design,
        color theory, music genres, and DMX networking.

        ## Workflow

        1. Before making changes, use state tools (getEngineState, getBeatState, getNetworkState) to understand the current setup.
        2. When asked to create a mood or scene, translate the creative intent into specific effect parameters: color palettes, movement speeds, spatial patterns, and beat synchronization.
        3. When troubleshooting, use diagnostic tools to identify issues before suggesting fixes.
        4. After setting up a scene you like, use createScene to save it for later recall.
        5. Always explain what you're doing and why.

        ## Available Effects
        solid_color, gradient_sweep_3d, rainbow_sweep_3d, strobe, chase_3d, wave_3d,
        radial_pulse_3d, perlin_noise_3d, particle_burst_3d.

        ## Blend Modes
        NORMAL, ADDITIVE, MULTIPLY, OVERLAY.

        ## Tips
        - Layer effects: use layer 0 for base, layer 1+ for accents.
        - Use ADDITIVE blend for layering colors; MULTIPLY for dramatic shadows.
        - Beat-synced effects respond to the detected BPM — adjust tempoMultiplier to scale.
        - Group fixtures by position (stage_left, stage_right, truss) for targeted control.
    """.trimIndent()
}
```

**Step 3: Enrich ChatMessage with tool call details**

```kotlin
package com.chromadmx.agent

import kotlinx.serialization.Serializable

@Serializable
enum class ChatRole {
    USER,
    ASSISTANT,
    SYSTEM,
    TOOL
}

/**
 * A single message in the agent conversation history.
 */
@Serializable
data class ChatMessage(
    val role: ChatRole,
    val content: String,
    val toolCalls: List<ToolCallRecord> = emptyList()
)

/**
 * Record of a tool call made by the agent.
 */
@Serializable
data class ToolCallRecord(
    val toolName: String,
    val arguments: String = "",
    val result: String = "",
)
```

**Step 4: Update AgentConfigTest for new fields**

In `shared/agent/src/commonTest/kotlin/com/chromadmx/agent/config/AgentConfigTest.kt`, add tests for `modelId` and `historyCompressionThreshold` defaults.

**Step 5: Run tests**

Run: `./gradlew :shared:agent:testDebugUnitTest --tests "com.chromadmx.agent.config.AgentConfigTest" --no-daemon`
Expected: All tests PASS

**Step 6: Commit**

```bash
git add shared/agent/src/
git commit -m "feat(agent): update AgentConfig, system prompt, and ChatMessage for Koog"
```

---

### Task 8: Rewrite LightingAgent with Koog AIAgent

**Files:**
- Rewrite: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/LightingAgent.kt`
- Create: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/LightingAgentStrategy.kt`

This is the core change — wiring the real Koog AIAgent.

**Step 1: Create the custom ReAct strategy with history compression**

```kotlin
package com.chromadmx.agent

import ai.koog.agents.core.agent.context.AIAgentContext
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeLLMCompressHistory
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.core.dsl.extension.nodeLLMSendToolResult
import ai.koog.agents.core.dsl.extension.nodeExecuteTool
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.dsl.extension.onToolCall
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.message.Message

/**
 * Custom ReAct-style strategy for the lighting agent.
 *
 * Flow:
 *   start → LLM request → [assistant message] → finish
 *                        → [tool call] → execute → [compress if needed] → send result → loop
 *
 * History compression triggers when message count exceeds [compressionThreshold].
 */
fun lightingAgentStrategy(compressionThreshold: Int = 50) = strategy<String, String>("lighting-react") {
    val callLLM by nodeLLMRequest()
    val executeTool by nodeExecuteTool()
    val sendResult by nodeLLMSendToolResult()
    val compressHistory by nodeLLMCompressHistory<Message.Tool.Result>()

    edge(nodeStart forwardTo callLLM)
    edge(callLLM forwardTo nodeFinish onAssistantMessage { true })
    edge(callLLM forwardTo executeTool onToolCall { true })

    edge(executeTool forwardTo compressHistory onCondition {
        llm.readSession { prompt.messages.size > compressionThreshold }
    })
    edge(executeTool forwardTo sendResult)

    edge(compressHistory forwardTo sendResult)

    edge(sendResult forwardTo nodeFinish onAssistantMessage { true })
    edge(sendResult forwardTo executeTool onToolCall { true })
}
```

Note: The exact Koog DSL imports may need adjustment based on the actual package structure — verify against the 0.6.3 API. The key node types (`nodeLLMRequest`, `nodeExecuteTool`, `nodeLLMSendToolResult`, `nodeLLMCompressHistory`) are in `ai.koog.agents.core.dsl.extension` or `ai.koog.agents.ext`.

**Step 2: Rewrite LightingAgent.kt**

```kotlin
package com.chromadmx.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.EventHandler
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.llms.all.simpleAnthropicExecutor
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.llm.LLModel
import com.chromadmx.agent.config.AgentConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The AI lighting director agent.
 *
 * When an API key is configured, wraps a Koog [AIAgent] with a ReAct strategy
 * that autonomously reasons and calls tools. When offline, tools are still
 * accessible via [dispatchTool] for direct programmatic use.
 */
class LightingAgent(
    private val config: AgentConfig,
    val toolRegistry: ToolRegistry,
) {
    private val _conversationHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val conversationHistory: StateFlow<List<ChatMessage>> = _conversationHistory.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _toolCallsInFlight = MutableStateFlow<List<ToolCallRecord>>(emptyList())
    /** Tools currently being executed by the agent (for UI progress). */
    val toolCallsInFlight: StateFlow<List<ToolCallRecord>> = _toolCallsInFlight.asStateFlow()

    val isAvailable: Boolean get() = config.isAvailable

    /** Resolve the Anthropic model from config. */
    private fun resolveModel(): LLModel = when (config.modelId) {
        "haiku_4_5" -> AnthropicModels.Haiku_4_5
        "sonnet_4" -> AnthropicModels.Sonnet_4
        "sonnet_4_5" -> AnthropicModels.Sonnet_4_5
        "opus_4_6" -> AnthropicModels.Opus_4_6
        else -> AnthropicModels.Sonnet_4_5
    }

    /** Lazily create the Koog AIAgent (only when API key is available). */
    private val koogAgent: AIAgent? by lazy {
        if (!config.isAvailable) return@lazy null

        val executor = simpleAnthropicExecutor(config.apiKey)
        val model = resolveModel()

        AIAgent(
            promptExecutor = executor,
            strategy = lightingAgentStrategy(config.historyCompressionThreshold),
            agentConfig = AIAgentConfig(
                prompt = prompt("lighting-agent") {
                    system(AgentSystemPrompt.PROMPT)
                },
                model = model,
                maxAgentIterations = config.maxIterations,
                temperature = config.temperature.toDouble(),
            ),
            toolRegistry = toolRegistry,
        ) {
            install(EventHandler) {
                onToolCallStarting { event ->
                    val record = ToolCallRecord(
                        toolName = event.tool.name,
                        arguments = event.toolArgs.toString()
                    )
                    _toolCallsInFlight.update { it + record }
                }
                onToolCallCompleted { event ->
                    _toolCallsInFlight.update { inflight ->
                        inflight.filterNot { it.toolName == event.tool.name }
                    }
                    _conversationHistory.update { it + ChatMessage(
                        role = ChatRole.TOOL,
                        content = event.result.toString(),
                        toolCalls = listOf(ToolCallRecord(
                            toolName = event.tool.name,
                            result = event.result.toString()
                        ))
                    ) }
                }
                onAgentExecutionFailed { event ->
                    _conversationHistory.update { it + ChatMessage(
                        role = ChatRole.SYSTEM,
                        content = "Agent error: ${event.throwable.message}"
                    ) }
                    _toolCallsInFlight.value = emptyList()
                }
            }
        }
    }

    /**
     * Send a user message to the agent.
     *
     * When available, runs the full Koog ReAct loop — the LLM reasons,
     * calls tools autonomously, and returns a final response.
     */
    suspend fun send(userMessage: String): String {
        if (!config.isAvailable) {
            return "Agent unavailable - no API key configured. Use tools directly or configure an Anthropic API key."
        }

        _isProcessing.value = true
        _conversationHistory.update { it + ChatMessage(role = ChatRole.USER, content = userMessage) }

        return try {
            val response = koogAgent!!.run(userMessage)
            _conversationHistory.update { it + ChatMessage(role = ChatRole.ASSISTANT, content = response) }
            response
        } catch (e: Exception) {
            val error = "Error: ${e.message}"
            _conversationHistory.update { it + ChatMessage(role = ChatRole.ASSISTANT, content = error) }
            error
        } finally {
            _isProcessing.value = false
            _toolCallsInFlight.value = emptyList()
        }
    }

    /**
     * Dispatch a tool call directly (bypasses LLM).
     *
     * Looks up the tool by name in the Koog registry and executes it
     * with the provided JSON arguments. This is the primary interface
     * for programmatic use, UI buttons, and testing.
     */
    suspend fun dispatchTool(toolName: String, argsJson: String = "{}"): String {
        val tool = toolRegistry.tools.find { it.name == toolName }
            ?: return "Unknown tool: '$toolName'. Available: ${toolNames.joinToString(", ")}"
        return try {
            tool.executeRaw(argsJson)
        } catch (e: Exception) {
            "Error executing tool '$toolName': ${e.message}"
        }
    }

    /** All registered tool names. */
    val toolNames: List<String> get() = toolRegistry.tools.map { it.name }

    /** Clear the conversation history. */
    fun clearHistory() {
        _conversationHistory.value = emptyList()
    }
}
```

Note: The `tool.executeRaw(argsJson)` call may need adjustment based on Koog's actual API for raw JSON execution. If `executeRaw` doesn't exist, we can deserialize manually:
```kotlin
val args = Json.decodeFromString(tool.argsSerializer, argsJson)
tool.execute(args)
```
Verify against the 0.6.3 API during implementation.

**Step 3: Verify build**

Run: `./gradlew :shared:agent:compileKotlinAndroid --no-daemon`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add shared/agent/src/commonMain/
git commit -m "feat(agent): wire Koog AIAgent with ReAct strategy, events, and history compression"
```

---

### Task 9: Update DI Module (AgentModule.kt)

**Files:**
- Modify: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/di/AgentModule.kt`

**Step 1: Rewire Koin bindings**

The `LightingAgent` now takes `(AgentConfig, ToolRegistry)` instead of individual controllers. The Koog `ToolRegistry` is built by the factory function.

```kotlin
package com.chromadmx.agent.di

import com.chromadmx.agent.LightingAgent
import com.chromadmx.agent.config.AgentConfig
import com.chromadmx.agent.config.ApiKeyProvider
import com.chromadmx.agent.controller.EngineController
import com.chromadmx.agent.controller.FixtureController
import com.chromadmx.agent.controller.NetworkController
import com.chromadmx.agent.controller.RealEngineController
import com.chromadmx.agent.controller.RealFixtureController
import com.chromadmx.agent.controller.RealNetworkController
import com.chromadmx.agent.controller.RealStateController
import com.chromadmx.agent.controller.StateController
import com.chromadmx.agent.pregen.PreGenerationService
import com.chromadmx.agent.scene.SceneStore
import com.chromadmx.agent.tools.buildToolRegistry
import com.chromadmx.core.model.Fixture3D
import ai.koog.agents.core.tools.ToolRegistry
import org.koin.core.module.Module
import org.koin.dsl.module

val agentModule: Module = module {
    single { ApiKeyProvider() }
    single { AgentConfig(apiKey = get<ApiKeyProvider>().getAnthropicKey() ?: "") }
    single { SceneStore() }
    single<EngineController> { RealEngineController(get(), get()) }
    single<NetworkController> { RealNetworkController(get()) }
    single<FixtureController> {
        val fixturesProvider: () -> List<Fixture3D> = getOrNull() ?: { emptyList() }
        RealFixtureController(fixturesProvider = fixturesProvider)
    }
    single<StateController> {
        val fixturesProvider: () -> List<Fixture3D> = getOrNull() ?: { emptyList() }
        RealStateController(get(), get(), get(), get(), fixturesProvider)
    }
    single<ToolRegistry> {
        buildToolRegistry(
            engineController = get(),
            networkController = get(),
            fixtureController = get(),
            stateController = get(),
            sceneStore = get()
        )
    }
    single { LightingAgent(get(), get()) }
    single { PreGenerationService(get()) }
}
```

**Step 2: Verify build**

Run: `./gradlew :shared:agent:compileKotlinAndroid --no-daemon`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared/agent/src/commonMain/kotlin/com/chromadmx/agent/di/AgentModule.kt
git commit -m "feat(agent): rewire Koin DI for Koog ToolRegistry and LightingAgent"
```

---

### Task 10: Update Tests

**Files:**
- Modify: `shared/agent/src/commonTest/kotlin/com/chromadmx/agent/LightingAgentTest.kt`
- Modify: `shared/agent/src/commonTest/kotlin/com/chromadmx/agent/integration/AgentIntegrationTest.kt`

**Step 1: Update LightingAgentTest**

The `LightingAgent` constructor changed — it now takes `(AgentConfig, ToolRegistry)` instead of individual controllers. The test helper `createAgent` needs updating:

```kotlin
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
        // Add a message by sending without API key (returns unavailable but doesn't add to history)
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
```

**Step 2: Update AgentIntegrationTest**

Same pattern — replace direct constructor calls with `buildToolRegistry` + new `LightingAgent(config, registry)` constructor. The integration tests exercise `dispatchTool` which still works the same way.

**Step 3: Run all agent tests**

Run: `./gradlew :shared:agent:testDebugUnitTest --no-daemon`
Expected: All tests PASS (84+ tests)

**Step 4: Commit**

```bash
git add shared/agent/src/commonTest/
git commit -m "test(agent): update all tests for Koog-based LightingAgent"
```

---

### Task 11: Update AgentViewModel

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/viewmodel/AgentViewModel.kt`

**Step 1: Update ViewModel for enriched ChatMessage**

The `ChatMessage` in the agent module now has `ToolCallRecord` instead of plain strings. Update the ViewModel to use the agent's types directly or map them:

```kotlin
package com.chromadmx.ui.viewmodel

import com.chromadmx.agent.ChatMessage
import com.chromadmx.agent.ChatRole
import com.chromadmx.agent.LightingAgent
import com.chromadmx.agent.ToolCallRecord
import com.chromadmx.agent.pregen.PreGenProgress
import com.chromadmx.agent.pregen.PreGenerationService
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Agent screen.
 *
 * Delegates to the real [LightingAgent] for conversation and tool dispatch,
 * and to [PreGenerationService] for batch scene generation.
 */
class AgentViewModel(
    private val agent: LightingAgent,
    private val preGenService: PreGenerationService,
    private val scope: CoroutineScope,
) {
    /** Conversation messages — sourced from the agent's history. */
    val messages: StateFlow<List<ChatMessage>> = agent.conversationHistory

    /** Whether the agent is currently processing a request. */
    val isProcessing: StateFlow<Boolean> = agent.isProcessing

    /** Tools currently being executed (for real-time progress UI). */
    val toolCallsInFlight: StateFlow<List<ToolCallRecord>> = agent.toolCallsInFlight

    val isAgentAvailable: Boolean get() = agent.isAvailable

    val preGenProgress: StateFlow<PreGenProgress> = preGenService.progress

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        scope.launch {
            agent.send(text)
        }
    }

    fun dispatchTool(toolName: String, argsJson: String = "{}") {
        scope.launch {
            agent.dispatchTool(toolName, argsJson)
        }
    }

    fun generateScenes(genre: String, count: Int) {
        scope.launch {
            preGenService.generate(genre, count)
        }
    }

    fun cancelGeneration() {
        preGenService.cancel()
    }

    fun clearHistory() {
        agent.clearHistory()
    }

    fun onCleared() {
        scope.coroutineContext[Job]?.cancel()
    }
}
```

Key simplifications:
- `messages` now flows directly from `agent.conversationHistory` — no duplicate state
- `isProcessing` flows directly from `agent.isProcessing`
- `toolCallsInFlight` is new — drives real-time "tool executing" UI indicators
- Removed redundant try/catch (agent handles errors internally)

**Step 2: Verify build**

Run: `./gradlew :shared:compileKotlinAndroid --no-daemon`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/chromadmx/ui/viewmodel/AgentViewModel.kt
git commit -m "feat(ui): simplify AgentViewModel to use Koog agent state directly"
```

---

### Task 12: Final Verification and Cleanup

**Step 1: Run full test suite**

Run: `./gradlew :shared:agent:testDebugUnitTest :shared:core:testDebugUnitTest :shared:networking:testDebugUnitTest :shared:tempo:testDebugUnitTest --no-daemon`
Expected: All tests PASS

**Step 2: Build the Android app**

Run: `./gradlew :android:app:assembleDebug --no-daemon`
Expected: BUILD SUCCESSFUL

**Step 3: Verify no compilation warnings about Koog**

Check that there are no unresolved imports or deprecation warnings from the Koog SDK integration.

**Step 4: Final commit**

If any adjustments were needed during verification, commit them:
```bash
git add -A
git commit -m "fix(agent): resolve integration issues from Koog migration"
```
