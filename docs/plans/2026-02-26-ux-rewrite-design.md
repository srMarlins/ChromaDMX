# ChromaDMX UX Rewrite Design

**Date:** 2026-02-26
**Status:** Approved
**Scope:** Complete UI/ViewModel rewrite using pixel design system, isometric stage preview, robust simulation, offline-first persistence

## Overview

Full rewrite of ChromaDMX's UI layer — screens, ViewModels, navigation, and data flow. The existing code is buggy and inconsistent. This rewrite builds on the pixel design system (25 components, 3 themes, centralized animations) merged in PR #80 and applies mobile best practices: unidirectional data flow, sliced state for compose performance, offline-first persistence, and interface-based simulation that's a drop-in replacement for real hardware.

**Core principles:**
- UDF everywhere — `Event → ViewModel → State → UI`
- Sliced state — each UI region subscribes to only what it needs
- Offline-first — SQLDelight for structured data, app works without network
- Simulation as interface swap — UI never knows if fixtures are real or simulated
- Testable — pure Kotlin ViewModels, fake repositories, Turbine for Flows

## Section 1: Architecture

### UDF Pattern

Every screen follows the same contract:

```kotlin
sealed interface StageEvent {
    data class FixtureSelected(val id: FixtureId) : StageEvent
    data object ToggleEditMode : StageEvent
    data class DimmerChanged(val value: Float) : StageEvent
}

class StageViewModel : ViewModel() {
    val performanceState: StateFlow<PerformanceState>
    val fixtureState: StateFlow<FixtureState>
    val presetState: StateFlow<PresetState>
    val networkState: StateFlow<NetworkState>
    val viewState: StateFlow<ViewState>

    fun onEvent(event: StageEvent)
}
```

One `onEvent()` entry point, multiple state slice outputs. No calling 15 different ViewModel methods from the UI.

### Sliced State

Instead of one monolithic `UiState`, split into focused slices mapping to distinct UI regions:

```kotlin
@Immutable data class PerformanceState(masterDimmer: Float, bpm: Float, beatPhase: Float)
@Immutable data class FixtureState(fixtures: List<FixtureRenderModel>, selectedFixtureId: FixtureId?, isEditMode: Boolean)
@Immutable data class PresetState(presets: List<PresetUiModel>, activePresetId: PresetId?)
@Immutable data class NetworkState(health: NetworkHealth, isSimulation: Boolean, nodeCount: Int)
@Immutable data class ViewState(viewMode: ViewMode, cameraAngle: IsoAngle, zoom: Float)
```

Benefits:
- **Granular recomposition** — changing dimmer doesn't recompose fixture list
- **Simpler tests** — test dimmer logic with just `PerformanceState`
- **Clear ownership** — each slice maps 1:1 to a repository/domain source

High-frequency data (fixture colors at ~30fps, beat phase) flows via separate `SharedFlow` consumed directly by renderers — never baked into recomposable state.

Overlay state (chat open, node list visible, fixture edit sheet) stays as local `remember` state in composables — pure UI state, not business logic.

### Module Boundaries

Existing modules stay: core, networking, tempo, engine, simulation, vision, agent, shared (UI). The UI layer within `shared` gets a clean rewrite. Repositories live in their respective domain modules, ViewModels in the UI layer.

### Key Repositories (offline-first)

- `FixtureRepository` — profiles, positions, groups (SQLDelight, reactive queries)
- `PresetRepository` — scene presets, user saves (SQLDelight)
- `SettingsRepository` — app preferences (multiplatform-settings, FlowSettings)
- `NetworkStateRepository` — discovered nodes, health (in-memory + cached to SQLDelight)

Each repository is the single source of truth. ViewModels observe repository Flows, never cache stale copies.

## Section 2: Navigation & Screen Flow

### Simplified Flow — 3 screens + overlays

```
App Launch
  ├─ First Launch → Setup (discovery + fixture config) → Stage
  └─ Repeat Launch → Stage (background health check)
```

No splash screen (system splash handles branding). No vibe check. No genre selection on first launch. Open the app, find lights (or pick simulation), configure fixtures, perform.

### Navigation State Machine

```kotlin
sealed interface AppScreen {
    data object Setup : AppScreen          // discovery + fixture config
    data object Stage : AppScreen          // main isometric preview
    data object Settings : AppScreen       // overlay from Stage
    data object Provisioning : AppScreen   // BLE config overlay from Settings
}
```

4 states. No deep nesting.

### Overlays (local state within Stage)

- Chat panel — slide up from mascot tap
- Node list — slide in from health indicator tap
- Fixture edit — bottom sheet on fixture tap in edit mode
- Preset browser — bottom sheet on preset strip long-press

### Back Behavior

- Stage → exits app (it's home)
- Settings → back to Stage
- Provisioning → back to Settings
- Setup → exits app (can skip to simulation, can't go backwards mid-setup)

### Repeat Launch

- App opens → check if fixtures persisted → if yes, skip to Stage
- Background coroutine runs network health check
- Topology changes → mascot alerts inline (no interrupting modal)

## Section 3: ViewModel Design

### 4 ViewModels (down from 6)

| ViewModel | Screen | Responsibility |
|-----------|--------|----------------|
| `SetupViewModel` | Setup | Discovery + fixture config flow |
| `StageViewModel` | Stage | All main screen state (sliced) |
| `SettingsViewModel` | Settings | Preferences, profiles, provisioning |
| `MascotViewModel` | Global | Mascot state, chat, alerts, agent |

`OnboardingViewModel` and `AgentViewModel` are gone. Setup replaces onboarding. Agent chat folds into MascotViewModel.

### SetupViewModel

```kotlin
sealed interface SetupStep {
    data object Discovery : SetupStep
    data object FixtureConfig : SetupStep
}

data class SetupUiState(
    val step: SetupStep = SetupStep.Discovery,
    val isScanning: Boolean = false,
    val discoveredNodes: List<NodeUiModel> = emptyList(),
    val isSimulation: Boolean = false,
    val selectedRig: RigPreset? = null,
    val fixtures: List<FixtureConfigModel> = emptyList(),
    val canProceed: Boolean = false,
)

sealed interface SetupEvent {
    data object StartScan : SetupEvent
    data object EnterSimulation : SetupEvent
    data class SelectRig(val preset: RigPreset) : SetupEvent
    data class AssignFixture(val id: FixtureId, val position: Position) : SetupEvent
    data object Proceed : SetupEvent
}
```

Scan starts automatically on enter.

### StageViewModel — Sliced State

```kotlin
class StageViewModel : ViewModel() {
    val performanceState: StateFlow<PerformanceState>
    val fixtureState: StateFlow<FixtureState>
    val presetState: StateFlow<PresetState>
    val networkState: StateFlow<NetworkState>
    val viewState: StateFlow<ViewState>

    // High-frequency flows (not StateFlow — consumed by renderers directly)
    val fixtureColors: SharedFlow<List<FixtureColor>>  // ~30fps from engine
    val beatPhase: SharedFlow<Float>                     // beat-reactive UI

    fun onEvent(event: StageEvent)
}
```

### MascotViewModel

```kotlin
data class MascotUiState(
    val mascotState: MascotAnimState = MascotAnimState.Idle,
    val position: Offset = Offset.Unspecified,
    val bubble: SpeechBubble? = null,
    val isChatOpen: Boolean = false,
    val chatMessages: List<ChatMessage> = emptyList(),
    val isAgentThinking: Boolean = false,
)
```

Chat and agent interaction fold in here. The mascot IS the agent interface.

## Section 4: Isometric Stage Preview

The centerpiece of the app. Two modes, one Canvas-based renderer.

### Isometric View (perform/view mode)

**Grid system:**
- Configurable stage grid (e.g. 16x12 tiles)
- Each tile is an isometric diamond rendered via Canvas
- Floor tiles: subtle grid lines, surfaceVariant at low alpha
- Fixtures sit ON tiles at Z-height (floor, low, truss, high)
- Z-height shifts fixtures UP in isometric projection — trusses visibly float

**Fixture rendering by type:**

| Type | Appearance |
|------|-----------|
| RGB Par | Single pixel block, colored by DMX output, glow halo |
| Moving Head | Pixel block + beam cone (isometric triangle from fixture to floor), direction from pan/tilt |
| Pixel Bar | Row of N pixel blocks (8/16/32 segments), each individually colored |
| Strobe | Flashing white block with burst particle effect |
| Wash | Larger soft glow circle on floor below fixture |

**Beam rendering:**
- Semi-transparent gradient triangles from fixture to floor
- Color = fixture's current RGB output
- Alpha = dimmer value
- Moving head pan/tilt changes cone direction in isometric space
- Overlapping beams: additive color blending on floor

**Camera controls:**
- 4 fixed isometric angles (front-left, front-right, back-left, back-right) — swipe to rotate 90° snaps
- Pinch to zoom, two-finger pan
- No free rotation — fixed angles keep pixel art aesthetic clean

### Top-Down View (edit mode)

Flat 2D grid. Fixtures are draggable colored squares. For layout editing only — position fixtures here, switch to isometric to see the result. Shared composable with Setup screen's fixture config.

### View Switching

Toggle edit mode → crossfade from isometric to top-down. Exit edit → crossfade back. Quick dissolve transition.

### Performance Architecture

```
Engine (40Hz DMX loop)
    → SharedFlow<FixtureColorFrame> at ~30fps
        → IsometricRenderer (Canvas) reads colors directly, no recomposition
```

Renderer splits into layers:
1. **Static layer** (drawWithCache): grid, stage outline, fixture positions — redraws on layout change only
2. **Dynamic layer** (per frame): fixture colors, beam cones, glow — redraws at animation frame rate
3. **UI layer** (compose): selection highlights, edit handles, labels — normal recomposition

## Section 5: Simulation System

### Core Principle

Simulation is not a mode toggle — it's a different implementation of the same interfaces. The UI never knows or cares whether fixtures are real or simulated.

### Interface Abstraction

```kotlin
interface DmxTransport {
    fun sendFrame(universe: Int, channels: ByteArray)
    val connectionState: StateFlow<ConnectionState>
}

interface FixtureDiscovery {
    fun scan(): Flow<DiscoveredNode>
    val discoveredNodes: StateFlow<List<DmxNode>>
}

interface SpatialScanner {
    fun scan(): Flow<ScanResult>
}
```

Real implementations: `ArtNetTransport`, `NetworkDiscovery`, `CameraScanner`
Simulated implementations: `SimulatedTransport`, `SimulatedDiscovery`, `SimulatedScanner`

### Router Pattern (Runtime Switching)

```kotlin
class DmxTransportRouter(
    private val realTransport: ArtNetTransport,
    private val simulatedTransport: SimulatedTransport,
) : DmxTransport {
    private val _activeMode = MutableStateFlow(TransportMode.Real)
    val activeMode: StateFlow<TransportMode> = _activeMode

    fun switchTo(mode: TransportMode) { _activeMode.value = mode }

    override fun sendFrame(universe: Int, channels: ByteArray) {
        when (_activeMode.value) {
            TransportMode.Real -> realTransport.sendFrame(universe, channels)
            TransportMode.Simulated -> simulatedTransport.sendFrame(universe, channels)
            TransportMode.Mixed -> {
                if (universe in simulatedUniverses) simulatedTransport.sendFrame(universe, channels)
                else realTransport.sendFrame(universe, channels)
            }
        }
    }
}

enum class TransportMode { Real, Simulated, Mixed }
```

Both implementations always alive. Router decides who gets frames. Switching is instant — flip `activeMode`, no teardown/rebuild.

Same router pattern for `FixtureDiscoveryRouter` and `SpatialScannerRouter`.

DI registers both implementations + router:
```kotlin
fun dmxModule() = module {
    singleOf(::ArtNetTransport)
    singleOf(::SimulatedTransport)
    singleOf(::DmxTransportRouter) bind DmxTransport::class
}
```

### Realistic Network Simulation

`SimulatedDiscovery` emits nodes with staggered timing (150ms + 80ms per node) — identical to real Art-Net response jitter. The Setup screen can't tell the difference.

`SimulatedNodeHealth` periodically emits health fluctuations:
- 5% chance of signal degradation per check (every 5s)
- Recovery after 3s
- Health indicators pulse, mascot fires alerts

### Network Behavior Profiles

```kotlin
data class RigPreset(
    val name: String,
    val nodes: List<SimNodeConfig>,
    val fixtures: List<SimFixtureConfig>,
    val networkProfile: NetworkProfile = NetworkProfile.Stable,
)

enum class NetworkProfile {
    Stable,         // all nodes stay online
    Flaky,          // occasional signal drops, recovery
    PartialFailure, // one node goes offline after 30s, stays down
    Overloaded,     // high latency, packet loss
}
```

### Runtime Switching Scenarios

| Scenario | Behavior |
|----------|----------|
| User picks simulation at setup | Router starts in Simulated |
| User has real hardware | Router starts in Real |
| Network drops mid-show | Mascot offers fallback → router switches to Simulated |
| User adds simulated fixtures | Router switches to Mixed |
| Network recovers | Mascot offers reconnect → router switches back to Real |

### State Preservation on Switch

- Real → Simulated: SimulatedTransport initializes with current fixture state from FixtureRepository
- Simulated → Real: real transport reconnects, discovery re-scans, fixtures merge with persisted positions

## Section 6: Persistence & Offline-First

### Storage Stack

| Layer | Technology | Data |
|-------|-----------|------|
| Structured | SQLDelight | Fixtures, presets, groups, profiles, known nodes |
| Key-value | multiplatform-settings | Settings, flags, last-used state |
| In-memory | StateFlow | Network state, engine colors, beat |

### SQLDelight Schema

```sql
CREATE TABLE fixture (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    profile_id TEXT NOT NULL REFERENCES fixture_profile(id),
    dmx_universe INTEGER NOT NULL,
    dmx_address INTEGER NOT NULL,
    position_x REAL NOT NULL DEFAULT 0.0,
    position_y REAL NOT NULL DEFAULT 0.0,
    position_z REAL NOT NULL DEFAULT 0.0,
    group_id TEXT REFERENCES fixture_group(id),
    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE fixture_profile (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    manufacturer TEXT NOT NULL DEFAULT '',
    channel_count INTEGER NOT NULL,
    channel_layout TEXT NOT NULL,  -- JSON
    capabilities TEXT NOT NULL DEFAULT '{}',  -- JSON
    render_hint TEXT NOT NULL DEFAULT 'point'
);

CREATE TABLE fixture_group (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    color TEXT NOT NULL DEFAULT '#FFFFFF'
);

CREATE TABLE preset (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    genre TEXT NOT NULL DEFAULT '',
    layer_data TEXT NOT NULL,  -- JSON: serialized effect layers
    is_builtin INTEGER NOT NULL DEFAULT 0,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL
);

CREATE TABLE known_node (
    id TEXT PRIMARY KEY,
    ip_address TEXT NOT NULL,
    name TEXT NOT NULL,
    universes TEXT NOT NULL,  -- JSON array
    last_seen INTEGER NOT NULL
);
```

### Repository Pattern

```kotlin
class FixtureRepository(private val db: ChromaDmxDatabase) {
    val fixtures: Flow<List<Fixture>> =
        db.fixtureQueries.selectAll().asFlow().mapToList(Dispatchers.Default)

    suspend fun upsert(fixture: Fixture) { ... }
    suspend fun updatePosition(id: FixtureId, x: Float, y: Float, z: Float) { ... }
    suspend fun delete(id: FixtureId) { ... }
}
```

ViewModels observe repository Flows. DB changes → UI updates. No manual refresh.

### Offline-First Data Flow

```
App Launch
  ├─ SQLDelight loads persisted fixtures, presets, profiles
  │   → UI renders immediately with cached data
  ├─ Network scan starts in background
  │   → Discovered nodes compared to known_node table
  │   → New/lost nodes trigger mascot alert
  └─ Engine starts with persisted preset
      → DMX output begins immediately
```

### What Persists vs What Doesn't

| Persists (SQLDelight/Settings) | Ephemeral (in-memory) |
|-------------------------------|-----------------------|
| Fixture profiles & positions | Current fixture colors |
| Presets & active preset ID | Beat phase |
| Groups | Network connection state |
| Last known nodes | Engine running state |
| Master dimmer level | Chat conversation |
| Simulation mode flag | Overlay open/close |
| Theme preference | Mascot position |

### Settings (key-value)

```kotlin
class SettingsRepository(private val settings: FlowSettings) {
    val masterDimmer: Flow<Float> = settings.getFloatFlow("master_dimmer", 1f)
    val isSimulation: Flow<Boolean> = settings.getBooleanFlow("simulation", false)
    val activePresetId: Flow<String?> = settings.getStringOrNullFlow("active_preset")
    val themePreference: Flow<PixelColorTheme> = ...
    val transportMode: Flow<TransportMode> = ...
}
```

## Section 7: Screen-by-Screen Design

### Setup Screen

Single screen, two phases — state transition within one composable.

**Phase 1 — Discovery:**
- PixelSectionTitle: "NETWORK DISCOVERY" with blinking cursor
- Nodes appear one by one in PixelCards with pop-in animation
- PixelLoadingSpinner while scanning
- "USE VIRTUAL STAGE" button (PixelButton, Surface variant) → opens PixelBottomSheet with rig preset selector
- "CONTINUE" button (PixelButton, Primary) enabled when nodes found

**Phase 2 — Fixture Config:**
- PixelSectionTitle: "FIXTURE SETUP"
- Top-down grid (shared composable with Stage edit mode)
- Fixture list from discovery — tap to add to grid, drag to position
- "START SHOW" button → persists fixtures, navigates to Stage

### Stage Screen (Main App)

**Top bar** (consumes `performanceState` + `networkState`):
- Node health hearts (tap → node list overlay)
- BPM display with beat phase dots (tap → tap tempo)
- Master dimmer PixelSlider (compact inline)
- Settings gear PixelIconButton

**Isometric renderer** (consumes `fixtureState` + `viewState` + color SharedFlow):
- Canvas-based, layered rendering
- Gesture handling: swipe for camera angle, pinch zoom, two-finger pan
- Tap fixture → select. Long-press → fixture info PixelToast

**Camera controls:**
- Row below renderer: angle label + left/right arrows
- Edit mode toggle PixelChip

**Preset strip** (consumes `presetState`):
- Horizontal LazyRow of PixelCard thumbnails
- Active preset has glowing border
- Tap to apply, long-press for detail PixelBottomSheet

**Edit mode** (toggle, no navigation change):
- Crossfade to top-down grid
- Drag fixtures to reposition
- Selected fixture → PixelBottomSheet with Z-height, group, address, test-fire, delete

### Settings Screen (overlay)

- PixelScaffold + LazyColumn
- Sections: Network, Simulation, Display, Hardware, Agent
- Every row: PixelListItem. Headers: PixelSectionTitle. Dividers: PixelEnchantedDivider
- Dropdowns: PixelDropdown. Toggles: PixelSwitch
- Destructive actions: PixelButton Danger variant + confirmation PixelDialog

### Chat Panel (slide-up overlay)

- PixelBottomSheet expanding to ~75% screen height
- Messages in LazyColumn with PixelCard bubbles
- Tool calls as compact PixelCards with status badge
- Input: PixelTextField + send PixelIconButton
- Managed by MascotViewModel

## Section 8: Testing Strategy

### ViewModel State Transition Tests

```kotlin
class StageViewModelTest {
    @Test
    fun `selecting fixture updates fixtureState`() = runTest {
        val vm = createStageViewModel()
        vm.onEvent(StageEvent.FixtureSelected(FixtureId("par-1")))
        vm.fixtureState.test {
            assertEquals(FixtureId("par-1"), awaitItem().selectedFixtureId)
        }
    }
}
```

**Per ViewModel:**

| ViewModel | Test Focus |
|-----------|-----------|
| SetupViewModel | Discovery → FixtureConfig transitions, simulation fallback, scan retry, fixture persistence |
| StageViewModel | Each state slice independently: performance, fixtures, presets, view mode, edit mode |
| SettingsViewModel | Setting changes persist, theme switching, transport mode |
| MascotViewModel | Chat message flow, alert triggers, bubble lifecycle |

### Navigation Tests

- First launch → Setup
- Repeat launch with fixtures → Stage
- Completing setup → Stage
- Settings back → Stage

### Repository Tests

- Insert/read/update/delete round-trips
- Flow emissions on data changes
- Preset serialization/deserialization
- Settings key-value storage

### Simulation Tests

- SimulatedTransport captures DMX frames correctly
- SimulatedDiscovery emits nodes with realistic timing
- DmxTransportRouter switches modes without data loss
- NetworkProfile.PartialFailure triggers correct health events

### Test Tooling

- `kotlinx-coroutines-test` — runTest, TestDispatcher
- `Turbine` — Flow testing (.test { awaitItem() })
- Fake repositories injected via constructor (no Koin in tests)
- No UI/screenshot tests — PixelShowcaseScreen for visual verification on emulator

### Rig Presets

| Preset | Fixtures | Layout |
|--------|----------|--------|
| DJ Booth | 8 RGB pars | Arc behind DJ, floor level |
| Club Rig | 16 pars + 4 moving heads | 2 trusses + floor |
| Festival | 48 pixel bars + 8 moving heads + 12 strobes | 4 trusses, big grid |
| Custom | User-defined | Manual placement in top-down editor |
