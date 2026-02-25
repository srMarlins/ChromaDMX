package com.chromadmx.ui.viewmodel

import com.chromadmx.agent.scene.Scene
import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.BlendMode
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3
import com.chromadmx.engine.effect.EffectLayer
import com.chromadmx.engine.effect.EffectRegistry
import com.chromadmx.engine.effect.EffectStack
import com.chromadmx.engine.pipeline.EffectEngine
import com.chromadmx.engine.preset.PresetLibrary
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
    val effectRegistry: EffectRegistry,
    private val presetLibrary: PresetLibrary,
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
     * Update a fixture's position in the UI model.
     * Note: Does not propagate to EffectEngine -- engine integration
     * requires support for mutable fixture lists (future work).
     */
    fun updateFixturePosition(index: Int, newPosition: Vec3) {
        val current = _fixtures.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(position = newPosition)
            _fixtures.value = current
        }
    }

    /**
     * Add a fixture to the UI model.
     * Note: Does not propagate to EffectEngine -- engine integration
     * requires support for mutable fixture lists (future work).
     */
    fun addFixture(fixture: Fixture3D) {
        _fixtures.value = _fixtures.value + fixture
    }

    /**
     * Remove a fixture from the UI model by index.
     * Note: Does not propagate to EffectEngine -- engine integration
     * requires support for mutable fixture lists (future work).
     */
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
