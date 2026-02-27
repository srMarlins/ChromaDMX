package com.chromadmx.ui.state

import androidx.compose.runtime.Immutable
import com.chromadmx.agent.scene.Scene
import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.BlendMode
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.ScenePreset
import com.chromadmx.core.model.Vec3
import com.chromadmx.core.persistence.FixtureGroup
import com.chromadmx.engine.effect.EffectLayer
import com.chromadmx.core.model.DmxNode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

/**
 * Performance-related UI state (master dimmer, layers, active scene).
 */
@Immutable
data class PerformanceState(
    val masterDimmer: Float = 1f,
    val layers: ImmutableList<EffectLayer> = persistentListOf(),
    val activeSceneName: String? = null,
)

/**
 * Beat/tempo UI state — separated from [PerformanceState] so that
 * high-frequency beat updates don't recompose the layer panel or dimmer.
 */
@Immutable
data class BeatUiState(
    val beatState: BeatState = BeatState.IDLE,
    val bpm: Float = 120f,
    val isRunning: Boolean = false,
)

/**
 * Fixture-related UI state (rig, selection, groups).
 */
@Immutable
data class FixtureState(
    val fixtures: ImmutableList<Fixture3D> = persistentListOf(),
    val selectedFixtureIndex: Int? = null,
    val groups: ImmutableList<FixtureGroup> = persistentListOf(),
    val isEditMode: Boolean = false
)

/**
 * Preset and scene related UI state.
 */
@Immutable
data class PresetState(
    val allScenes: ImmutableList<Scene> = persistentListOf(),
    val allPresets: ImmutableList<ScenePreset> = persistentListOf(),
    val favoriteIds: ImmutableSet<String> = persistentSetOf(),
    val availableEffects: ImmutableSet<String> = persistentSetOf(),
    val availableGenres: ImmutableList<String> = persistentListOf()
)

/**
 * Network and discovery UI state.
 */
@Immutable
data class NetworkState(
    val nodes: ImmutableList<DmxNode> = persistentListOf(),
    val currentTimeMs: Long = 0,
    val isNodeListOpen: Boolean = false,
    val diagnosticsResult: NodeDiagnostics? = null
)

/**
 * Diagnostic snapshot of a single DMX node for the overlay.
 *
 * Gathered from the discovered [DmxNode] data and, when available, a
 * network ping measurement.
 */
@Immutable
data class NodeDiagnostics(
    val nodeName: String,
    val ipAddress: String,
    val macAddress: String,
    val firmwareVersion: String,
    val latencyMs: Long,
    val universes: ImmutableList<Int>,
    val numPorts: Int,
    val uptimeMs: Long,
    val isAlive: Boolean,
    val lastError: String? = null,
    val frameCount: Long = 0L,
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
    data object DismissDiagnostics : StageEvent

    // Preset management
    data class SaveCurrentPreset(val name: String, val genre: String) : StageEvent
    data class DeletePreset(val id: String) : StageEvent
    data class ToggleFavorite(val presetId: String) : StageEvent

    // Simulation controls
    data class EnableSimulation(val presetName: String, val fixtureCount: Int) : StageEvent
    data object DisableSimulation : StageEvent
}
