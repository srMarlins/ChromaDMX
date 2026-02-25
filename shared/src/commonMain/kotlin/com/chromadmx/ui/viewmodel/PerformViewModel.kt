package com.chromadmx.ui.viewmodel

import com.chromadmx.agent.scene.Scene
import com.chromadmx.agent.scene.SceneStore
import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.*
import com.chromadmx.core.persistence.FixtureLibrary
import com.chromadmx.engine.effect.EffectLayer
import com.chromadmx.engine.effect.EffectRegistry
import com.chromadmx.engine.effect.EffectStack
import com.chromadmx.engine.pipeline.EffectEngine
import com.chromadmx.tempo.clock.BeatClock
import com.chromadmx.tempo.tap.TapTempoClock
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
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
 * ViewModel for the Perform screen.
 *
 * Exposes beat state, effect layers, and master dimmer controls.
 * Observes [BeatClock] for real-time musical timing and manages the
 * [EffectStack] for layer compositing.
 *
 * Periodically syncs from the [EffectStack] to pick up changes made by
 * the agent or other external sources.
 */
class PerformViewModel(
    private val engine: EffectEngine,
    val effectRegistry: EffectRegistry,
    private val sceneStore: SceneStore,
    private val beatClock: BeatClock,
    private val fixtureLibrary: FixtureLibrary,
    private val scope: CoroutineScope,
) {
    private val effectStack: EffectStack get() = engine.effectStack

    val beatState: StateFlow<BeatState> = beatClock.beatState
    val isRunning: StateFlow<Boolean> = beatClock.isRunning
    val bpm: StateFlow<Float> = beatClock.bpm

    private val _masterDimmer = MutableStateFlow(effectStack.masterDimmer)
    val masterDimmer: StateFlow<Float> = _masterDimmer.asStateFlow()

    private val _layers = MutableStateFlow(effectStack.layers)
    val layers: StateFlow<List<EffectLayer>> = _layers.asStateFlow()

    private val _scenes = MutableStateFlow<List<String>>(emptyList())
    val scenes: StateFlow<List<String>> = _scenes.asStateFlow()

    val fixtures: StateFlow<List<Fixture3D>> = fixtureLibrary.fixtures
    val groups: StateFlow<List<FixtureGroup>> = fixtureLibrary.groups

    private val _selectedFixtureIndices = MutableStateFlow<Set<Int>>(emptySet())
    val selectedFixtureIndices: StateFlow<Set<Int>> = _selectedFixtureIndices.asStateFlow()

    val allScenes: StateFlow<List<Scene>> = _scenes.map { names ->
        names.mapNotNull { sceneStore.load(it) }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    private var layersBeforePreview: List<EffectLayer>? = null
    private var masterDimmerBeforePreview: Float? = null

    private val syncJob: Job

    init {
        // Periodically sync from the engine to pick up external changes
        // (e.g., agent tool calls loading a scene).
        syncJob = scope.launch {
            while (isActive) {
                syncFromEngine()
                delay(500L) // 2 Hz sync rate — responsive without being wasteful
            }
        }
    }

    /** Cancel all coroutines launched by this ViewModel. */
    fun onCleared() {
        syncJob.cancel()
        scope.coroutineContext[Job]?.cancel()
    }

    /** Pull current state from the EffectStack into our StateFlows. */
    private fun syncFromEngine() {
        _masterDimmer.value = effectStack.masterDimmer
        _layers.value = effectStack.layers
        _scenes.value = sceneStore.list()
    }

    fun availableEffects(): Set<String> = effectRegistry.ids()

    fun availableGenres(): List<String> = listOf("techno", "ambient", "house", "default")

    /**
     * Set the effect on a given layer by its registry ID.
     * Creates a new layer if [layerIndex] equals current layer count.
     */
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
        val scene = sceneStore.load(name) ?: return
        layersBeforePreview = null
        masterDimmerBeforePreview = null
        applySceneToStack(scene)
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
            val scene = sceneStore.load(name) ?: return
            if (layersBeforePreview == null) {
                layersBeforePreview = effectStack.layers
                masterDimmerBeforePreview = effectStack.masterDimmer
            }
            applySceneToStack(scene)
            syncFromEngine()
        }
    }

    private fun applySceneToStack(scene: Scene) {
        val newLayers = scene.layers.mapNotNull { config ->
            val effect = effectRegistry.get(config.effectId) ?: return@mapNotNull null
            EffectLayer(
                effect = effect,
                params = EffectParams(config.params.mapValues { it.value.toVariant() }),
                blendMode = try {
                    BlendMode.valueOf(config.blendMode.uppercase())
                } catch (e: Exception) {
                    BlendMode.NORMAL
                },
                opacity = config.opacity
            )
        }
        effectStack.replaceLayers(newLayers)
        effectStack.masterDimmer = scene.masterDimmer
    }

    private fun Float.toVariant(): Any = this // EffectParams takes Any

    fun addLayer() {
        val ids = effectRegistry.ids()
        val firstEffect = ids.firstOrNull()?.let { effectRegistry.get(it) } ?: return
        effectStack.addLayer(EffectLayer(effect = firstEffect))
        syncFromEngine()
    }

    fun tap() {
        (beatClock as? TapTempoClock)?.tap()
    }

    /* ------------------------------------------------------------------ */
    /*  Fixture Spatial Editor                                            */
    /* ------------------------------------------------------------------ */

    fun selectFixture(index: Int, multiSelect: Boolean = false) {
        if (multiSelect) {
            val current = _selectedFixtureIndices.value
            if (current.contains(index)) {
                _selectedFixtureIndices.value = current - index
            } else {
                _selectedFixtureIndices.value = current + index
            }
        } else {
            _selectedFixtureIndices.value = setOf(index)
        }
    }

    fun selectFixturesInRegion(xRange: ClosedFloatingPointRange<Float>, yRange: ClosedFloatingPointRange<Float>) {
        val inRegion = fixtures.value.withIndex().filter { (_, f) ->
            f.position.x in xRange && f.position.y in yRange
        }.map { it.index }.toSet()
        _selectedFixtureIndices.value = inRegion
    }

    fun clearSelection() {
        _selectedFixtureIndices.value = emptySet()
    }

    fun updateSelectedFixturesPosition(newPos: Vec3) {
        val selected = _selectedFixtureIndices.value
        if (selected.isEmpty()) return

        val currentFixtures = fixtures.value.toMutableList()
        selected.forEach { index ->
            if (index in currentFixtures.indices) {
                currentFixtures[index] = currentFixtures[index].copy(position = newPos)
            }
        }

        fixtureLibrary.saveFixtures(currentFixtures)
        engine.updateFixtures(currentFixtures)
    }

    fun updateFixturePosition(index: Int, newPos: Vec3) {
        fixtureLibrary.updateFixture(index) { it.copy(position = newPos) }
        engine.updateFixtures(fixtures.value)
    }

    fun setZHeight(height: Float) {
        val selected = _selectedFixtureIndices.value
        if (selected.isEmpty()) return

        val currentFixtures = fixtures.value.toMutableList()
        selected.forEach { index ->
            if (index in currentFixtures.indices) {
                currentFixtures[index] = currentFixtures[index].copy(
                    position = currentFixtures[index].position.copy(z = height)
                )
            }
        }
        fixtureLibrary.saveFixtures(currentFixtures)
        engine.updateFixtures(currentFixtures)
    }

    fun assignToGroup(groupId: String?) {
        val selected = _selectedFixtureIndices.value
        if (selected.isEmpty()) return

        val currentFixtures = fixtures.value.toMutableList()
        selected.forEach { index ->
            if (index in currentFixtures.indices) {
                currentFixtures[index] = currentFixtures[index].copy(groupId = groupId)
            }
        }
        fixtureLibrary.saveFixtures(currentFixtures)
        engine.updateFixtures(currentFixtures)
    }

    fun createGroup(name: String, color: Color) {
        val id = name.lowercase().replace(" ", "_") + "_" + (hashCode() % 1000)
        fixtureLibrary.upsertGroup(FixtureGroup(id, name, color))
    }

    fun testFireSelected() {
        val selected = _selectedFixtureIndices.value
        selected.forEach { index ->
            engine.setOverride(index, Color.WHITE)
            scope.launch {
                delay(1000L)
                engine.setOverride(index, null)
            }
        }
    }

    fun reScanFixtures() {
        // Placeholder for vision scan trigger
    }
}
