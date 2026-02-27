package com.chromadmx.ui.state

import androidx.compose.runtime.Immutable
import com.chromadmx.agent.scene.Scene
import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.BlendMode
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3
import com.chromadmx.core.persistence.FixtureGroup
import com.chromadmx.engine.effect.EffectLayer
import com.chromadmx.core.model.DmxNode

/**
 * Performance-related UI state (tempo, master, layers).
 */
@Immutable
data class PerformanceState(
    val beatState: BeatState = BeatState.IDLE,
    val bpm: Float = 120f,
    val isRunning: Boolean = false,
    val masterDimmer: Float = 1f,
    val layers: List<EffectLayer> = emptyList(),
    val activeSceneName: String? = null
)

/**
 * Fixture-related UI state (rig, selection, groups).
 */
@Immutable
data class FixtureState(
    val fixtures: List<Fixture3D> = emptyList(),
    val fixtureColors: List<Color> = emptyList(),
    val selectedFixtureIndex: Int? = null,
    val groups: List<FixtureGroup> = emptyList(),
    val isEditMode: Boolean = false
)

/**
 * Preset and scene related UI state.
 */
@Immutable
data class PresetState(
    val allScenes: List<Scene> = emptyList(),
    val availableEffects: Set<String> = emptySet(),
    val availableGenres: List<String> = emptyList()
)

/**
 * Network and discovery UI state.
 */
@Immutable
data class NetworkState(
    val nodes: List<DmxNode> = emptyList(),
    val currentTimeMs: Long = 0,
    val isNodeListOpen: Boolean = false
)

/**
 * View-specific UI state (camera, simulation mode).
 */
@Immutable
data class ViewState(
    val mode: ViewMode = ViewMode.TOP_DOWN,
    val isSimulationMode: Boolean = false,
    val simulationPresetName: String? = null,
    val simulationFixtureCount: Int = 0
)

/**
 * Stage view mode options.
 */
enum class ViewMode {
    TOP_DOWN,
    AUDIENCE
}

/**
 * Events for the Stage screen.
 */
sealed interface StageEvent {
    // Effect controls
    data class SetEffect(val layerIndex: Int, val effectId: String, val params: EffectParams = EffectParams.EMPTY) : StageEvent
    data class SetMasterDimmer(val value: Float) : StageEvent
    data class SetLayerOpacity(val layerIndex: Int, val opacity: Float) : StageEvent
    data class SetLayerBlendMode(val layerIndex: Int, val blendMode: BlendMode) : StageEvent
    data class ToggleLayerEnabled(val layerIndex: Int) : StageEvent
    data class RemoveLayer(val layerIndex: Int) : StageEvent
    data class ReorderLayer(val fromIndex: Int, val toIndex: Int) : StageEvent
    data class ApplyScene(val name: String) : StageEvent
    data class PreviewScene(val name: String?) : StageEvent
    data object AddLayer : StageEvent
    data object TapTempo : StageEvent

    // Fixture controls
    data class SelectFixture(val index: Int?) : StageEvent
    data class UpdateFixturePosition(val index: Int, val newPosition: Vec3) : StageEvent
    data class PersistFixturePosition(val index: Int) : StageEvent
    data class AddFixture(val fixture: Fixture3D) : StageEvent
    data class RemoveFixture(val index: Int) : StageEvent
    data class UpdateZHeight(val index: Int, val z: Float) : StageEvent
    data class AssignGroup(val index: Int, val groupId: String?) : StageEvent
    data class CreateGroup(val name: String, val color: Long) : StageEvent
    data class DeleteGroup(val groupId: String) : StageEvent
    data class TestFireFixture(val index: Int) : StageEvent
    data object RescanFixtures : StageEvent

    // View controls
    data object ToggleViewMode : StageEvent
    data object ToggleEditMode : StageEvent
    data object ToggleNodeList : StageEvent
    data class DiagnoseNode(val node: DmxNode) : StageEvent

    // Simulation controls
    data class EnableSimulation(val presetName: String, val fixtureCount: Int) : StageEvent
    data object DisableSimulation : StageEvent
}
