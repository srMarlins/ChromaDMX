# Screen Architecture Overhaul (#20) Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the 4-tab bottom navigation (Perform, Network, Map, Agent) with a single main screen (Stage Preview) plus contextual overlays (Settings, Chat Panel) and an onboarding flow.

**Architecture:** Create an `AppState` sealed class for navigation, new combined `StageViewModel`, and rewrite `ChromaDmxApp.kt` to use state-based routing instead of enum tabs. Settings is a gear-icon overlay, Chat Panel is a bottom sheet (wired in #22), mascot overlay is always visible (wired in #18). Old screens/ViewModels become internal to the new StagePreview or are removed.

**Tech Stack:** Compose Multiplatform, Koin DI, kotlinx-coroutines, StateFlow

---

## Context Files

Before starting, read these files to understand the current state:

- `shared/src/commonMain/kotlin/com/chromadmx/ui/ChromaDmxApp.kt` — current root composable with tab nav
- `shared/src/commonMain/kotlin/com/chromadmx/ui/navigation/Screen.kt` — current Screen enum
- `shared/src/commonMain/kotlin/com/chromadmx/ui/viewmodel/PerformViewModel.kt` — will merge into StageViewModel
- `shared/src/commonMain/kotlin/com/chromadmx/ui/viewmodel/MapViewModel.kt` — will merge into StageViewModel
- `shared/src/commonMain/kotlin/com/chromadmx/ui/viewmodel/NetworkViewModel.kt` — will be used internally
- `shared/src/commonMain/kotlin/com/chromadmx/ui/viewmodel/AgentViewModel.kt` — stays, powers chat panel
- `shared/src/commonMain/kotlin/com/chromadmx/ui/di/UiModule.kt` — Koin wiring
- `shared/src/commonMain/kotlin/com/chromadmx/di/ChromaDiModule.kt` — central DI

---

### Task 1: Create AppState Navigation Model

**Files:**
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/navigation/AppState.kt`
- Test: `shared/src/commonTest/kotlin/com/chromadmx/ui/navigation/AppStateTest.kt`

**Step 1: Write the failing test**

```kotlin
package com.chromadmx.ui.navigation

import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppStateTest {
    @Test
    fun onboardingIsInitialStateForFirstLaunch() {
        val state: AppState = AppState.Onboarding(OnboardingStep.SPLASH)
        assertIs<AppState.Onboarding>(state)
    }

    @Test
    fun stagePreviewIsDefaultState() {
        val state: AppState = AppState.StagePreview
        assertIs<AppState.StagePreview>(state)
    }

    @Test
    fun settingsIsOverlayState() {
        val state: AppState = AppState.Settings
        assertIs<AppState.Settings>(state)
    }

    @Test
    fun onboardingStepsAreOrdered() {
        val steps = OnboardingStep.entries
        assertTrue(steps.indexOf(OnboardingStep.SPLASH) < steps.indexOf(OnboardingStep.NETWORK_DISCOVERY))
        assertTrue(steps.indexOf(OnboardingStep.NETWORK_DISCOVERY) < steps.indexOf(OnboardingStep.FIXTURE_SCAN))
        assertTrue(steps.indexOf(OnboardingStep.FIXTURE_SCAN) < steps.indexOf(OnboardingStep.VIBE_CHECK))
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.chromadmx.ui.navigation.AppStateTest"`

Note: The umbrella `:shared` module has tests at `shared/src/commonTest/`. If no test source set exists, create the directory.

**Step 3: Implement AppState**

Create `shared/src/commonMain/kotlin/com/chromadmx/ui/navigation/AppState.kt`:

```kotlin
package com.chromadmx.ui.navigation

/**
 * Top-level app navigation state.
 *
 * Replaces the old [Screen] enum. The app is either:
 * - In the onboarding flow (first launch)
 * - On the main stage preview screen
 * - Viewing settings (overlay)
 *
 * Chat panel and mascot are overlays managed independently.
 */
sealed class AppState {
    data class Onboarding(val step: OnboardingStep) : AppState()
    data object StagePreview : AppState()
    data object Settings : AppState()
}

/**
 * Steps in the first-launch onboarding flow.
 */
enum class OnboardingStep {
    SPLASH,
    NETWORK_DISCOVERY,
    FIXTURE_SCAN,
    VIBE_CHECK
}
```

**Step 4: Run test**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.chromadmx.ui.navigation.AppStateTest"`
Expected: PASS

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/chromadmx/ui/navigation/AppState.kt
git add shared/src/commonTest/kotlin/com/chromadmx/ui/navigation/AppStateTest.kt
git commit -m "feat(ui): add AppState sealed class for new navigation model (#20)"
```

---

### Task 2: Create AppStateManager

**Files:**
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/navigation/AppStateManager.kt`
- Test: `shared/src/commonTest/kotlin/com/chromadmx/ui/navigation/AppStateManagerTest.kt`

**Step 1: Write the failing test**

```kotlin
package com.chromadmx.ui.navigation

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AppStateManagerTest {
    @Test
    fun firstLaunchStartsWithOnboarding() = runTest {
        val manager = AppStateManager(isFirstLaunch = true)
        val state = manager.currentState.first()
        assertIs<AppState.Onboarding>(state)
        assertEquals(OnboardingStep.SPLASH, (state as AppState.Onboarding).step)
    }

    @Test
    fun repeatLaunchStartsWithStagePreview() = runTest {
        val manager = AppStateManager(isFirstLaunch = false)
        val state = manager.currentState.first()
        assertIs<AppState.StagePreview>(state)
    }

    @Test
    fun navigateToSettings() = runTest {
        val manager = AppStateManager(isFirstLaunch = false)
        manager.navigateTo(AppState.Settings)
        assertEquals(AppState.Settings, manager.currentState.first())
    }

    @Test
    fun navigateBackFromSettings() = runTest {
        val manager = AppStateManager(isFirstLaunch = false)
        manager.navigateTo(AppState.Settings)
        manager.navigateBack()
        assertIs<AppState.StagePreview>(manager.currentState.first())
    }

    @Test
    fun advanceOnboardingStep() = runTest {
        val manager = AppStateManager(isFirstLaunch = true)
        manager.advanceOnboarding()
        val state = manager.currentState.first()
        assertIs<AppState.Onboarding>(state)
        assertEquals(OnboardingStep.NETWORK_DISCOVERY, state.step)
    }

    @Test
    fun completeOnboardingGoesToStagePreview() = runTest {
        val manager = AppStateManager(isFirstLaunch = true)
        // Advance through all steps
        manager.advanceOnboarding() // SPLASH -> NETWORK_DISCOVERY
        manager.advanceOnboarding() // -> FIXTURE_SCAN
        manager.advanceOnboarding() // -> VIBE_CHECK
        manager.advanceOnboarding() // -> StagePreview
        assertIs<AppState.StagePreview>(manager.currentState.first())
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.chromadmx.ui.navigation.AppStateManagerTest"`

**Step 3: Implement AppStateManager**

Create `shared/src/commonMain/kotlin/com/chromadmx/ui/navigation/AppStateManager.kt`:

```kotlin
package com.chromadmx.ui.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages top-level app navigation state.
 *
 * @param isFirstLaunch True if this is the first app launch (show onboarding).
 */
class AppStateManager(isFirstLaunch: Boolean) {

    private val _currentState = MutableStateFlow<AppState>(
        if (isFirstLaunch) AppState.Onboarding(OnboardingStep.SPLASH)
        else AppState.StagePreview
    )
    val currentState: StateFlow<AppState> = _currentState.asStateFlow()

    private var previousState: AppState = AppState.StagePreview

    fun navigateTo(state: AppState) {
        previousState = _currentState.value
        _currentState.value = state
    }

    fun navigateBack() {
        _currentState.value = previousState
        previousState = AppState.StagePreview
    }

    /**
     * Advance to the next onboarding step.
     * If already at the last step, transitions to StagePreview.
     */
    fun advanceOnboarding() {
        val current = _currentState.value
        if (current !is AppState.Onboarding) return

        val steps = OnboardingStep.entries
        val currentIndex = steps.indexOf(current.step)
        if (currentIndex < steps.lastIndex) {
            _currentState.value = AppState.Onboarding(steps[currentIndex + 1])
        } else {
            _currentState.value = AppState.StagePreview
        }
    }
}
```

**Step 4: Run test**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.chromadmx.ui.navigation.AppStateManagerTest"`
Expected: PASS

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/chromadmx/ui/navigation/AppStateManager.kt
git add shared/src/commonTest/kotlin/com/chromadmx/ui/navigation/AppStateManagerTest.kt
git commit -m "feat(ui): add AppStateManager for navigation state management (#20)"
```

---

### Task 3: Create StageViewModel

**Files:**
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/viewmodel/StageViewModel.kt`
- Test: `shared/src/commonTest/kotlin/com/chromadmx/ui/viewmodel/StageViewModelTest.kt`

**Step 1: Write the failing test**

```kotlin
package com.chromadmx.ui.viewmodel

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Fixture
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3
import com.chromadmx.engine.effect.EffectLayer
import com.chromadmx.engine.effect.EffectRegistry
import com.chromadmx.engine.effect.EffectStack
import com.chromadmx.engine.effects.SolidColorEffect
import com.chromadmx.engine.pipeline.EffectEngine
import com.chromadmx.tempo.clock.BeatClock
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StageViewModelTest {
    private fun makeFixtures(count: Int): List<Fixture3D> = (0 until count).map { i ->
        Fixture3D(
            fixture = Fixture("f$i", "Fixture $i", i * 3, 3, 0),
            position = Vec3(i.toFloat(), 0f, 0f)
        )
    }

    private fun makeEngine(fixtures: List<Fixture3D>, scope: TestScope): EffectEngine {
        return EffectEngine(scope, fixtures)
    }

    private val registry = EffectRegistry().apply { register(SolidColorEffect()) }

    private val stubBeatClock = object : BeatClock {
        override val beatState: StateFlow<BeatState> = MutableStateFlow(BeatState.IDLE)
        override val isRunning: StateFlow<Boolean> = MutableStateFlow(false)
        override val bpm: StateFlow<Float> = MutableStateFlow(120f)
    }

    @Test
    fun exposesFixturesList() = runTest {
        val fixtures = makeFixtures(4)
        val engine = makeEngine(fixtures, this)
        val vm = StageViewModel(engine, registry, stubBeatClock, this)
        assertEquals(4, vm.fixtures.value.size)
    }

    @Test
    fun exposesMasterDimmer() = runTest {
        val engine = makeEngine(emptyList(), this)
        val vm = StageViewModel(engine, registry, stubBeatClock, this)
        assertEquals(1.0f, vm.masterDimmer.value)
        vm.setMasterDimmer(0.5f)
        assertEquals(0.5f, vm.masterDimmer.value)
    }

    @Test
    fun exposesEffectLayers() = runTest {
        val engine = makeEngine(emptyList(), this)
        val vm = StageViewModel(engine, registry, stubBeatClock, this)
        assertTrue(vm.layers.value.isEmpty())
        vm.setEffect(0, "solid-color", EffectParams.EMPTY.with("color", Color.RED))
        assertEquals(1, vm.layers.value.size)
    }

    @Test
    fun addAndRemoveFixture() = runTest {
        val engine = makeEngine(emptyList(), this)
        val vm = StageViewModel(engine, registry, stubBeatClock, this)
        val fixture = Fixture3D(
            fixture = Fixture("new", "New", 0, 3, 0),
            position = Vec3.ZERO
        )
        vm.addFixture(fixture)
        assertEquals(1, vm.fixtures.value.size)
        vm.removeFixture(0)
        assertEquals(0, vm.fixtures.value.size)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.chromadmx.ui.viewmodel.StageViewModelTest"`

**Step 3: Implement StageViewModel**

Create `shared/src/commonMain/kotlin/com/chromadmx/ui/viewmodel/StageViewModel.kt`:

```kotlin
package com.chromadmx.ui.viewmodel

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.BlendMode
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3
import com.chromadmx.engine.effect.EffectLayer
import com.chromadmx.engine.effect.EffectRegistry
import com.chromadmx.engine.effect.EffectStack
import com.chromadmx.engine.pipeline.EffectEngine
import com.chromadmx.tempo.clock.BeatClock
import com.chromadmx.tempo.tap.TapTempoClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Combined ViewModel for the Stage Preview screen.
 *
 * Merges responsibilities of the old PerformViewModel (effects, dimmer, beat)
 * and MapViewModel (fixture list, positions). This is the primary ViewModel
 * for the main screen.
 */
class StageViewModel(
    private val engine: EffectEngine,
    private val effectRegistry: EffectRegistry,
    private val beatClock: BeatClock,
    private val scope: CoroutineScope,
) {
    private val effectStack: EffectStack get() = engine.effectStack

    // --- Beat state ---
    val beatState: StateFlow<BeatState> = beatClock.beatState
    val isRunning: StateFlow<Boolean> = beatClock.isRunning
    val bpm: StateFlow<Float> = beatClock.bpm

    // --- Master dimmer ---
    private val _masterDimmer = MutableStateFlow(effectStack.masterDimmer)
    val masterDimmer: StateFlow<Float> = _masterDimmer.asStateFlow()

    // --- Effect layers ---
    private val _layers = MutableStateFlow(effectStack.layers)
    val layers: StateFlow<List<EffectLayer>> = _layers.asStateFlow()

    // --- Fixtures ---
    private val _fixtures = MutableStateFlow(engine.fixtures)
    val fixtures: StateFlow<List<Fixture3D>> = _fixtures.asStateFlow()

    private val _selectedFixtureIndex = MutableStateFlow<Int?>(null)
    val selectedFixtureIndex: StateFlow<Int?> = _selectedFixtureIndex.asStateFlow()

    // --- Stage view mode ---
    private val _isTopDownView = MutableStateFlow(true)
    val isTopDownView: StateFlow<Boolean> = _isTopDownView.asStateFlow()

    private val syncJob: Job = scope.launch {
        while (isActive) {
            syncFromEngine()
            delay(500L)
        }
    }

    fun onCleared() {
        syncJob.cancel()
        scope.coroutineContext[Job]?.cancel()
    }

    private fun syncFromEngine() {
        _masterDimmer.value = effectStack.masterDimmer
        _layers.value = effectStack.layers
    }

    // --- Effect controls ---
    fun availableEffects(): Set<String> = effectRegistry.ids()

    fun setEffect(layerIndex: Int, effectId: String, params: EffectParams = EffectParams.EMPTY) {
        val effect = effectRegistry.get(effectId) ?: return
        val layer = EffectLayer(effect = effect, params = params)
        if (layerIndex < effectStack.layerCount) {
            effectStack.setLayer(layerIndex, layer)
        } else {
            effectStack.addLayer(layer)
        }
        syncFromEngine()
    }

    fun setMasterDimmer(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        effectStack.masterDimmer = clamped
        _masterDimmer.value = clamped
    }

    fun setLayerOpacity(layerIndex: Int, opacity: Float) {
        if (layerIndex >= effectStack.layerCount) return
        val current = effectStack.layers[layerIndex]
        effectStack.setLayer(layerIndex, current.copy(opacity = opacity.coerceIn(0f, 1f)))
        syncFromEngine()
    }

    fun setLayerBlendMode(layerIndex: Int, blendMode: BlendMode) {
        if (layerIndex >= effectStack.layerCount) return
        val current = effectStack.layers[layerIndex]
        effectStack.setLayer(layerIndex, current.copy(blendMode = blendMode))
        syncFromEngine()
    }

    fun toggleLayerEnabled(layerIndex: Int) {
        if (layerIndex >= effectStack.layerCount) return
        val current = effectStack.layers[layerIndex]
        effectStack.setLayer(layerIndex, current.copy(enabled = !current.enabled))
        syncFromEngine()
    }

    fun removeLayer(layerIndex: Int) {
        if (layerIndex >= effectStack.layerCount) return
        effectStack.removeLayerAt(layerIndex)
        syncFromEngine()
    }

    fun addLayer() {
        val firstEffect = effectRegistry.ids().firstOrNull()?.let { effectRegistry.get(it) } ?: return
        effectStack.addLayer(EffectLayer(effect = firstEffect))
        syncFromEngine()
    }

    fun tap() {
        (beatClock as? TapTempoClock)?.tap()
    }

    // --- Fixture controls ---
    fun selectFixture(index: Int?) {
        _selectedFixtureIndex.value = index
    }

    fun updateFixturePosition(index: Int, newPosition: Vec3) {
        val current = _fixtures.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(position = newPosition)
            _fixtures.value = current
        }
    }

    fun addFixture(fixture: Fixture3D) {
        _fixtures.value = _fixtures.value + fixture
    }

    fun removeFixture(index: Int) {
        val current = _fixtures.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _fixtures.value = current
            if (_selectedFixtureIndex.value == index) {
                _selectedFixtureIndex.value = null
            }
        }
    }

    // --- View toggle ---
    fun toggleViewMode() {
        _isTopDownView.value = !_isTopDownView.value
    }
}
```

**Step 4: Run test**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.chromadmx.ui.viewmodel.StageViewModelTest"`
Expected: PASS

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/chromadmx/ui/viewmodel/StageViewModel.kt
git add shared/src/commonTest/kotlin/com/chromadmx/ui/viewmodel/StageViewModelTest.kt
git commit -m "feat(ui): add StageViewModel combining perform + map concerns (#20)"
```

---

### Task 4: Create SettingsViewModel

**Files:**
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/viewmodel/SettingsViewModel.kt`
- Test: `shared/src/commonTest/kotlin/com/chromadmx/ui/viewmodel/SettingsViewModelTest.kt`

**Step 1: Write the failing test**

```kotlin
package com.chromadmx.ui.viewmodel

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsViewModelTest {
    @Test
    fun simulationModeDefaultsToOff() = runTest {
        val vm = SettingsViewModel(scope = this)
        assertFalse(vm.simulationEnabled.value)
    }

    @Test
    fun toggleSimulationMode() = runTest {
        val vm = SettingsViewModel(scope = this)
        vm.setSimulationEnabled(true)
        assertTrue(vm.simulationEnabled.value)
    }
}
```

**Step 2: Implement SettingsViewModel**

```kotlin
package com.chromadmx.ui.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the Settings overlay.
 */
class SettingsViewModel(
    private val scope: CoroutineScope,
) {
    private val _simulationEnabled = MutableStateFlow(false)
    val simulationEnabled: StateFlow<Boolean> = _simulationEnabled.asStateFlow()

    private val _agentApiKey = MutableStateFlow("")
    val agentApiKey: StateFlow<String> = _agentApiKey.asStateFlow()

    fun setSimulationEnabled(enabled: Boolean) {
        _simulationEnabled.value = enabled
    }

    fun setAgentApiKey(key: String) {
        _agentApiKey.value = key
    }

    fun onCleared() {
        scope.coroutineContext[Job]?.cancel()
    }
}
```

**Step 3: Run tests**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.chromadmx.ui.viewmodel.SettingsViewModelTest"`
Expected: PASS

**Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/chromadmx/ui/viewmodel/SettingsViewModel.kt
git add shared/src/commonTest/kotlin/com/chromadmx/ui/viewmodel/SettingsViewModelTest.kt
git commit -m "feat(ui): add SettingsViewModel (#20)"
```

---

### Task 5: Rewrite ChromaDmxApp with AppState Navigation

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/ChromaDmxApp.kt`
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/stage/StagePreviewScreen.kt`
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/settings/SettingsScreen.kt`
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/onboarding/OnboardingScreen.kt`

**Step 1: Create StagePreviewScreen placeholder**

Create `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/stage/StagePreviewScreen.kt`:

```kotlin
package com.chromadmx.ui.screen.stage

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.components.VenueCanvas
import com.chromadmx.ui.viewmodel.StageViewModel

/**
 * Main stage preview screen — the primary screen users interact with.
 *
 * Shows the venue canvas with fixture colors, preset strip at bottom,
 * BPM/network info at top, and settings gear icon.
 */
@Composable
fun StagePreviewScreen(
    viewModel: StageViewModel,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fixtures by viewModel.fixtures.collectAsState()
    val bpm by viewModel.bpm.collectAsState()
    val masterDimmer by viewModel.masterDimmer.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        // Main venue canvas
        VenueCanvas(
            fixtures = fixtures,
            fixtureColors = emptyList(), // TODO: wire to engine output
            modifier = Modifier.fillMaxSize()
        )

        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${bpm.toInt()} BPM",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        // Master dimmer (bottom-left)
        Text(
            text = "Dimmer: ${(masterDimmer * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
        )
    }
}
```

**Step 2: Create SettingsScreen placeholder**

Create `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/settings/SettingsScreen.kt`:

```kotlin
package com.chromadmx.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.viewmodel.SettingsViewModel

/**
 * Settings overlay screen.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val simulationEnabled by viewModel.simulationEnabled.collectAsState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Settings", style = MaterialTheme.typography.headlineMedium)
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Simulation toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Simulation Mode", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = simulationEnabled,
                    onCheckedChange = { viewModel.setSimulationEnabled(it) }
                )
            }
        }
    }
}
```

**Step 3: Create OnboardingScreen placeholder**

Create `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/onboarding/OnboardingScreen.kt`:

```kotlin
package com.chromadmx.ui.screen.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.navigation.OnboardingStep

/**
 * Onboarding flow screen. Placeholder — will be fully implemented in #19.
 */
@Composable
fun OnboardingScreen(
    step: OnboardingStep,
    onAdvance: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = when (step) {
                    OnboardingStep.SPLASH -> "ChromaDMX"
                    OnboardingStep.NETWORK_DISCOVERY -> "Scanning for lights..."
                    OnboardingStep.FIXTURE_SCAN -> "Mapping fixtures..."
                    OnboardingStep.VIBE_CHECK -> "What's tonight's vibe?"
                },
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onAdvance) {
                Text("Continue")
            }
        }
    }
}
```

**Step 4: Rewrite ChromaDmxApp.kt**

Replace the entire `ChromaDmxApp.kt` with the new AppState-based navigation:

```kotlin
package com.chromadmx.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.chromadmx.ui.navigation.AppState
import com.chromadmx.ui.navigation.AppStateManager
import com.chromadmx.ui.screen.onboarding.OnboardingScreen
import com.chromadmx.ui.screen.settings.SettingsScreen
import com.chromadmx.ui.screen.stage.StagePreviewScreen
import com.chromadmx.ui.theme.ChromaDmxTheme
import com.chromadmx.ui.viewmodel.SettingsViewModel
import com.chromadmx.ui.viewmodel.StageViewModel
import org.koin.compose.getKoin

/**
 * Root composable for the ChromaDMX application.
 *
 * Uses [AppStateManager] for navigation: Onboarding → StagePreview ← Settings.
 * No tab bar. Single main screen with contextual overlays.
 */
@Composable
fun ChromaDmxApp() {
    ChromaDmxTheme {
        // TODO: Read isFirstLaunch from persistent storage
        val appStateManager = remember { AppStateManager(isFirstLaunch = false) }
        val currentState by appStateManager.currentState.collectAsState()

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            when (val state = currentState) {
                is AppState.Onboarding -> {
                    OnboardingScreen(
                        step = state.step,
                        onAdvance = { appStateManager.advanceOnboarding() },
                    )
                }
                is AppState.StagePreview -> {
                    val stageVm = resolveOrNull<StageViewModel>()
                    if (stageVm != null) {
                        DisposableEffect(stageVm) {
                            onDispose { stageVm.onCleared() }
                        }
                        StagePreviewScreen(
                            viewModel = stageVm,
                            onSettingsClick = { appStateManager.navigateTo(AppState.Settings) },
                        )
                    } else {
                        ScreenPlaceholder("Stage Preview", "Engine services not yet registered in DI.")
                    }
                }
                is AppState.Settings -> {
                    val settingsVm = resolveOrNull<SettingsViewModel>()
                    if (settingsVm != null) {
                        DisposableEffect(settingsVm) {
                            onDispose { settingsVm.onCleared() }
                        }
                        SettingsScreen(
                            viewModel = settingsVm,
                            onClose = { appStateManager.navigateBack() },
                        )
                    } else {
                        ScreenPlaceholder("Settings", "Services not registered.")
                    }
                }
            }
        }
    }
}

@Composable
private inline fun <reified T : Any> resolveOrNull(): T? {
    val koin = getKoin()
    return remember { runCatching { koin.get<T>() }.getOrNull() }
}

@Composable
private fun ScreenPlaceholder(title: String, subtitle: String) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        androidx.compose.material3.Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        androidx.compose.material3.Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
```

**Step 5: Compile check**

Run: `./gradlew :shared:compileCommonMainKotlinMetadata`
Expected: PASS

**Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/com/chromadmx/ui/ChromaDmxApp.kt
git add shared/src/commonMain/kotlin/com/chromadmx/ui/screen/stage/StagePreviewScreen.kt
git add shared/src/commonMain/kotlin/com/chromadmx/ui/screen/settings/SettingsScreen.kt
git add shared/src/commonMain/kotlin/com/chromadmx/ui/screen/onboarding/OnboardingScreen.kt
git commit -m "feat(ui): rewrite app root with AppState navigation, remove tab bar (#20)"
```

---

### Task 6: Update Koin DI Module

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/di/UiModule.kt`

**Step 1: Add StageViewModel and SettingsViewModel factories**

Add to `UiModule.kt`:

```kotlin
factory {
    val parentScope: CoroutineScope = get()
    val childJob = SupervisorJob(parentScope.coroutineContext[Job])
    val vmScope = CoroutineScope(Dispatchers.Default + childJob)
    StageViewModel(
        engine = get(),
        effectRegistry = get(),
        beatClock = get(),
        scope = vmScope,
    )
}

factory {
    val parentScope: CoroutineScope = get()
    val childJob = SupervisorJob(parentScope.coroutineContext[Job])
    val vmScope = CoroutineScope(Dispatchers.Default + childJob)
    SettingsViewModel(scope = vmScope)
}
```

Keep the old ViewModel factories temporarily (PerformViewModel, NetworkViewModel, MapViewModel, AgentViewModel) — they will be removed after verifying everything works. AgentViewModel stays permanently for the chat panel.

**Step 2: Compile check**

Run: `./gradlew :shared:compileCommonMainKotlinMetadata`
Expected: PASS

**Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/chromadmx/ui/di/UiModule.kt
git commit -m "feat(ui): add StageViewModel and SettingsViewModel to Koin DI (#20)"
```

---

### Task 7: Remove Old Tab Navigation and Unused Screens

**Files:**
- Delete: `shared/src/commonMain/kotlin/com/chromadmx/ui/navigation/Screen.kt`
- Keep but stop routing to: `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/perform/` (internals reused by StagePreview)
- Keep: `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/agent/` (reused by Chat Panel in #22)
- Delete old ViewModel factories from `UiModule.kt` for PerformViewModel, NetworkViewModel, MapViewModel

**Step 1: Delete Screen.kt**

Delete `shared/src/commonMain/kotlin/com/chromadmx/ui/navigation/Screen.kt`

**Step 2: Remove old ViewModel factories from UiModule**

Remove the `PerformViewModel`, `NetworkViewModel`, and `MapViewModel` factory blocks from `UiModule.kt`. Keep `AgentViewModel`.

**Step 3: Compile check**

Run: `./gradlew :shared:compileCommonMainKotlinMetadata`

Fix any remaining references to `Screen` enum. The old screen composables (PerformScreen, NetworkScreen, MapScreen) can stay as files but are no longer routed to from `ChromaDmxApp`. They may be refactored later or their UI elements reused in StagePreviewScreen.

**Step 4: Build Android app**

Run: `./gradlew :android:app:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add -A
git commit -m "refactor(ui): remove old tab navigation and unused ViewModel factories (#20)"
```

---

### Task 8: Full Integration Verification

**Step 1: Run all tests**

```bash
./gradlew :shared:core:testAndroidHostTest :shared:engine:testAndroidHostTest :shared:networking:testAndroidHostTest :shared:simulation:testAndroidHostTest :shared:tempo:testAndroidHostTest :shared:vision:testAndroidHostTest :shared:agent:testAndroidHostTest
```

Expected: ALL PASS

**Step 2: Build and verify Android app launches**

```bash
./gradlew :android:app:assembleDebug
```

Deploy to emulator and verify:
- App launches to Stage Preview (no tabs)
- Gear icon visible in top-right
- Tapping gear icon opens Settings overlay
- Close button returns to Stage Preview
- Venue canvas renders (even if empty)

**Step 3: Commit any fixups**

```bash
git add -A
git commit -m "fix: resolve integration issues from screen architecture overhaul (#20)"
```
