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
