# ChromaDMX Full Sweep Fix — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Fix all bugs found during emulator testing (black fixtures, dialog layout, agent lifecycle) and build all missing feature UIs (layer panel, preset browser, fixture profiles, audience beams, export/import) to make the app fully functional.

**Architecture:** Five phases ordered by dependency. Phase 1 fixes critical bugs that block the app. Phase 2 rewrites the AI agent to use correct Koog lifecycle (AIAgentService + ReAct per-message). Phases 3-4 add missing feature UIs. Phase 5 handles polish and tech debt. Each phase is a separate PR.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Koin DI, Koog 0.6.3 (AI agent SDK), SQLDelight, kotlinx.serialization

---

## Phase 1: Critical Bug Fixes

### Task 1: Diagnose and Fix Black Fixtures

The fixtures show on the stage canvas but never display color, even after tapping presets. The persistence code in `SetupViewModel.persistSetupComplete()` (line 268-280) already saves fixtures. The bug is somewhere in the color pipeline.

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/viewmodel/StageViewModelV2.kt`
- Modify: `shared/engine/src/commonMain/kotlin/com/chromadmx/engine/pipeline/EffectEngine.kt`
- Test: `shared/src/commonTest/kotlin/com/chromadmx/ui/viewmodel/StageViewModelV2Test.kt`

**Step 1: Add diagnostic logging to trace the color pipeline**

In `StageViewModelV2.kt`, add logging to `syncColorsFromEngine()` (around line 125) to verify:
- `engine.fixtures.size` — are fixtures loaded?
- `engine.effectStack.layerCount` — are layers loaded after preset tap?
- `buffer.readSlot().size` — is the triple buffer sized correctly?
- `colors.any { it != Color.BLACK }` — are any colors non-black?

```kotlin
private fun syncColorsFromEngine() {
    val buffer = engine.colorOutput
    buffer.swapRead()
    val colors = buffer.readSlot()
    // Debug: uncomment temporarily to trace the pipeline
    // println("CHROMA_DEBUG fixtures=${engine.fixtures.size} layers=${engine.effectStack.layerCount} bufSize=${colors.size} hasColor=${colors.any { it != Color.BLACK }}")
    _fixtureColors.tryEmit(colors.toList())
}
```

**Step 2: Add logging to `handleApplyScene`**

In `StageViewModelV2.kt` around line 282, verify preset loading succeeds:

```kotlin
private fun handleApplyScene(name: String) {
    val presets = presetLibrary.listPresets()
    val preset = presets.find { it.name == name } ?: run {
        println("CHROMA_DEBUG ApplyScene FAILED: no preset named '$name' in ${presets.map { it.name }}")
        return
    }
    val loaded = presetLibrary.loadPreset(preset.id)
    println("CHROMA_DEBUG ApplyScene loaded=$loaded layers=${effectStack.layerCount} dimmer=${effectStack.masterDimmer}")
    // ...rest of method
}
```

**Step 3: Build, install on emulator, reproduce, check logcat**

```bash
./gradlew :android:app:assembleDebug
adb -s emulator-5554 install -r android/app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell am start -n com.chromadmx.android/.MainActivity
# Tap a preset, then check logcat:
adb -s emulator-5554 logcat -d | grep CHROMA_DEBUG
```

**Step 4: Fix the identified root cause**

Based on logcat output, apply the fix. Likely candidates:
- **If `fixtures.size == 0`**: Race condition — `updateFixtures()` replaces the `colorOutput` TripleBuffer reference, but `syncColorsFromEngine()` captured the old one. Fix: make `colorOutput` access always read the latest via a `@Volatile` annotation or read `engine.colorOutput` fresh each sync.
- **If `layerCount == 0` after preset tap**: Effect ID mismatch between `BuiltInPresets` and `EffectRegistry`. Fix: verify IDs match (e.g., `RadialPulse3DEffect.ID` registered in `ChromaDiModule.kt:96`).
- **If colors are computed but still black on screen**: `DmxColor.toComposeColor()` conversion issue in `VenueCanvas.kt`.

**Step 5: Write regression test**

```kotlin
@Test
fun `applying preset should produce non-black fixture colors`() = runTest {
    // Setup: create engine with fixtures, load preset
    val engine = EffectEngine(scope = this)
    val registry = EffectRegistry().apply {
        register(RadialPulse3DEffect())
        register(GradientSweep3DEffect())
    }
    val fixtures = listOf(
        Fixture3D(id = "f1", name = "Par 1", position = Vec3(0f, 0f, 0f)),
    )
    engine.updateFixtures(fixtures)

    val library = PresetLibrary(InMemoryFileStorage(), registry, engine.effectStack)
    library.loadPreset("builtin_neon_pulse")

    // Act: tick the engine
    engine.tick()
    engine.colorOutput.swapRead()
    val colors = engine.colorOutput.readSlot()

    // Assert: fixture should have non-black color
    assertTrue(colors.isNotEmpty(), "Color buffer should not be empty")
    assertTrue(colors[0] != Color.BLACK, "Fixture color should not be black after loading preset")
}
```

**Step 6: Remove debug logging, commit**

```bash
git add -A && git commit -m "fix: diagnose and fix black fixture colors on stage"
```

---

### Task 2: Fix PixelButton fillMaxWidth Breaking Dialog Layouts

The "Reset" confirm button in the Reset Onboarding dialog is invisible because `PixelButton` hardcodes `.fillMaxWidth()` on its face modifier (line 104). When two buttons are in a Row, they fight for space.

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/components/PixelButton.kt:103-104`
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/components/PixelContainers.kt:326-336`
- Modify: All PixelButton call-sites that need full-width (search codebase)

**Step 1: Remove hardcoded fillMaxWidth from PixelButton face modifier**

In `PixelButton.kt`, line 103-104, change:
```kotlin
// BEFORE:
val faceModifier = Modifier
    .fillMaxWidth()
    .offset { ... }
```
to:
```kotlin
// AFTER:
val faceModifier = Modifier
    .then(if (modifier.toString().contains("fillMaxWidth")) Modifier else Modifier)
    .fillMaxWidth() // Keep — the outer Box already constrains width via `modifier`
```

Actually, the better fix: the outer `Box` on line 80-90 already receives `modifier` from the caller. The face should use `.matchParentSize()` instead of `.fillMaxWidth()`:

```kotlin
// AFTER (line 103-104):
val faceModifier = Modifier
    .matchParentSize()  // Match the outer Box size instead of forcing full width
    .offset { IntOffset(0, (currentOffset - pressDepth).roundToPx()) }
```

Wait — `matchParentSize()` only works inside a `Box` scope. The face IS inside the outer Box. This should work.

**Step 2: Update PixelDialog button row to use weights**

In `PixelContainers.kt`, lines 326-336, update the Row to give buttons equal weight:

```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
    verticalAlignment = Alignment.CenterVertically,
) {
    if (dismissButton != null) {
        Box(modifier = Modifier.weight(1f)) { dismissButton() }
    }
    Box(modifier = Modifier.weight(1f)) { confirmButton() }
}
```

**Step 3: Test on emulator — verify both dialog buttons visible**

```bash
./gradlew :android:app:assembleDebug
# Install, open Settings, tap Reset Onboarding — both "Cancel" and "Reset" should be visible
```

**Step 4: Commit**

```bash
git add -A && git commit -m "fix(ui): PixelButton matchParentSize + dialog button weights

PixelButton face modifier used fillMaxWidth() which caused buttons
in dialog Rows to overflow. Changed to matchParentSize() so the face
fills the outer Box constraint. Dialog action row now uses weight(1f)
for even button distribution."
```

---

## Phase 2: Agent Architecture Rewrite

### Task 3: Create LightingAgentService with Correct Koog Lifecycle

Replace single-instance `koogAgent.run()` with `AIAgentService.createAgentAndRun()` per message.

**Files:**
- Create: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/LightingAgentService.kt`
- Create: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/ConversationStore.kt`
- Modify: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/LightingAgent.kt` (extract interface)
- Modify: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/di/AgentModule.kt`
- Test: `shared/agent/src/commonTest/kotlin/com/chromadmx/agent/LightingAgentServiceTest.kt`

**Step 1: Extract `LightingAgentInterface`**

```kotlin
// LightingAgent.kt — add interface
interface LightingAgentInterface {
    val conversationHistory: StateFlow<List<ChatMessage>>
    val isProcessing: StateFlow<Boolean>
    val isAvailable: Boolean
    suspend fun send(userMessage: String): String
}
```

**Step 2: Create ConversationStore**

```kotlin
// ConversationStore.kt
package com.chromadmx.agent

import com.chromadmx.agent.ChatMessage
import com.chromadmx.agent.ChatRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConversationStore(private val maxMessages: Int = 50) {
    private val _history = MutableStateFlow<List<ChatMessage>>(emptyList())
    val history: StateFlow<List<ChatMessage>> = _history.asStateFlow()

    fun addUserMessage(content: String) {
        append(ChatMessage(role = ChatRole.USER, content = content))
    }

    fun addAssistantMessage(content: String) {
        append(ChatMessage(role = ChatRole.ASSISTANT, content = content))
    }

    fun addSystemMessage(content: String) {
        append(ChatMessage(role = ChatRole.SYSTEM, content = content))
    }

    fun getRecent(count: Int = 10): List<ChatMessage> =
        _history.value.takeLast(count)

    fun clear() { _history.value = emptyList() }

    private fun append(msg: ChatMessage) {
        _history.update { list ->
            val updated = list + msg
            if (updated.size > maxMessages) updated.takeLast(maxMessages) else updated
        }
    }
}
```

**Step 3: Create LightingAgentService**

```kotlin
// LightingAgentService.kt
package com.chromadmx.agent

import ai.koog.agents.core.agent.AIAgentService
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.agent.reActStrategy
import com.chromadmx.agent.config.AgentConfig
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withTimeout

class LightingAgentService(
    private val config: AgentConfig,
    private val toolRegistry: ToolRegistry,
) : LightingAgentInterface {

    private val store = ConversationStore()
    private val _isProcessing = MutableStateFlow(false)

    override val conversationHistory: StateFlow<List<ChatMessage>> = store.history
    override val isProcessing: StateFlow<Boolean> = _isProcessing
    override val isAvailable: Boolean get() = config.isAvailable

    private val service by lazy {
        AIAgentService(
            executor = createExecutor(config),
            strategy = reActStrategy(reasoningInterval = 1),
            llmModel = resolveModel(config),
            systemPrompt = AgentSystemPrompt.PROMPT,
            temperature = config.temperature.toDouble(),
            toolRegistry = toolRegistry,
            maxIterations = config.maxIterations,
        )
    }

    override suspend fun send(userMessage: String): String {
        _isProcessing.value = true
        store.addUserMessage(userMessage)
        return try {
            val prompt = buildContextualPrompt(userMessage)
            withTimeout(15_000L) {
                service.createAgentAndRun(prompt)
            }
        } catch (e: Exception) {
            "Error: ${e.message ?: "unknown error"}"
        } finally {
            _isProcessing.value = false
        }.also { response ->
            store.addAssistantMessage(response)
        }
    }

    private fun buildContextualPrompt(currentMessage: String): String = buildString {
        val recent = store.getRecent(10)
        if (recent.size > 1) { // >1 because we already added the current user msg
            appendLine("## Recent conversation context:")
            recent.dropLast(1).forEach { msg ->
                appendLine("${msg.role}: ${msg.content}")
            }
            appendLine()
        }
        appendLine("## Current request:")
        appendLine(currentMessage)
    }

    companion object {
        // Extracted from old LightingAgent for reuse
        fun createExecutor(config: AgentConfig) = if (config.isGoogleModel) {
            simpleGoogleAIExecutor(config.apiKey)
        } else {
            simpleAnthropicExecutor(config.apiKey)
        }

        fun resolveModel(config: AgentConfig) = when (config.modelId) {
            "gemini_2_0_flash" -> GoogleModels.Gemini2_0Flash
            "gemini_2_5_flash" -> GoogleModels.Gemini2_5Flash
            "gemini_2_5_pro" -> GoogleModels.Gemini2_5Pro
            "haiku_4_5" -> AnthropicModels.Haiku_4_5
            "sonnet_4" -> AnthropicModels.Sonnet_4
            "sonnet_4_5" -> AnthropicModels.Sonnet_4_5
            "opus_4" -> AnthropicModels.Opus_4
            "opus_4_1" -> AnthropicModels.Opus_4_1
            "opus_4_5" -> AnthropicModels.Opus_4_5
            else -> GoogleModels.Gemini2_5Flash
        }
    }
}
```

**Step 4: Update AgentModule DI**

In `AgentModule.kt`, replace the `LightingAgent` singleton:
```kotlin
single<LightingAgentInterface> {
    if (get<AgentConfig>().isAvailable) {
        LightingAgentService(get(), get())
    } else {
        SimulatedLightingAgent(get(), get(), get()) // Task 4
    }
}
```

**Step 5: Write test**

```kotlin
@Test
fun `conversation store maintains history across sends`() = runTest {
    val store = ConversationStore(maxMessages = 5)
    store.addUserMessage("hello")
    store.addAssistantMessage("hi there")
    store.addUserMessage("set color blue")

    val recent = store.getRecent(10)
    assertEquals(3, recent.size)
    assertEquals(ChatRole.USER, recent[0].role)
    assertEquals(ChatRole.ASSISTANT, recent[1].role)
}

@Test
fun `conversation store caps at maxMessages`() = runTest {
    val store = ConversationStore(maxMessages = 3)
    repeat(5) { store.addUserMessage("msg $it") }
    assertEquals(3, store.getRecent(10).size)
}
```

**Step 6: Commit**

```bash
git add -A && git commit -m "feat(agent): rewrite to AIAgentService pattern

Each user message creates a fresh agent via createAgentAndRun()
(correct Koog lifecycle). Conversation context injected into prompt.
ConversationStore manages bounded history. Extracted LightingAgentInterface
for SimulatedAgent fallback."
```

---

### Task 4: Create SimulatedLightingAgent for Offline Mode

**Files:**
- Create: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/SimulatedLightingAgent.kt`
- Test: `shared/agent/src/commonTest/kotlin/com/chromadmx/agent/SimulatedLightingAgentTest.kt`

**Step 1: Implement SimulatedLightingAgent**

A keyword-matching agent that executes REAL tool calls without an LLM. Handles common commands: color changes, dimmer, preset loading, status queries, help.

```kotlin
class SimulatedLightingAgent(
    private val engineController: EngineController,
    private val stateController: StateController,
    private val presetLibrary: PresetLibrary,
) : LightingAgentInterface {
    private val store = ConversationStore()
    private val _isProcessing = MutableStateFlow(false)

    override val conversationHistory = store.history
    override val isProcessing = _isProcessing
    override val isAvailable = true // Always available

    override suspend fun send(userMessage: String): String {
        _isProcessing.value = true
        store.addUserMessage(userMessage)
        return try {
            delay(300) // Simulate thinking
            processMessage(userMessage.lowercase())
        } finally {
            _isProcessing.value = false
        }.also { store.addAssistantMessage(it) }
    }

    private fun processMessage(msg: String): String = when {
        "dim" in msg -> handleDimmer(msg)
        "blue" in msg -> handleSolidColor("blue", 0f, 0f, 1f)
        "red" in msg -> handleSolidColor("red", 1f, 0f, 0f)
        "green" in msg -> handleSolidColor("green", 0f, 1f, 0f)
        "white" in msg -> handleSolidColor("white", 1f, 1f, 1f)
        "warm" in msg || "amber" in msg -> handleSolidColor("warm amber", 1f, 0.6f, 0.1f)
        "preset" in msg || "scene" in msg -> handlePresetQuery(msg)
        "status" in msg || "state" in msg -> handleStatus()
        "effect" in msg -> handleEffectList()
        "help" in msg -> handleHelp()
        "party" in msg || "strobe" in msg -> handleLoadPreset("Strobe Storm")
        "chill" in msg || "ambient" in msg || "relax" in msg -> handleLoadPreset("Sunset Sweep")
        "ocean" in msg || "wave" in msg -> handleLoadPreset("Ocean Waves")
        "rainbow" in msg -> handleLoadPreset("Midnight Rainbow")
        else -> "I'm running in offline mode. Try commands like: 'set it to blue', 'dim to 50%', 'load party preset', 'show status', or 'help' for more options."
    }

    // ... handler methods that call real engineController/presetLibrary methods
}
```

**Step 2: Write tests**

```kotlin
@Test
fun `simulated agent handles color commands`() = runTest {
    val agent = SimulatedLightingAgent(mockEngine, mockState, mockPresets)
    val response = agent.send("make it blue")
    assertTrue(response.contains("blue", ignoreCase = true))
}

@Test
fun `simulated agent handles help command`() = runTest {
    val agent = SimulatedLightingAgent(mockEngine, mockState, mockPresets)
    val response = agent.send("help")
    assertTrue(response.contains("dim", ignoreCase = true))
    assertTrue(response.contains("preset", ignoreCase = true))
}
```

**Step 3: Update MascotViewModelV2 to use interface**

In `MascotViewModelV2.kt` line 40, change `LightingAgent?` to `LightingAgentInterface?`:
```kotlin
private val lightingAgent: LightingAgentInterface? = null,
```

**Step 4: Commit**

```bash
git add -A && git commit -m "feat(agent): SimulatedLightingAgent for offline mode

Keyword-matching agent that executes real tool calls without LLM.
Handles color, dimmer, presets, status, help. Auto-injected via Koin
when no API key is configured."
```

---

## Phase 3: Core Feature UIs

### Task 5: Layer Control Panel

**Files:**
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/components/EffectLayerPanel.kt`
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/stage/StagePreviewScreen.kt`

**Step 1: Create EffectLayerPanel composable**

A collapsible panel showing active effect layers with controls. Each layer row has: enable toggle, effect name, opacity slider, blend mode chip, delete button.

The panel sits between the stage canvas and the preset strip. It collapses to a single "LAYERS (N)" header when closed.

Wire all interactions to existing `StageEvent` variants: `ToggleLayerEnabled`, `SetLayerOpacity`, `SetLayerBlendMode`, `RemoveLayer`, `ReorderLayer`, `AddLayer`, `SetEffect`.

**Step 2: Integrate into StagePreviewScreen**

Add `EffectLayerPanel` between the venue canvas/audience view and the preset strip bar. Pass `performanceState.layers` and the `onEvent` callback.

**Step 3: Test on emulator**

Verify: panel expands/collapses, add layer works, opacity slider changes layer opacity, blend mode toggles between modes, delete removes layer.

**Step 4: Commit**

```bash
git add -A && git commit -m "feat(ui): add EffectLayerPanel to stage screen

Collapsible panel showing active effect layers with enable toggle,
opacity slider, blend mode selector, and delete. Wires into existing
StageEvent layer control events."
```

---

### Task 6: Preset Browser Bottom Sheet

**Files:**
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/components/PresetBrowserSheet.kt`
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/stage/StagePreviewScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/viewmodel/StageViewModelV2.kt`

**Step 1: Create PresetBrowserSheet**

Bottom sheet with grid of preset cards (color swatch thumbnails), genre filter tabs, save current state button, favorite toggle. Uses `PixelBottomSheet` as container.

**Step 2: Add "Save Current" preset flow**

New event `StageEvent.SaveCurrentPreset(name: String, genre: Genre)` → handler in `StageViewModelV2` calls `presetLibrary.savePreset()` with current effect stack state.

**Step 3: Add long-press gesture to preset chips**

In `StagePreviewScreen.kt` preset strip, add `combinedClickable` with `onLongClick` that opens the browser sheet.

**Step 4: Test on emulator, commit**

```bash
git add -A && git commit -m "feat(ui): preset browser bottom sheet with save/delete

Long-press preset chip to browse all presets in grid view. Genre filter
tabs, save current state as new preset, delete user presets. Built-in
presets protected from deletion."
```

---

### Task 7: Fixture Profile Management in Settings

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/settings/SettingsScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/viewmodel/SettingsViewModelV2.kt`

**Step 1: Add FixtureProfilesSection composable**

New collapsible section in SettingsScreen between Simulation and AI Agent sections. Lists profiles with name, channel count, type badge. Add/delete buttons.

**Step 2: Load profiles on init in SettingsViewModelV2**

In the ViewModel init block, populate `fixtureProfiles` from `BuiltInProfiles.all()`.

**Step 3: Wire Add/Delete handlers**

Implement `handleAddFixtureProfile` and `handleDeleteFixtureProfile` event handlers. Add profile dialog with name, type, channel count inputs.

**Step 4: Test on emulator, commit**

```bash
git add -A && git commit -m "feat(ui): fixture profile management in settings

Collapsible profiles section with list of built-in + user profiles.
Add profile dialog for name/type/channels. Delete with confirmation."
```

---

## Phase 4: Audience View Enhancement

### Task 8: Light Beams + Truss in FRONT View

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/components/AudienceView.kt`

The current `AudienceView.kt` already has helper functions for drawing truss, fixtures, beam cones, and floor reflections (lines 200-466). The issue is that these are called but produce minimal visual output because the fixtures have BLACK color.

**Step 1: Enhance beam cone rendering**

In `drawAudienceBeamCone()` (lines 402-438), increase beam width, add gradient alpha from fixture to floor, and make beams more visible even at low opacity.

**Step 2: Enhance floor reflection**

In `drawFloorReflection()` (lines 443-466), increase glow radius and add radial gradient for more visible floor glow.

**Step 3: Improve truss rendering**

In `drawTruss()` (lines 200-218), add mounting bracket shapes at fixture positions and increase truss visual weight.

**Step 4: Add default "idle glow" for dark fixtures**

When a fixture color is BLACK (no effect running), draw a dim grey indicator glow so the FRONT view isn't completely dark.

**Step 5: Test on emulator (FRONT view toggle), commit**

```bash
git add -A && git commit -m "feat(ui): enhanced audience view with beams and truss

Brighter beam cones with gradient alpha, visible floor reflections,
styled truss with mounting brackets. Dark fixtures show dim idle glow."
```

---

## Phase 5: Remaining Features & Polish

### Task 9: Export/Import Data

**Files:**
- Create: `shared/src/commonMain/kotlin/com/chromadmx/core/persistence/DataExportService.kt`
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/viewmodel/SettingsViewModelV2.kt`
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/di/ChromaDiModule.kt`

**Step 1: Create DataExportService**

Serializes fixtures + presets + settings into a single JSON bundle. Uses `kotlinx.serialization`.

```kotlin
@Serializable
data class ExportBundle(
    val version: Int = 1,
    val fixtures: List<Fixture3D>,
    val settings: Map<String, String>,
    val presets: String, // JSON from PresetLibrary.exportPresets()
)
```

**Step 2: Implement export handler in SettingsViewModelV2**

Replace the `/* TODO */` stub at line ~101 with actual export logic. Write JSON to a file, emit the file path in state for the platform to share.

**Step 3: Implement import handler**

Read JSON from a provided file path, validate schema, apply to repositories.

**Step 4: Write tests, commit**

```bash
git add -A && git commit -m "feat: export/import data bundle (JSON)

Serializes fixtures + presets + settings into a single JSON file.
Export triggers platform share sheet. Import validates and applies."
```

---

### Task 10: Network Diagnostics

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/viewmodel/StageViewModelV2.kt`
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/stage/StagePreviewScreen.kt`

**Step 1: Implement DiagnoseNode event handler**

In `StageViewModelV2.onEvent()`, handle the `DiagnoseNode` event. Call the network controller to get diagnostic info (ping, frame count, last error). Update a `diagnosticsResult` field in `NetworkState`.

**Step 2: Add diagnostics overlay in StagePreviewScreen**

When `networkState.diagnosticsResult` is non-null, show a pixel-styled card overlay with the diagnostic details. Tap to dismiss.

**Step 3: Commit**

```bash
git add -A && git commit -m "feat(ui): network node diagnostics overlay

Tap a node in the node list to run diagnostics. Shows ping, frame
count, and error info in a dismissable overlay card."
```

---

### Task 11: Genre Pack Generation

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/viewmodel/SetupViewModel.kt`
- Modify: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/pregen/PreGenerationService.kt`

**Step 1: Wire PreGenerationService into SetupViewModel**

The `PreGenerationService` at lines 56-86 already has a `generate()` method with genre-based templates. Wire it into the SetupViewModel's Vibe Check step completion: when the user selects a genre and advances, call `preGenerationService.generate(genre, count=4)`.

**Step 2: Connect progress flow to UI state**

Map `preGenerationService.progress` StateFlow into `SetupUiState.generationProgress`.

**Step 3: Commit**

```bash
git add -A && git commit -m "feat: genre pack generation in setup flow

Vibe Check genre selection triggers PreGenerationService to create
4 genre-appropriate presets. Progress shown during Stage Preview step."
```

---

### Task 12: Repeat Launch Checks + Mascot Alerts

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/viewmodel/SetupViewModel.kt`
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/viewmodel/MascotViewModelV2.kt`
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/core/persistence/NetworkStateRepository.kt`

**Step 1: Implement topology comparison in SetupViewModel init**

On repeat launch (setupCompleted == true), compare `NetworkStateRepository.lastKnownNodes` with a quick network scan result. If topology changed, set a flag.

**Step 2: Connect to MascotViewModelV2**

When topology change is detected, inject an alert into the mascot's tip queue: "I noticed some fixtures changed since last time. Want to rescan?"

**Step 3: Commit**

```bash
git add -A && git commit -m "feat: repeat launch topology check with mascot alert

Compares persisted node list vs current network on repeat launch.
Mascot alerts user if fixtures added or removed."
```

---

### Task 13: Tech Debt Cleanup

**Files:**
- Modify: `android/app/build.gradle.kts` — remove unused `androidx.navigation.compose`
- Delete: `shared/src/commonMain/kotlin/com/chromadmx/ui/navigation/AppState.kt` (if no remaining references)
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/settings/ProvisioningScreen.kt:146` — replace `Icons.Filled.ArrowBack` with `Icons.AutoMirrored.Filled.ArrowBack`

**Step 1: Remove unused navigation dependency**

In `android/app/build.gradle.kts`, remove the `androidx.navigation.compose` dependency line.

**Step 2: Check and remove legacy AppState**

```bash
grep -r "AppState" --include="*.kt" shared/ android/ | grep -v "AppStateManager" | grep -v "Test"
```

If only `AppStateManager.kt` references it (for the bridge), keep it. If nothing else does, delete it.

**Step 3: Fix deprecated ArrowBack import**

**Step 4: Commit**

```bash
git add -A && git commit -m "chore: remove unused navigation dep, fix deprecated imports"
```

---

## Execution Summary

| Phase | Tasks | PR | Dependencies |
|-------|-------|----|-------------|
| 1: Bugs | Tasks 1-2 | `fix/critical-bugs` | None |
| 2: Agent | Tasks 3-4 | `feat/agent-rewrite` | None |
| 3: Features | Tasks 5-7 | `feat/missing-uis` | Phase 1 |
| 4: Audience | Task 8 | `feat/audience-beams` | Phase 1 |
| 5: Polish | Tasks 9-13 | `feat/polish-sweep` | Phases 1-2 |

**Total: 13 tasks across 5 PRs**

Phases 1 and 2 can run in parallel (independent branches). Phases 3-4 can run in parallel after Phase 1 merges. Phase 5 waits for 1+2.
