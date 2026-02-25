package com.chromadmx.ui.viewmodel

import com.chromadmx.core.model.ScenePreset
import com.chromadmx.engine.preset.PresetLibrary
import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.BlendMode
import com.chromadmx.core.model.Genre
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
    private val presetLibrary: PresetLibrary,
    private val beatClock: BeatClock,
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

    val allScenes: StateFlow<List<ScenePreset>> = _scenes.map { names ->
        names.mapNotNull { presetLibrary.getPreset(it) }
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
        // List presets, for now just IDs
        _scenes.value = presetLibrary.listPresets().map { it.id }
    }

    fun availableEffects(): Set<String> = effectRegistry.ids()

    fun availableGenres(): List<String> = Genre.values().map { it.name.lowercase() }

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
        if (presetLibrary.loadPreset(name)) {
            layersBeforePreview = null
            masterDimmerBeforePreview = null
            syncFromEngine()
        }
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
            // Store current state if not already stored
            if (layersBeforePreview == null) {
                layersBeforePreview = effectStack.layers
                masterDimmerBeforePreview = effectStack.masterDimmer
            }
            // Apply preset
            if (presetLibrary.loadPreset(name)) {
                syncFromEngine()
            }
        }
    }

    fun addLayer() {
        val ids = effectRegistry.ids()
        val firstEffect = ids.firstOrNull()?.let { effectRegistry.get(it) } ?: return
        effectStack.addLayer(EffectLayer(effect = firstEffect))
        syncFromEngine()
    }

    fun tap() {
        (beatClock as? TapTempoClock)?.tap()
    }
}
