package com.chromadmx.ui.viewmodel

import com.chromadmx.agent.controller.FixtureController
import com.chromadmx.agent.scene.Scene
import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.BlendMode
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3
import com.chromadmx.core.persistence.FixtureGroup
import com.chromadmx.core.persistence.FixtureRepository
import com.chromadmx.engine.effect.EffectLayer
import com.chromadmx.engine.effect.EffectRegistry
import com.chromadmx.engine.effect.EffectStack
import com.chromadmx.engine.pipeline.EffectEngine
import com.chromadmx.engine.preset.PresetLibrary
import com.chromadmx.networking.discovery.NodeDiscovery
import com.chromadmx.networking.discovery.currentTimeMillis
import com.chromadmx.networking.model.DmxNode
import com.chromadmx.tempo.clock.BeatClock
import com.chromadmx.tempo.tap.TapTempoClock
import com.chromadmx.core.model.Color as DmxColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Combined ViewModel for the Stage Preview screen.
 *
 * Merges responsibilities of the old PerformViewModel (effects, dimmer, beat)
 * and MapViewModel (fixture list, positions). This is the primary ViewModel
 * for the main screen.
 *
 * Also exposes [isSimulationMode] so the UI can render a simulation badge
 * and adjust visual treatment when running with virtual fixtures.
 *
 * With SQLDelight persistence (via [fixtureRepository]), fixture positions,
 * group assignments, and group metadata are persisted across app restarts.
 */
class StageViewModel(
    private val engine: EffectEngine,
    val effectRegistry: EffectRegistry,
    private val presetLibrary: PresetLibrary,
    private val beatClock: BeatClock,
    private val nodeDiscovery: NodeDiscovery,
    private val scope: CoroutineScope,
    private val fixtureRepository: FixtureRepository? = null,
    private val fixtureController: FixtureController? = null,
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

    // --- Scenes (from PresetLibrary) ---
    private val _scenes = MutableStateFlow<List<Scene>>(emptyList())
    val allScenes: StateFlow<List<Scene>> = _scenes.asStateFlow()

    private var layersBeforePreview: List<EffectLayer>? = null
    private var masterDimmerBeforePreview: Float? = null

    // --- Fixtures ---
    private val _fixtures = MutableStateFlow(engine.fixtures)
    val fixtures: StateFlow<List<Fixture3D>> = _fixtures.asStateFlow()

    private val _selectedFixtureIndex = MutableStateFlow<Int?>(null)
    val selectedFixtureIndex: StateFlow<Int?> = _selectedFixtureIndex.asStateFlow()

    // --- Fixture colors (from engine output) ---
    private val _fixtureColors = MutableStateFlow<List<DmxColor>>(emptyList())
    val fixtureColors: StateFlow<List<DmxColor>> = _fixtureColors.asStateFlow()

    // --- Active scene name ---
    private val _activeSceneName = MutableStateFlow<String?>(null)
    val activeSceneName: StateFlow<String?> = _activeSceneName.asStateFlow()

    // --- Simulation mode ---
    private val _isSimulationMode = MutableStateFlow(false)
    val isSimulationMode: StateFlow<Boolean> = _isSimulationMode.asStateFlow()

    private val _simulationPresetName = MutableStateFlow<String?>(null)
    val simulationPresetName: StateFlow<String?> = _simulationPresetName.asStateFlow()

    private val _simulationFixtureCount = MutableStateFlow(0)
    val simulationFixtureCount: StateFlow<Int> = _simulationFixtureCount.asStateFlow()

    // --- Stage view mode ---
    private val _isTopDownView = MutableStateFlow(true)
    val isTopDownView: StateFlow<Boolean> = _isTopDownView.asStateFlow()

    // --- Edit mode ---
    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    // --- Groups ---
    private val _groups = MutableStateFlow<List<FixtureGroup>>(emptyList())
    val groups: StateFlow<List<FixtureGroup>> = _groups.asStateFlow()

    // --- Network state ---
    val nodes: StateFlow<List<DmxNode>> = nodeDiscovery.nodes
        .map { it.values.toList() }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentTimeMs = MutableStateFlow(0L)
    val currentTimeMs: StateFlow<Long> = _currentTimeMs.asStateFlow()

    private val _isNodeListOpen = MutableStateFlow(false)
    val isNodeListOpen: StateFlow<Boolean> = _isNodeListOpen.asStateFlow()

    /** Syncs engine state (layers, scenes, dimmer) at a moderate rate. */
    private val syncJob: Job = scope.launch {
        while (isActive) {
            syncFromEngine()
            _currentTimeMs.value = currentTimeMillis()
            delay(500L)
        }
    }

    /** Reads fixture colors from the engine triple buffer at ~30fps for smooth visuals. */
    private val colorSyncJob: Job = scope.launch {
        while (isActive) {
            syncColorsFromEngine()
            delay(33L) // ~30fps
        }
    }

    /** Collect fixtures from repository flow if available. */
    private val repoSyncJob: Job? = fixtureRepository?.let { repo ->
        scope.launch {
            repo.allFixtures().collect { dbFixtures ->
                if (dbFixtures.isNotEmpty()) {
                    _fixtures.value = dbFixtures
                }
            }
        }
    }

    /** Collect groups from repository flow if available. */
    private val groupSyncJob: Job? = fixtureRepository?.let { repo ->
        scope.launch {
            repo.allGroups().collect { dbGroups ->
                _groups.value = dbGroups
            }
        }
    }

    fun onCleared() {
        syncJob.cancel()
        colorSyncJob.cancel()
        repoSyncJob?.cancel()
        groupSyncJob?.cancel()
        scope.coroutineContext[Job]?.cancel()
    }

    /**
     * Read the latest fixture colors from the engine's triple buffer.
     * The reader calls swapRead() to get the most recent data, then
     * reads from readSlot().
     */
    private fun syncColorsFromEngine() {
        val buffer = engine.colorOutput
        buffer.swapRead()
        val colors = buffer.readSlot()
        _fixtureColors.value = colors.toList()
    }

    private fun syncFromEngine() {
        _masterDimmer.value = effectStack.masterDimmer
        _layers.value = effectStack.layers
        _scenes.value = presetLibrary.listPresets().map { preset ->
            Scene(
                name = preset.name,
                layers = preset.layers.map { config ->
                    @Suppress("UNCHECKED_CAST")
                    val floatParams = config.params.toMap().mapValues { (_, v) ->
                        when (v) {
                            is Float -> v
                            is Double -> v.toFloat()
                            is Int -> v.toFloat()
                            is Number -> v.toFloat()
                            else -> 0f
                        }
                    }
                    Scene.LayerConfig(
                        effectId = config.effectId,
                        params = floatParams,
                        blendMode = config.blendMode.name,
                        opacity = config.opacity,
                    )
                },
                masterDimmer = preset.masterDimmer,
            )
        }
    }

    // --- Effect controls ---
    fun availableEffects(): Set<String> = effectRegistry.ids()

    fun availableGenres(): List<String> = listOf("techno", "ambient", "house", "default")

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

    fun reorderLayer(fromIndex: Int, toIndex: Int) {
        effectStack.moveLayer(fromIndex, toIndex)
        syncFromEngine()
    }

    fun applyScene(name: String) {
        val presets = presetLibrary.listPresets()
        val preset = presets.find { it.name == name } ?: return
        layersBeforePreview = null
        masterDimmerBeforePreview = null
        presetLibrary.loadPreset(preset.id)
        _activeSceneName.value = name
        syncFromEngine()
    }

    fun previewScene(name: String?) {
        if (name == null) {
            // Revert preview
            layersBeforePreview?.let {
                effectStack.replaceLayers(it)
                layersBeforePreview = null
            }
            masterDimmerBeforePreview?.let {
                effectStack.masterDimmer = it
                masterDimmerBeforePreview = null
            }
            syncFromEngine()
        } else {
            val presets = presetLibrary.listPresets()
            val preset = presets.find { it.name == name } ?: return
            if (layersBeforePreview == null) {
                layersBeforePreview = effectStack.layers
                masterDimmerBeforePreview = effectStack.masterDimmer
            }
            presetLibrary.loadPreset(preset.id)
            syncFromEngine()
        }
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

    /**
     * Update a fixture's position in the UI model and persist to database.
     */
    fun updateFixturePosition(index: Int, newPosition: Vec3) {
        val current = _fixtures.value.toMutableList()
        if (index in current.indices) {
            val updated = current[index].copy(position = newPosition)
            current[index] = updated
            _fixtures.value = current
            fixtureRepository?.updatePosition(updated.fixture.fixtureId, newPosition)
        }
    }

    /**
     * Add a fixture to the UI model and persist to database.
     */
    fun addFixture(fixture: Fixture3D) {
        _fixtures.value = _fixtures.value + fixture
        fixtureRepository?.saveFixture(fixture)
    }

    /**
     * Remove a fixture from the UI model by index and delete from database.
     */
    fun removeFixture(index: Int) {
        val current = _fixtures.value.toMutableList()
        if (index in current.indices) {
            val removed = current.removeAt(index)
            _fixtures.value = current
            if (_selectedFixtureIndex.value == index) {
                _selectedFixtureIndex.value = null
            }
            fixtureRepository?.deleteFixture(removed.fixture.fixtureId)
        }
    }

    // --- View toggle ---
    fun toggleViewMode() {
        _isTopDownView.value = !_isTopDownView.value
    }

    // --- Edit mode ---

    /** Toggle edit mode for fixture spatial editing. */
    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
    }

    // --- Z-Height ---

    /** Update the Z-height of a fixture by index. */
    fun updateZHeight(index: Int, z: Float) {
        val current = _fixtures.value
        if (index in current.indices) {
            val fixture = current[index]
            val newPosition = fixture.position.copy(z = z)
            updateFixturePosition(index, newPosition)
        }
    }

    // --- Group management ---

    /** Assign a fixture (by index) to a group (or null to unassign). */
    fun assignGroup(index: Int, groupId: String?) {
        val current = _fixtures.value.toMutableList()
        if (index in current.indices) {
            val updated = current[index].copy(groupId = groupId)
            current[index] = updated
            _fixtures.value = current
            fixtureRepository?.updateGroup(updated.fixture.fixtureId, groupId)
        }
    }

    /** Create a new fixture group. Returns the new group ID. */
    fun createGroup(name: String, color: Long = 0xFF00FBFF): String {
        val groupId = "grp-${name.lowercase().replace(" ", "-")}-${currentTimeMillis()}-${kotlin.random.Random.nextInt()}"
        val group = FixtureGroup(groupId = groupId, name = name, color = color)
        val currentGroups = _groups.value.toMutableList()
        currentGroups.add(group)
        _groups.value = currentGroups
        fixtureRepository?.saveGroup(group)
        return groupId
    }

    /** Delete a fixture group by ID. */
    fun deleteGroup(groupId: String) {
        val currentGroups = _groups.value.toMutableList()
        currentGroups.removeAll { it.groupId == groupId }
        _groups.value = currentGroups
        fixtureRepository?.deleteGroup(groupId)
    }

    // --- Test fire ---

    /** Flash a fixture white for ~1 second via the FixtureController. */
    fun testFireFixture(index: Int) {
        val fixture = _fixtures.value.getOrNull(index) ?: return
        scope.launch {
            fixtureController?.fireFixture(fixture.fixture.fixtureId, "#FFFFFF")
            delay(1000L)
            fixtureController?.fireFixture(fixture.fixture.fixtureId, "#000000")
        }
    }

    // --- Re-scan ---

    /** Trigger a network re-scan via NodeDiscovery. */
    fun rescanFixtures() {
        scope.launch {
            nodeDiscovery.sendPoll()
        }
    }

    // --- Simulation mode controls ---

    /**
     * Enable simulation mode with the given preset name and fixture count.
     * Called when the user confirms a rig selection in the onboarding or settings flow.
     */
    fun enableSimulation(presetName: String, fixtureCount: Int) {
        _isSimulationMode.value = true
        _simulationPresetName.value = presetName
        _simulationFixtureCount.value = fixtureCount
    }

    /**
     * Disable simulation mode and clear associated metadata.
     */
    fun disableSimulation() {
        _isSimulationMode.value = false
        _simulationPresetName.value = null
        _simulationFixtureCount.value = 0
    }

    // --- Network actions ---
    fun toggleNodeList() {
        _isNodeListOpen.value = !_isNodeListOpen.value
    }

    fun diagnoseNode(node: DmxNode) {
        // In a real app, this would send a message to AgentViewModel
        // or trigger the DiagnoseConnectionTool directly.
        // For now, we'll close the overlay and let the Mascot handle it if needed.
        _isNodeListOpen.value = false
        // TODO: Wire to agent
    }
}
