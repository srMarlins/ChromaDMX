package com.chromadmx.ui.viewmodel

import com.chromadmx.agent.controller.FixtureController
import com.chromadmx.agent.scene.Scene
import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.EffectLayerConfig
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Genre
import com.chromadmx.core.model.ScenePreset
import com.chromadmx.core.model.Vec3
import com.chromadmx.core.persistence.FixtureGroup
import com.chromadmx.core.persistence.FixtureRepository
import com.chromadmx.engine.effect.EffectLayer
import com.chromadmx.engine.effect.EffectRegistry
import com.chromadmx.engine.effect.EffectStack
import com.chromadmx.engine.pipeline.EffectEngine
import com.chromadmx.engine.preset.PresetLibrary
import com.chromadmx.networking.FixtureDiscovery
import com.chromadmx.networking.discovery.NodeDiscovery
import com.chromadmx.networking.discovery.currentTimeMillis
import com.chromadmx.tempo.clock.BeatClock
import com.chromadmx.tempo.tap.TapTempoClock
import com.chromadmx.core.model.Color as DmxColor
import com.chromadmx.ui.state.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Stage screen ViewModel with 5 sliced StateFlows.
 *
 * Each UI region subscribes only to its relevant state slice, avoiding
 * unnecessary recompositions when unrelated state changes.
 *
 * Slices:
 * - [performanceState] — tempo, master dimmer, effect layers, active scene
 * - [fixtureState] — fixture list, selection, groups, edit mode
 * - [presetState] — available presets/effects/genres
 * - [networkState] — discovered nodes, node list visibility
 * - [viewState] — view mode, simulation mode
 *
 * High-frequency data (fixture colors, beat phase) is emitted via
 * [SharedFlow]s to avoid backpressure on the state slices.
 */
class StageViewModelV2(
    private val engine: EffectEngine,
    val effectRegistry: EffectRegistry,
    private val presetLibrary: PresetLibrary,
    private val beatClock: BeatClock,
    private val fixtureDiscovery: FixtureDiscovery,
    private val nodeDiscovery: NodeDiscovery?,
    private val scope: CoroutineScope,
    private val fixtureRepository: FixtureRepository? = null,
    private val fixtureController: FixtureController? = null,
) {
    private val effectStack: EffectStack get() = engine.effectStack

    // ── Sliced state flows ─────────────────────────────────────────────

    private val _performanceState = MutableStateFlow(PerformanceState(
        masterDimmer = effectStack.masterDimmer,
        layers = effectStack.layers,
    ))
    val performanceState: StateFlow<PerformanceState> = _performanceState.asStateFlow()

    private val _fixtureState = MutableStateFlow(FixtureState(
        fixtures = if (fixtureRepository != null) emptyList() else engine.fixtures,
    ))
    val fixtureState: StateFlow<FixtureState> = _fixtureState.asStateFlow()

    private val _presetState = MutableStateFlow(PresetState(
        availableEffects = effectRegistry.ids(),
        availableGenres = listOf("techno", "ambient", "house", "default"),
    ))
    val presetState: StateFlow<PresetState> = _presetState.asStateFlow()

    private val _networkState = MutableStateFlow(NetworkState())
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

    private val _viewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = _viewState.asStateFlow()

    // ── High-frequency shared flows ────────────────────────────────────

    private val _fixtureColors = MutableSharedFlow<List<DmxColor>>(replay = 1)
    val fixtureColors: SharedFlow<List<DmxColor>> = _fixtureColors.asSharedFlow()

    private val _beatPhase = MutableSharedFlow<Float>(replay = 1)
    val beatPhase: SharedFlow<Float> = _beatPhase.asSharedFlow()

    // ── Preview stack for scene preview/revert ─────────────────────────

    private var layersBeforePreview: List<EffectLayer>? = null
    private var masterDimmerBeforePreview: Float? = null

    // ── Background jobs ────────────────────────────────────────────────

    /** Syncs engine state (layers, scenes, dimmer) at a moderate rate. */
    private val syncJob: Job = scope.launch {
        while (isActive) {
            syncFromEngine()
            _networkState.update { it.copy(currentTimeMs = currentTimeMillis()) }
            delay(500L)
        }
    }

    /** Reads fixture colors from the engine triple buffer at ~30fps. */
    private val colorSyncJob: Job = scope.launch {
        while (isActive) {
            syncColorsFromEngine()
            delay(33L)
        }
    }

    /** Collect beat state from beat clock into performance state. */
    private val beatSyncJob: Job = scope.launch {
        beatClock.beatState.collect { beat ->
            _performanceState.update { it.copy(beatState = beat, bpm = beat.bpm) }
        }
    }

    /** Collect running state from beat clock. */
    private val runningSyncJob: Job = scope.launch {
        beatClock.isRunning.collect { running ->
            _performanceState.update { it.copy(isRunning = running) }
        }
    }

    /** Collect beat phase into high-frequency shared flow. */
    private val beatPhaseSyncJob: Job = scope.launch {
        beatClock.beatPhase.collect { phase ->
            _beatPhase.emit(phase)
        }
    }

    /** Collect nodes from FixtureDiscovery interface. */
    private val discoverySyncJob: Job = scope.launch {
        fixtureDiscovery.discoveredNodes.collect { nodes ->
            _networkState.update { it.copy(nodes = nodes) }
        }
    }

    /** Collect fixtures from repository flow if available (single source of truth). */
    private val repoSyncJob: Job? = fixtureRepository?.let { repo ->
        scope.launch {
            repo.allFixtures().collect { dbFixtures ->
                _fixtureState.update { it.copy(fixtures = dbFixtures) }
                engine.updateFixtures(dbFixtures)
                // Keep simulation badge in sync when rig preset changes via Settings
                if (_viewState.value.isSimulationMode) {
                    _viewState.update {
                        it.copy(
                            simulationFixtureCount = dbFixtures.size,
                            simulationPresetName = null,
                        )
                    }
                }
            }
        }
    }

    /** Collect groups from repository flow if available. */
    private val groupSyncJob: Job? = fixtureRepository?.let { repo ->
        scope.launch {
            repo.allGroups().collect { dbGroups ->
                _fixtureState.update { it.copy(groups = dbGroups) }
            }
        }
    }

    // ── Event handler ──────────────────────────────────────────────────

    fun onEvent(event: StageEvent) {
        when (event) {
            // Effect controls
            is StageEvent.SetEffect -> handleSetEffect(event.layerIndex, event.effectId, event.params)
            is StageEvent.SetMasterDimmer -> handleSetMasterDimmer(event.value)
            is StageEvent.SetLayerOpacity -> handleSetLayerOpacity(event.layerIndex, event.opacity)
            is StageEvent.SetLayerBlendMode -> handleSetLayerBlendMode(event.layerIndex, event.blendMode)
            is StageEvent.ToggleLayerEnabled -> handleToggleLayerEnabled(event.layerIndex)
            is StageEvent.RemoveLayer -> handleRemoveLayer(event.layerIndex)
            is StageEvent.ReorderLayer -> handleReorderLayer(event.fromIndex, event.toIndex)
            is StageEvent.ApplyScene -> handleApplyScene(event.name)
            is StageEvent.PreviewScene -> handlePreviewScene(event.name)
            is StageEvent.AddLayer -> handleAddLayer()
            is StageEvent.TapTempo -> handleTapTempo()

            // Fixture controls
            is StageEvent.SelectFixture -> handleSelectFixture(event.index)
            is StageEvent.UpdateFixturePosition -> handleUpdateFixturePosition(event.index, event.newPosition)
            is StageEvent.PersistFixturePosition -> handlePersistFixturePosition(event.index)
            is StageEvent.AddFixture -> handleAddFixture(event.fixture)
            is StageEvent.RemoveFixture -> handleRemoveFixture(event.index)
            is StageEvent.UpdateZHeight -> handleUpdateZHeight(event.index, event.z)
            is StageEvent.AssignGroup -> handleAssignGroup(event.index, event.groupId)
            is StageEvent.CreateGroup -> handleCreateGroup(event.name, event.color)
            is StageEvent.DeleteGroup -> handleDeleteGroup(event.groupId)
            is StageEvent.TestFireFixture -> handleTestFireFixture(event.index)
            is StageEvent.RescanFixtures -> handleRescanFixtures()

            // View controls
            is StageEvent.ToggleViewMode -> handleToggleViewMode()
            is StageEvent.ToggleEditMode -> handleToggleEditMode()
            is StageEvent.ToggleNodeList -> handleToggleNodeList()
            is StageEvent.DiagnoseNode -> handleDiagnoseNode(event.node)
            is StageEvent.DismissDiagnostics -> handleDismissDiagnostics()

            // Preset management
            is StageEvent.SaveCurrentPreset -> handleSaveCurrentPreset(event.name, event.genre)
            is StageEvent.DeletePreset -> handleDeletePreset(event.id)
            is StageEvent.ToggleFavorite -> handleToggleFavorite(event.presetId)

            // Simulation controls
            is StageEvent.EnableSimulation -> handleEnableSimulation(event.presetName, event.fixtureCount)
            is StageEvent.DisableSimulation -> handleDisableSimulation()
        }
    }

    fun onCleared() {
        syncJob.cancel()
        colorSyncJob.cancel()
        beatSyncJob.cancel()
        runningSyncJob.cancel()
        beatPhaseSyncJob.cancel()
        discoverySyncJob.cancel()
        repoSyncJob?.cancel()
        groupSyncJob?.cancel()
        scope.coroutineContext[Job]?.cancel()
    }

    // ── Effect control handlers ────────────────────────────────────────

    private fun handleSetEffect(layerIndex: Int, effectId: String, params: EffectParams) {
        val effect = effectRegistry.get(effectId) ?: return
        val layer = EffectLayer(effect = effect, params = params)
        if (layerIndex < effectStack.layerCount) {
            effectStack.setLayer(layerIndex, layer)
        } else {
            effectStack.addLayer(layer)
        }
        syncFromEngine()
    }

    private fun handleSetMasterDimmer(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        effectStack.masterDimmer = clamped
        _performanceState.update { it.copy(masterDimmer = clamped) }
    }

    private fun handleSetLayerOpacity(layerIndex: Int, opacity: Float) {
        if (layerIndex >= effectStack.layerCount) return
        val current = effectStack.layers[layerIndex]
        effectStack.setLayer(layerIndex, current.copy(opacity = opacity.coerceIn(0f, 1f)))
        syncFromEngine()
    }

    private fun handleSetLayerBlendMode(layerIndex: Int, blendMode: com.chromadmx.core.model.BlendMode) {
        if (layerIndex >= effectStack.layerCount) return
        val current = effectStack.layers[layerIndex]
        effectStack.setLayer(layerIndex, current.copy(blendMode = blendMode))
        syncFromEngine()
    }

    private fun handleToggleLayerEnabled(layerIndex: Int) {
        if (layerIndex >= effectStack.layerCount) return
        val current = effectStack.layers[layerIndex]
        effectStack.setLayer(layerIndex, current.copy(enabled = !current.enabled))
        syncFromEngine()
    }

    private fun handleRemoveLayer(layerIndex: Int) {
        if (layerIndex >= effectStack.layerCount) return
        effectStack.removeLayerAt(layerIndex)
        syncFromEngine()
    }

    private fun handleReorderLayer(fromIndex: Int, toIndex: Int) {
        effectStack.moveLayer(fromIndex, toIndex)
        syncFromEngine()
    }

    private fun handleApplyScene(name: String) {
        val presets = presetLibrary.listPresets()
        val preset = presets.find { it.name == name } ?: return
        layersBeforePreview = null
        masterDimmerBeforePreview = null
        presetLibrary.loadPreset(preset.id)
        _performanceState.update { it.copy(activeSceneName = name) }
        syncFromEngine()
    }

    private fun handlePreviewScene(name: String?) {
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

    private fun handleAddLayer() {
        val firstEffect = effectRegistry.ids().firstOrNull()?.let { effectRegistry.get(it) } ?: return
        effectStack.addLayer(EffectLayer(effect = firstEffect))
        syncFromEngine()
    }

    private fun handleTapTempo() {
        (beatClock as? TapTempoClock)?.tap()
    }

    // ── Fixture control handlers ───────────────────────────────────────

    private fun handleSelectFixture(index: Int?) {
        _fixtureState.update { it.copy(selectedFixtureIndex = index) }
    }

    private fun handleUpdateFixturePosition(index: Int, newPosition: Vec3) {
        _fixtureState.update { state ->
            val current = state.fixtures.toMutableList()
            if (index in current.indices) {
                current[index] = current[index].copy(position = newPosition)
                state.copy(fixtures = current)
            } else {
                state
            }
        }
    }

    private fun handlePersistFixturePosition(index: Int) {
        val fixture = _fixtureState.value.fixtures.getOrNull(index) ?: return
        scope.launch {
            withContext(Dispatchers.IO) {
                fixtureRepository?.updatePosition(fixture.fixture.fixtureId, fixture.position)
            }
        }
    }

    private fun handleAddFixture(fixture: Fixture3D) {
        _fixtureState.update { state ->
            state.copy(fixtures = state.fixtures + fixture)
        }
        scope.launch { withContext(Dispatchers.IO) { fixtureRepository?.saveFixture(fixture) } }
    }

    private fun handleRemoveFixture(index: Int) {
        var removedFixtureId: String? = null
        _fixtureState.update { state ->
            val current = state.fixtures.toMutableList()
            if (index in current.indices) {
                val removed = current.removeAt(index)
                removedFixtureId = removed.fixture.fixtureId
                val newSelection = if (state.selectedFixtureIndex == index) null else state.selectedFixtureIndex
                state.copy(fixtures = current, selectedFixtureIndex = newSelection)
            } else {
                state
            }
        }
        removedFixtureId?.let { id -> scope.launch { withContext(Dispatchers.IO) { fixtureRepository?.deleteFixture(id) } } }
    }

    private fun handleUpdateZHeight(index: Int, z: Float) {
        var updatedFixtureId: String? = null
        var updatedPosition: Vec3? = null
        _fixtureState.update { state ->
            val current = state.fixtures.toMutableList()
            if (index in current.indices) {
                val f = current[index]
                val newPos = f.position.copy(z = z)
                current[index] = f.copy(position = newPos)
                updatedFixtureId = f.fixture.fixtureId
                updatedPosition = newPos
                state.copy(fixtures = current)
            } else {
                state
            }
        }
        val id = updatedFixtureId
        val pos = updatedPosition
        if (id != null && pos != null) {
            scope.launch { withContext(Dispatchers.IO) { fixtureRepository?.updatePosition(id, pos) } }
        }
    }

    private fun handleAssignGroup(index: Int, groupId: String?) {
        var assignedFixtureId: String? = null
        var assignedGroupId: String? = null
        _fixtureState.update { state ->
            val current = state.fixtures.toMutableList()
            if (index in current.indices) {
                val updated = current[index].copy(groupId = groupId)
                current[index] = updated
                assignedFixtureId = updated.fixture.fixtureId
                assignedGroupId = groupId
                state.copy(fixtures = current)
            } else {
                state
            }
        }
        assignedFixtureId?.let { id -> scope.launch { withContext(Dispatchers.IO) { fixtureRepository?.updateGroup(id, assignedGroupId) } } }
    }

    private fun handleCreateGroup(name: String, color: Long) {
        val groupId = "grp-${name.lowercase().replace(" ", "-")}-${currentTimeMillis()}-${kotlin.random.Random.nextInt(0, Int.MAX_VALUE)}"
        val group = FixtureGroup(groupId = groupId, name = name, color = color)
        _fixtureState.update { state ->
            state.copy(groups = state.groups + group)
        }
        scope.launch { withContext(Dispatchers.IO) { fixtureRepository?.saveGroup(group) } }
    }

    private fun handleDeleteGroup(groupId: String) {
        _fixtureState.update { state ->
            state.copy(groups = state.groups.filter { it.groupId != groupId })
        }
        scope.launch { withContext(Dispatchers.IO) { fixtureRepository?.deleteGroup(groupId) } }
    }

    private fun handleTestFireFixture(index: Int) {
        val fixture = _fixtureState.value.fixtures.getOrNull(index) ?: return
        scope.launch {
            fixtureController?.fireFixture(fixture.fixture.fixtureId, "#FFFFFF")
            delay(1000L)
            fixtureController?.fireFixture(fixture.fixture.fixtureId, "#000000")
        }
    }

    private fun handleRescanFixtures() {
        scope.launch {
            nodeDiscovery?.sendPoll()
        }
    }

    // ── View control handlers ──────────────────────────────────────────

    private fun handleToggleViewMode() {
        _viewState.update { state ->
            val nextMode = when (state.mode) {
                ViewMode.TOP_DOWN -> ViewMode.AUDIENCE
                ViewMode.AUDIENCE -> ViewMode.TOP_DOWN
            }
            state.copy(mode = nextMode)
        }
    }

    private fun handleToggleEditMode() {
        _fixtureState.update { it.copy(isEditMode = !it.isEditMode) }
    }

    private fun handleToggleNodeList() {
        _networkState.update { it.copy(isNodeListOpen = !it.isNodeListOpen) }
    }

    private fun handleDiagnoseNode(node: com.chromadmx.core.model.DmxNode) {
        val currentTime = currentTimeMillis()
        val diagnostics = NodeDiagnostics(
            nodeName = node.shortName.ifEmpty { node.longName.ifEmpty { "Unknown Node" } },
            ipAddress = node.ipAddress,
            macAddress = node.macAddress,
            firmwareVersion = "v${node.firmwareVersion}",
            latencyMs = node.latencyMs,
            universes = node.universes,
            numPorts = node.numPorts,
            uptimeMs = if (node.firstSeenMs > 0) currentTime - node.firstSeenMs else 0L,
            isAlive = node.isAlive(currentTime),
            lastError = if (!node.isAlive(currentTime)) "Node not responding" else null,
            frameCount = 0L,
        )
        _networkState.update {
            it.copy(
                isNodeListOpen = false,
                diagnosticsResult = diagnostics,
            )
        }
    }

    private fun handleDismissDiagnostics() {
        _networkState.update { it.copy(diagnosticsResult = null) }
    }

    // ── Simulation control handlers ────────────────────────────────────

    private fun handleEnableSimulation(presetName: String, fixtureCount: Int) {
        _viewState.update {
            it.copy(
                isSimulationMode = true,
                simulationPresetName = presetName,
                simulationFixtureCount = fixtureCount,
            )
        }
        // Auto-load: prefer the named preset from onboarding, fall back to first available
        val presets = presetLibrary.listPresets()
        val preset = presets.find { it.name == presetName }
            ?: presets.firstOrNull()
            ?: return
        presetLibrary.loadPreset(preset.id)
        _performanceState.update { it.copy(activeSceneName = preset.name) }
        syncFromEngine()
    }

    private fun handleDisableSimulation() {
        _viewState.update {
            it.copy(
                isSimulationMode = false,
                simulationPresetName = null,
                simulationFixtureCount = 0,
            )
        }
    }

    // ── Preset management handlers ─────────────────────────────────────

    private fun handleSaveCurrentPreset(name: String, genre: String) {
        val genreEnum = try {
            Genre.valueOf(genre.uppercase())
        } catch (_: Exception) {
            Genre.CUSTOM
        }
        val layers = effectStack.layers
        val layerConfigs = layers.map { layer ->
            EffectLayerConfig(
                effectId = layer.effect.id,
                params = layer.params,
                blendMode = layer.blendMode,
                opacity = layer.opacity,
                enabled = layer.enabled,
            )
        }
        val id = "user_${name.lowercase().replace(" ", "_")}_${currentTimeMillis()}"
        val preset = ScenePreset(
            id = id,
            name = name,
            genre = genreEnum,
            layers = layerConfigs,
            masterDimmer = effectStack.masterDimmer,
            isBuiltIn = false,
            createdAt = currentTimeMillis(),
            thumbnailColors = emptyList(),
        )
        presetLibrary.savePreset(preset)
        _performanceState.update { it.copy(activeSceneName = name) }
        syncFromEngine()
    }

    private fun handleDeletePreset(id: String) {
        presetLibrary.deletePreset(id)
        syncFromEngine()
    }

    private fun handleToggleFavorite(presetId: String) {
        val current = presetLibrary.getFavorites().toMutableList()
        if (presetId in current) {
            current.remove(presetId)
        } else {
            current.add(presetId)
        }
        presetLibrary.setFavorites(current)
        _presetState.update { it.copy(favoriteIds = current) }
    }

    // ── Sync helpers ───────────────────────────────────────────────────

    private fun syncFromEngine() {
        scope.launch {
            _performanceState.update { state ->
                state.copy(
                    masterDimmer = effectStack.masterDimmer,
                    layers = effectStack.layers,
                )
            }
            val (presets, favorites, scenes) = withContext(Dispatchers.Default) {
                val p = presetLibrary.listPresets()
                val f = presetLibrary.getFavorites()
                val s = p.map { preset ->
                    Scene(
                        name = preset.name,
                        layers = preset.layers.map { config ->
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
                Triple(p, f, s)
            }
            _presetState.update { state ->
                state.copy(
                    allPresets = presets,
                    favoriteIds = favorites,
                    allScenes = scenes,
                    availableEffects = effectRegistry.ids(),
                )
            }
        }
    }

    /**
     * Read the latest color frame from the engine's triple buffer and
     * emit it to the UI.
     *
     * Only emits when the engine has published new data (swapRead returns true)
     * or when the buffer reference has changed (after updateFixtures), which
     * avoids flooding the SharedFlow with stale BLACK frames that overwrite
     * the last good colors.
     */
    private var lastColorBuffer: Any? = null  // identity tracking for buffer replacement

    private fun syncColorsFromEngine() {
        val buffer = engine.colorOutput
        val bufferChanged = buffer !== lastColorBuffer
        lastColorBuffer = buffer

        val hasNewData = buffer.swapRead()

        // Emit when the engine has published a new frame, or when the buffer
        // reference changed (e.g. after updateFixtures) so the UI gets the
        // correct count even if the engine hasn't ticked yet.
        if (hasNewData || bufferChanged) {
            _fixtureColors.tryEmit(buffer.readSlot().toList())
        }
    }
}
