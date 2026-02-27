# ChromaDMX Full Sweep Fix — Design Document

**Date:** 2026-02-27
**Scope:** Critical bugs + all missing feature UIs + agent rewrite + polish

---

## Problem Statement

UI testing on the Android emulator revealed 3 critical bugs that break core functionality, plus 6+ missing feature UIs and an incorrectly architected AI agent. This design covers fixing everything needed for a fully functional app.

---

## Phase 1: Critical Bug Fixes

### 1a. Fixtures Stay Black — SetupViewModel Never Persists Fixtures

**Root cause:** `SetupViewModel` creates a `SimulatedFixtureRig` during onboarding but never calls `fixtureStore.saveAll()`. The `EffectEngine` starts immediately in DI with an empty fixture list. Its `tick()` method early-returns on `curFixtures.isEmpty()`, so no colors are ever computed.

Meanwhile, `StageViewModelV2.repoSyncJob` collects from `FixtureRepository.allFixtures()` and calls `engine.updateFixtures()` — but the DB is empty, so it emits `emptyList()`.

**Fix:**
1. In `SetupViewModel`, when setup completes (transition to COMPLETE phase), persist the simulation rig to the DB:
   ```kotlin
   val rig = SimulatedFixtureRig(selectedPreset)
   fixtureStore?.deleteAll()
   fixtureStore?.saveAll(rig.fixtures)
   ```
2. This must happen BEFORE navigating to Stage, so `repoSyncJob` receives non-empty fixtures.
3. Add a safety net in `StageViewModelV2.handleEnableSimulation()` — if fixtures are empty after enabling simulation, persist the rig there too.
4. Inject `FixtureStore` into `SetupViewModel` via Koin (currently missing).

**Files:** `SetupViewModel.kt`, `UiModule.kt`, `StageViewModelV2.kt`

### 1b. PixelButton `fillMaxWidth()` Breaks Dialog Layouts

**Root cause:** `PixelButton` in `PixelButtons.kt` hardcodes `.fillMaxWidth()` on its face modifier (line ~104). When two PixelButtons are placed in a Row (e.g., PixelDialog's action row), they fight for space and the second button gets clipped off-screen.

**Fix:**
1. Add `modifier: Modifier = Modifier` parameter to `PixelButton` (standard Compose pattern).
2. Remove the hardcoded `.fillMaxWidth()` from the internal face modifier.
3. Apply the caller's modifier to the root container.
4. Update all existing call-sites that need full-width to pass `Modifier.fillMaxWidth()` explicitly.
5. Dialog buttons will now size to content and both will be visible.

**Files:** `PixelButtons.kt`, all call-sites (~20-30 usages across screens)

### 1c. PixelDialog Button Row

After fixing PixelButton, ensure the dialog action Row works:
- Wrap each button in `Modifier.weight(1f)` so they share space evenly.
- Add `Spacer(Modifier.width(12.dp))` between dismiss and confirm buttons.

**Files:** `PixelContainers.kt` (PixelDialog function)

---

## Phase 2: Agent Architecture Rewrite

### Current Problems

1. **Single-instance reuse:** `LightingAgent` creates one `AIAgent` and calls `koogAgent.run()` per user message. Koog agents are single-run — `run()` executes the full ReAct loop and terminates. Calling it again on the same instance is undefined behavior.
2. **No conversation context:** Each `run()` call is stateless — the LLM doesn't see previous messages. The `conversationHistory` StateFlow stores messages locally but never passes them to the LLM.
3. **No offline fallback:** If no API key is configured, the agent returns a static error string. No simulation or offline capability.

### Design: AIAgentService + ReAct + SimulatedAgent

#### 2a. Switch to `AIAgentService` pattern

Replace the persistent `koogAgent` instance with Koog's `AIAgentService`:

```kotlin
class LightingAgentService(
    private val config: AgentConfig,
    private val toolRegistry: ToolRegistry,
    private val conversationStore: ConversationStore,
) {
    private val service = AIAgentService(
        executor = createExecutor(config),
        strategy = reActStrategy(reasoningInterval = 1),
        llmModel = resolveModel(config),
        systemPrompt = AgentSystemPrompt.PROMPT,
        temperature = config.temperature.toDouble(),
        toolRegistry = toolRegistry,
        maxIterations = config.maxIterations,
    )

    suspend fun send(userMessage: String): String {
        conversationStore.addUserMessage(userMessage)
        val contextualPrompt = buildContextualPrompt(userMessage)
        val response = service.createAgentAndRun(contextualPrompt)
        conversationStore.addAssistantMessage(response)
        return response
    }

    private fun buildContextualPrompt(currentMessage: String): String {
        val recentHistory = conversationStore.getRecent(maxMessages = 10)
        return buildString {
            if (recentHistory.isNotEmpty()) {
                appendLine("## Recent conversation context:")
                recentHistory.forEach { msg ->
                    appendLine("${msg.role}: ${msg.content}")
                }
                appendLine()
            }
            appendLine("## Current user request:")
            appendLine(currentMessage)
        }
    }
}
```

Key changes:
- `createAgentAndRun()` creates a FRESH agent per message (correct Koog lifecycle)
- `reActStrategy(reasoningInterval = 1)` preserved — multi-step reasoning for complex tasks
- Conversation context injected into the prompt text (last 10 messages)
- `ConversationStore` manages history persistence and compression

#### 2b. `SimulatedLightingAgent` for Offline/Testing

A keyword-matching agent that works without an API key:

```kotlin
class SimulatedLightingAgent(
    private val engineController: EngineController,
    private val stateController: StateController,
    private val presetLibrary: PresetLibrary,
) : LightingAgentInterface {

    suspend fun send(userMessage: String): String {
        val lower = userMessage.lowercase()
        return when {
            "dim" in lower -> handleDimmer(lower)
            "blue" in lower || "red" in lower || "green" in lower -> handleColor(lower)
            "effect" in lower || "available" in lower -> handleListEffects()
            "preset" in lower || "scene" in lower -> handlePresets(lower)
            "status" in lower || "what" in lower -> handleStatus()
            "help" in lower -> handleHelp()
            else -> handleGeneric(lower)
        }
    }
}
```

- Executes REAL tool calls (actually changes effects, dimmer, etc.)
- Returns descriptive responses explaining what it did
- Injected via Koin when `AgentConfig.isAvailable == false`
- Makes the mascot chat functional on every device, always

#### 2c. Chat Error Handling

- `ChatState` enum: `IDLE`, `THINKING`, `ERROR`, `TIMEOUT`
- 15-second timeout on agent calls with cancellation
- Error state shows retry button + error message in chat bubble
- "No API key" → "I'm running in offline mode! I can still help with basic commands. Configure an API key in Settings for full AI capabilities."

**Files:** `LightingAgent.kt` (rewrite), new `LightingAgentService.kt`, new `SimulatedLightingAgent.kt`, `ConversationStore.kt`, `AgentModule.kt`, `MascotViewModelV2.kt`

---

## Phase 3: Core Feature UIs

### 3a. Layer Control Panel

Expandable panel on the Stage screen showing active effect layers.

**Layout:**
```
┌─────────────────────────────────────────┐
│ ▸ LAYERS (3)                        [+] │
├─────────────────────────────────────────┤
│ ☑ Radial Pulse   ████████░░ 80%  Normal │
│ ☑ Gradient Sweep  ██████░░░░ 60%  Add   │
│ ☐ Strobe          ██████████ 100% Add   │
│                                         │
│ [↑] [↓] [Delete]                        │
└─────────────────────────────────────────┘
```

- Collapsible header: "LAYERS (N)" with add button
- Each row: enable toggle, effect name, opacity slider, blend mode chip
- Selected layer shows up/down/delete controls
- "Add Layer" → dropdown of available effects from `EffectRegistry`
- Wires into existing `StageEvent.AddLayer/RemoveLayer/SetEffect/SetLayerOpacity/SetLayerBlendMode/ToggleLayerEnabled/ReorderLayer`

**New files:** `EffectLayerPanel.kt`
**Modified:** `StagePreviewScreen.kt` (add panel below canvas)

### 3b. Preset Browser Bottom Sheet

**Trigger:** Long-press on any preset chip in the strip, or dedicated browse button.

**Layout:**
```
┌─────────────────────────────────────────┐
│ ─── PRESETS ───────────── [Save Current] │
│                                         │
│ ALL | TECHNO | AMBIENT | HOUSE | CUSTOM │
│                                         │
│ ┌──────────┐ ┌──────────┐ ┌──────────┐ │
│ │ ████████ │ │ ████████ │ │ ████████ │ │
│ │Neon Pulse│ │Sunset Sw.│ │Strobe St.│ │
│ │  ★ TECHNO│ │  ☆ AMBNT │ │  ☆ DNB   │ │
│ └──────────┘ └──────────┘ └──────────┘ │
│                                         │
│ ┌──────────┐ ┌──────────┐ ┌──────────┐ │
│ │ ████████ │ │ ████████ │ │ ████████ │ │
│ │Ocean Wave│ │Fire & Ice│ │Midnight R│ │
│ │  ☆ AMBNT │ │  ☆ HOUSE │ │  ☆ CSTM  │ │
│ └──────────┘ └──────────┘ └──────────┘ │
└─────────────────────────────────────────┘
```

- Grid of preset cards with thumbnail color swatches
- Genre filter tabs at top
- Favorite toggle (star)
- "Save Current" button → name input dialog → `PresetLibrary.savePreset()`
- Tap to apply, long-press to delete (user presets only)
- Built-in presets show lock icon, can't be deleted

**New files:** `PresetBrowserSheet.kt`
**Modified:** `StagePreviewScreen.kt` (add long-press gesture + browse trigger)

### 3c. Fixture Profile Management in Settings

New collapsible section between Simulation and AI Agent.

**Layout:**
```
▸ FIXTURE PROFILES █
┌─────────────────────────────────────────┐
│ RGB Par (3ch)                    [Edit] │
│ Moving Head (16ch)               [Edit] │
│ Pixel Bar (48ch)                 [Edit] │
│                                         │
│         [Add Profile]                   │
└─────────────────────────────────────────┘
```

- List of profiles from `BuiltInProfiles` + user-created
- Each row: name, channel count badge, edit button
- "Add Profile" → dialog with name, type dropdown, channel count, channel mapping fields
- Edit opens same dialog pre-populated
- Delete with confirmation (built-ins protected)
- Wires into `SettingsEvent.AddFixtureProfile/DeleteFixtureProfile`

**Modified:** `SettingsScreen.kt` (add section), `SettingsViewModelV2.kt` (load profiles on init)

---

## Phase 4: Audience View Enhancement

### 4a. Light Beams + Truss

Replace the current minimal FRONT view with a proper stage visualization.

**Rendering layers (bottom to top):**
1. **Floor** — dark gradient with slight horizon line
2. **Floor glow** — elliptical gradients where beams hit, colored by fixture
3. **Beams** — triangular cones from each fixture downward, gradient alpha (1.0→0.0), colored by fixture DMX output
4. **Truss** — styled horizontal bar with rectangular mounting brackets at fixture positions
5. **Fixtures** — colored squares/circles on the truss, matching current DMX color

**Beam rendering:**
```kotlin
// For each fixture:
val beamPath = Path().apply {
    moveTo(fixtureX - beamTopWidth/2, fixtureY)     // top-left of fixture
    lineTo(fixtureX + beamTopWidth/2, fixtureY)     // top-right
    lineTo(fixtureX + beamBottomWidth/2, floorY)    // bottom-right (wider)
    lineTo(fixtureX - beamBottomWidth/2, floorY)    // bottom-left
    close()
}
drawPath(beamPath, Brush.verticalGradient(
    colors = listOf(fixtureColor.copy(alpha = 0.6f), Color.Transparent),
    startY = fixtureY, endY = floorY
))
```

**Files:** `AudienceView.kt` (rewrite Canvas drawing)

---

## Phase 5: Remaining Features & Polish

### 5a. Export/Import

- **Format:** Single JSON file containing `{ fixtures: [...], presets: [...], settings: {...} }`
- **Export:** Serialize all data → write to app cache dir → trigger platform share sheet (Android `Intent.ACTION_SEND`)
- **Import:** File picker → read JSON → validate schema → apply to repositories
- Add `expect/actual` for platform file operations

**Files:** `SettingsViewModelV2.kt` (implement handlers), new `DataExportService.kt`

### 5b. Network Diagnostics

- Implement `StageEvent.DiagnoseNode` handler in `StageViewModelV2`
- Call `NetworkController.diagnoseConnection()` (already exists as a tool)
- Show results in a diagnostics overlay: ping time, frame count, last error, firmware version
- Add health color indicators (green <50ms, yellow <200ms, red >200ms) to node list

**Files:** `StageViewModelV2.kt`, `StagePreviewScreen.kt` (diagnostics overlay)

### 5c. Repeat Launch Checks

- On app start, if `setupCompleted == true`, compare persisted node list vs. current network
- If topology changed (nodes added/removed), trigger mascot alert
- "Hey! I noticed some fixtures changed since last time. Want to rescan?"
- User can dismiss or trigger rescan from the alert

**Files:** `SetupViewModel.kt`, `MascotViewModelV2.kt`, `NetworkStateRepository.kt`

### 5d. Genre Pack Generation

- Connect "Vibe Check" genre selection to `PresetLibrary`
- Use `SimulatedLightingAgent` to generate 4-6 presets based on genre keywords
- Map genres to effect palettes: Techno → strobes + pulse, Ambient → sweeps + noise, etc.
- Progress bar during generation, error handling

**Files:** `SetupViewModel.kt`, `PreGenerationService.kt`

### 5e. Startup Optimization

- Profile the 58-frame skip (logcat "Skipped 58 frames")
- Likely cause: synchronous DI initialization + DB setup on main thread
- Fix: Move DB driver creation to background, use `lazyInject` for non-critical singletons
- Defer `EffectEngine.start()` until after first frame

**Files:** `ChromaDiModule.kt`, `ChromaDMXApp.kt`

### 5f. Tech Debt Cleanup

- Remove unused `androidx.navigation.compose` dependency from `android/app/build.gradle.kts`
- Delete legacy `AppState.kt` if no longer referenced
- Remove deprecated `ArrowBack` usage (use `AutoMirrored` version)

---

## Execution Order

Phases are ordered by dependency — later phases depend on earlier ones:

1. **Phase 1** (bugs) — unblocks the entire app; must be first
2. **Phase 2** (agent) — independent of Phase 1 but foundational for Phases 5c/5d
3. **Phase 3** (feature UIs) — can run in parallel with Phase 2
4. **Phase 4** (audience view) — independent, can run anytime after Phase 1
5. **Phase 5** (polish) — depends on Phases 1-2 being complete

Within phases, items are independent and can be parallelized via worktrees/subagents.

---

## Testing Strategy

- Each phase gets its own PR with unit tests for new/changed logic
- ViewModel tests for all new event handlers
- Emulator smoke test after each phase merge
- Agent tests use `SimulatedLightingAgent` (no API key needed)
- Canvas rendering tested visually on emulator (screenshot comparison)
