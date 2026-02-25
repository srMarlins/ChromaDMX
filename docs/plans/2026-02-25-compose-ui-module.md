# Compose UI Module Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build all four main screens (Perform, Network, Map, Agent) with Compose Multiplatform, plus shared theme/design tokens, Canvas venue visualization, and ViewModels backed by real shared module data.

**Architecture:** Compose UI code lives in the `shared/` umbrella module which already has `chromadmx.compose` plugin applied. ViewModels use Koin DI to inject shared services (EffectEngine, NodeDiscovery, BeatClock, LightingAgent). Unidirectional data flow: UI observes `StateFlow`, user actions call ViewModel methods.

**Tech Stack:** Compose Multiplatform (Material 3), Koin Compose, kotlinx.coroutines, Compose Canvas for venue visualization

---

## Reference Files

- **Shared umbrella (already has Compose):** `shared/build.gradle.kts`
- **Compose convention plugin:** `build-logic/convention/src/main/kotlin/ComposeMultiplatformConventionPlugin.kt`
- **Android app:** `android/app/build.gradle.kts`
- **Core models:** `shared/core/src/commonMain/kotlin/com/chromadmx/core/model/` (Color, Vec3, BeatState, Fixture, Fixture3D, BlendMode)
- **EffectParams:** `shared/core/src/commonMain/kotlin/com/chromadmx/core/EffectParams.kt`
- **Effect engine:** `shared/engine/src/commonMain/kotlin/com/chromadmx/engine/pipeline/EffectEngine.kt`
- **Effect stack:** `shared/engine/src/commonMain/kotlin/com/chromadmx/engine/effect/EffectStack.kt`
- **Effect registry:** `shared/engine/src/commonMain/kotlin/com/chromadmx/engine/effect/EffectRegistry.kt`
- **Node discovery:** `shared/networking/src/commonMain/kotlin/com/chromadmx/networking/discovery/NodeDiscovery.kt`
- **DmxNode:** `shared/networking/src/commonMain/kotlin/com/chromadmx/networking/model/DmxNode.kt`
- **DMX output:** `shared/networking/src/commonMain/kotlin/com/chromadmx/networking/output/DmxOutputService.kt`
- **BeatClock:** `shared/tempo/src/commonMain/kotlin/com/chromadmx/tempo/clock/BeatClock.kt`
- **TapTempoClock:** `shared/tempo/src/commonMain/kotlin/com/chromadmx/tempo/tap/TapTempoClock.kt`
- **Version catalog:** `gradle/libs.versions.toml`
- **Architecture design:** `docs/plans/2026-02-24-chromadmx-architecture-design.md`

---

### Task 1: Theme & Design Tokens

**Files:**
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/theme/ChromaDmxTheme.kt`
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/theme/Color.kt`
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/theme/Typography.kt`

**Step 1: Create color tokens**

`Color.kt`:
```kotlin
package com.chromadmx.ui.theme

import androidx.compose.ui.graphics.Color

// Dark theme — optimized for dark stage environments
val DmxPrimary = Color(0xFF6C63FF)       // Electric purple
val DmxOnPrimary = Color(0xFFFFFFFF)
val DmxPrimaryContainer = Color(0xFF352F7E)
val DmxSecondary = Color(0xFF00E5FF)      // Cyan accent
val DmxOnSecondary = Color(0xFF000000)
val DmxBackground = Color(0xFF0D0D0D)     // Near-black
val DmxSurface = Color(0xFF1A1A2E)        // Dark navy
val DmxSurfaceVariant = Color(0xFF252540)
val DmxOnBackground = Color(0xFFE0E0E0)
val DmxOnSurface = Color(0xFFE0E0E0)
val DmxError = Color(0xFFFF5252)
val DmxOnError = Color(0xFFFFFFFF)

// Status colors for node health
val NodeOnline = Color(0xFF4CAF50)
val NodeWarning = Color(0xFFFFC107)
val NodeOffline = Color(0xFFFF5252)
val NodeUnknown = Color(0xFF9E9E9E)

// Beat visualization colors
val BeatActive = Color(0xFF6C63FF)
val BeatInactive = Color(0xFF2A2A3E)
```

**Step 2: Create typography**

`Typography.kt`:
```kotlin
package com.chromadmx.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val DmxTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.5.sp),
)
```

**Step 3: Create ChromaDmxTheme composable**

`ChromaDmxTheme.kt`:
```kotlin
package com.chromadmx.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DmxDarkColorScheme = darkColorScheme(
    primary = DmxPrimary,
    onPrimary = DmxOnPrimary,
    primaryContainer = DmxPrimaryContainer,
    secondary = DmxSecondary,
    onSecondary = DmxOnSecondary,
    background = DmxBackground,
    surface = DmxSurface,
    surfaceVariant = DmxSurfaceVariant,
    onBackground = DmxOnBackground,
    onSurface = DmxOnSurface,
    error = DmxError,
    onError = DmxOnError,
)

@Composable
fun ChromaDmxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DmxDarkColorScheme,
        typography = DmxTypography,
        content = content,
    )
}
```

**Step 4: Verify build compiles**

Run: `./gradlew :shared:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/chromadmx/ui/
git commit -m "Add ChromaDMX dark theme with design tokens (#12)"
```

---

### Task 2: Navigation Shell

**Files:**
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/navigation/ChromaDmxNavHost.kt`
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/navigation/Screen.kt`
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/ChromaDmxApp.kt`
- Modify: `shared/build.gradle.kts` — add navigation dependency if needed

**Step 1: Check if navigation-compose is available in commonMain**

Compose Multiplatform navigation: use `org.jetbrains.androidx.navigation:navigation-compose` (the KMP version). Add to version catalog if needed:

```toml
# In libs.versions.toml
compose-navigation = "2.8.0-alpha10"

# Libraries
compose-navigation = { module = "org.jetbrains.androidx.navigation:navigation-compose", version.ref = "compose-navigation" }
```

Add to `shared/build.gradle.kts` commonMain deps:
```kotlin
implementation(libs.compose.navigation)
```

**Step 2: Define Screen enum**

```kotlin
package com.chromadmx.ui.navigation

enum class Screen(val route: String, val title: String, val icon: String) {
    PERFORM("perform", "Perform", "play_circle"),
    NETWORK("network", "Network", "wifi"),
    MAP("map", "Map", "map"),
    AGENT("agent", "Agent", "smart_toy"),
}
```

**Step 3: Create ChromaDmxApp root composable with bottom navigation**

```kotlin
package com.chromadmx.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.chromadmx.ui.navigation.Screen
import com.chromadmx.ui.theme.ChromaDmxTheme

@Composable
fun ChromaDmxApp() {
    ChromaDmxTheme {
        var currentScreen by remember { mutableStateOf(Screen.PERFORM) }

        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    Screen.entries.forEach { screen ->
                        NavigationBarItem(
                            selected = currentScreen == screen,
                            onClick = { currentScreen = screen },
                            icon = { Icon(screenIcon(screen), contentDescription = screen.title) },
                            label = { Text(screen.title) },
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (currentScreen) {
                    Screen.PERFORM -> PerformScreenPlaceholder()
                    Screen.NETWORK -> NetworkScreenPlaceholder()
                    Screen.MAP -> MapScreenPlaceholder()
                    Screen.AGENT -> AgentScreenPlaceholder()
                }
            }
        }
    }
}
```

Use placeholder composables initially (`Text("Perform Screen")`, etc.) to verify navigation works.

**Step 4: Wire into Android app**

Update `android/app/src/main/kotlin/.../MainActivity.kt` to call `ChromaDmxApp()`.

**Step 5: Verify build**

Run: `./gradlew :android:app:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git commit -m "Add navigation shell with 4 bottom tabs (#12)"
```

---

### Task 3: ViewModels

**Files:**
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/viewmodel/PerformViewModel.kt`
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/viewmodel/NetworkViewModel.kt`
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/viewmodel/MapViewModel.kt`
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/viewmodel/AgentViewModel.kt`

**Step 1: Add lifecycle-viewmodel dependency for KMP**

In `shared/build.gradle.kts`, add:
```kotlin
implementation(libs.androidx.lifecycle.viewmodel)
implementation(libs.androidx.lifecycle.runtime.compose)
```

NOTE: Check if KMP-compatible lifecycle viewmodel is available via Compose Multiplatform. If not, use a simple class with CoroutineScope instead.

**Step 2: Create PerformViewModel**

```kotlin
package com.chromadmx.ui.viewmodel

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.Color
import com.chromadmx.engine.effect.EffectRegistry
import com.chromadmx.engine.effect.EffectStack
import com.chromadmx.engine.pipeline.EffectEngine
import com.chromadmx.tempo.clock.BeatClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class PerformViewModel(
    private val engine: EffectEngine,
    private val effectStack: EffectStack,
    private val effectRegistry: EffectRegistry,
    private val beatClock: BeatClock,
    private val scope: CoroutineScope,
) {
    val beatState: StateFlow<BeatState> = beatClock.beatState
    val masterDimmer: StateFlow<Float> get() = effectStack.masterDimmerFlow
    val isRunning: StateFlow<Boolean> = beatClock.isRunning
    val bpm: StateFlow<Float> = beatClock.bpm

    fun availableEffects(): List<String> = effectRegistry.ids()

    fun setEffect(layer: Int, effectId: String, params: EffectParams = EffectParams.EMPTY) {
        val effect = effectRegistry.get(effectId) ?: return
        effectStack.setEffect(layer, effect, params)
    }

    fun setMasterDimmer(value: Float) {
        effectStack.setMasterDimmer(value.coerceIn(0f, 1f))
    }

    fun setLayerOpacity(layer: Int, opacity: Float) {
        effectStack.setOpacity(layer, opacity.coerceIn(0f, 1f))
    }

    fun tap() {
        // Forward to TapTempoClock if available
    }
}
```

**Step 3: Create NetworkViewModel**

```kotlin
class NetworkViewModel(
    private val nodeDiscovery: NodeDiscovery,
    private val scope: CoroutineScope,
) {
    val nodes: StateFlow<Map<String, DmxNode>> = nodeDiscovery.nodes

    fun startScan() { /* trigger ArtPoll */ }
    fun stopScan() { /* stop polling */ }
}
```

**Step 4: Create MapViewModel and AgentViewModel similarly**

AgentViewModel wraps LightingAgent — delegates send(), observes conversationHistory and isProcessing.

**Step 5: Verify build, commit**

```bash
git commit -m "Add ViewModels for all 4 screens with DI (#12)"
```

---

### Task 4: Perform Screen

**Files:**
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/perform/PerformScreen.kt`
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/perform/BeatVisualization.kt`
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/perform/EffectLayerCard.kt`
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/perform/MasterDimmerSlider.kt`
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/perform/ScenePresetRow.kt`

**Step 1: Build BeatVisualization composable**

A row of 4 beat indicators that pulse with beatPhase/barPhase. Uses Canvas to draw filled circles that animate opacity on the beat.

```kotlin
@Composable
fun BeatVisualization(beatState: BeatState, modifier: Modifier = Modifier) {
    // 4 circles representing beats in a bar
    // Current beat is highlighted based on barPhase
    // Beat pulse animation driven by beatPhase
}
```

**Step 2: Build MasterDimmerSlider**

Vertical slider (0-100%) that controls master output level.

**Step 3: Build EffectLayerCard**

Card showing: effect selector dropdown, opacity slider, blend mode selector, enabled toggle.

**Step 4: Build ScenePresetRow**

Horizontal row of preset buttons. Tap to load scene, long-press to save current state.

**Step 5: Compose PerformScreen from components**

Layout: BPM display + BeatVisualization at top, effect layer cards in scrollable column, master dimmer on right edge, scene presets at bottom.

**Step 6: Verify build, commit**

```bash
git commit -m "Add Perform screen with beat visualization and effect controls (#12)"
```

---

### Task 5: Network Screen

**Files:**
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/network/NetworkScreen.kt`
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/network/NodeCard.kt`
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/network/NodeHealthIndicator.kt`

**Step 1: Build NodeHealthIndicator**

Small colored dot: green = online (seen < 5s ago), yellow = warning (5-15s), red = offline (>15s), gray = unknown.

**Step 2: Build NodeCard**

Card showing: node name, IP address, universes, firmware version, health indicator, last seen timestamp.

**Step 3: Build NetworkScreen**

Pull-to-refresh triggers scan. LazyColumn of NodeCards. "Scanning..." indicator at top when discovery is active. Empty state message when no nodes found.

**Step 4: Verify build, commit**

```bash
git commit -m "Add Network screen with node discovery and health indicators (#12)"
```

---

### Task 6: Map Screen

**Files:**
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/map/MapScreen.kt`
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/map/FixturePositionEditor.kt`
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/map/FixtureListPanel.kt`

**Step 1: Build FixtureListPanel**

List of all mapped fixtures with name, position (x,y,z), group, and a "test fire" button.

**Step 2: Build FixturePositionEditor**

Canvas-based 2D top-down view showing fixture positions as dots. Drag to reposition. Tap to select and show Z-height slider.

NOTE: Camera preview (for spatial scanning) is platform-specific and will be a platform stub for now — just show a "Camera not available" placeholder in shared code, actual camera integration done in android/ios source sets later.

**Step 3: Build MapScreen**

Split layout: fixture list on left (or bottom on mobile), canvas editor on right (or top on mobile).

**Step 4: Verify build, commit**

```bash
git commit -m "Add Map screen with fixture position editor (#12)"
```

---

### Task 7: Agent Screen

**Files:**
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/agent/AgentScreen.kt`
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/agent/ChatBubble.kt`
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/agent/ToolCallCard.kt`
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/agent/PreGenPanel.kt`

**Step 1: Build ChatBubble**

User messages right-aligned (primary color), assistant messages left-aligned (surface color). Show tool call badges inline.

**Step 2: Build ToolCallCard**

Small card showing tool name, parameters, and result. Collapsed by default, expandable.

**Step 3: Build PreGenPanel**

"Generate Scenes" UI: genre text input, count selector (5/10/20), progress bar, generate button. Shows when agent is available.

**Step 4: Build AgentScreen**

LazyColumn chat history, text input with send button at bottom, "Agent unavailable" banner when no API key. Pre-gen mode accessible via FAB or menu.

**Step 5: Verify build, commit**

```bash
git commit -m "Add Agent screen with chat interface and pre-generation UI (#12)"
```

---

### Task 8: Canvas Venue Visualization

**Files:**
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/components/VenueCanvas.kt`
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/perform/PerformScreen.kt` — integrate VenueCanvas

**Step 1: Build VenueCanvas composable**

Uses Compose Canvas to render a top-down venue view:
- Each fixture rendered as a colored circle at its (x, y) position
- Color comes from engine's latest computed fixture colors
- Background is dark
- Scale/pan support via transformable modifier
- Should update at display refresh rate (observes engine color output StateFlow)

```kotlin
@Composable
fun VenueCanvas(
    fixtures: List<Fixture3D>,
    fixtureColors: List<Color>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        // Draw dark background
        // For each fixture, draw a circle at (x, y) scaled to canvas
        // Fill circle with the fixture's current color
        // Draw fixture name label below
    }
}
```

**Step 2: Integrate into PerformScreen**

Add VenueCanvas as the background/main area of the Perform screen, with effect controls overlaid.

**Step 3: Verify build, commit**

```bash
git commit -m "Add Canvas venue visualization with live fixture colors (#12)"
```

---

### Task 9: Koin UI Module & App Wiring

**Files:**
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/di/UiModule.kt`
- Modify: `android/app/src/main/kotlin/.../MainApplication.kt` or wherever Koin is started — add uiModule
- Modify: `shared/build.gradle.kts` — add remaining module dependencies

**Step 1: Add all shared module dependencies to umbrella**

In `shared/build.gradle.kts`, ensure commonMain has:
```kotlin
api(project(":shared:core"))
api(project(":shared:engine"))
api(project(":shared:networking"))
api(project(":shared:tempo"))
api(project(":shared:simulation"))
api(project(":shared:agent"))
```

**Step 2: Create Koin UI module**

```kotlin
package com.chromadmx.ui.di

import com.chromadmx.ui.viewmodel.*
import org.koin.dsl.module

val uiModule = module {
    factory { params -> PerformViewModel(get(), get(), get(), get(), params.get()) }
    factory { params -> NetworkViewModel(get(), params.get()) }
    factory { params -> MapViewModel(get(), params.get()) }
    factory { params -> AgentViewModel(get(), params.get()) }
}
```

**Step 3: Wire into Android app Koin startup**

Ensure the Android Application class starts Koin with all modules:
```kotlin
startKoin {
    androidContext(this@MainApplication)
    modules(coreModule, networkingModule, tempoModule, engineModule, agentModule, uiModule)
}
```

**Step 4: Verify Android build**

Run: `./gradlew :android:app:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git commit -m "Wire Koin UI module and app integration (#12)"
```

---

### Task 10: Replace Placeholders with Real Screens

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/ChromaDmxApp.kt` — replace placeholders with real screens

**Step 1: Update ChromaDmxApp to use real screen composables**

Each screen gets its ViewModel via Koin:
```kotlin
Screen.PERFORM -> PerformScreen(viewModel = koinViewModel())
Screen.NETWORK -> NetworkScreen(viewModel = koinViewModel())
Screen.MAP -> MapScreen(viewModel = koinViewModel())
Screen.AGENT -> AgentScreen(viewModel = koinViewModel())
```

**Step 2: Verify full build**

Run: `./gradlew :android:app:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 3: Final commit**

```bash
git commit -m "Wire real screens into navigation, completing Compose UI (#12)"
```
