package com.chromadmx.agent.controller

import com.chromadmx.agent.scene.Scene
import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.*
import com.chromadmx.engine.effect.EffectLayer
import com.chromadmx.engine.effect.EffectRegistry
import com.chromadmx.engine.effect.EffectStack
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * Real [EngineController] bridging to the effect engine.
 *
 * Translates agent tool calls into [EffectStack] and [EffectRegistry] operations.
 * Thread safety: [currentPalette] and [currentTempoMultiplier] use atomicfu,
 * and multi-step [EffectStack] operations are guarded by [lock].
 *
 * @param effectStack    The compositing effect stack.
 * @param effectRegistry Registry for looking up effects by ID.
 */
class RealEngineController(
    private val effectStack: EffectStack,
    private val effectRegistry: EffectRegistry,
) : EngineController {

    private val lock = SynchronizedObject()

    /** Current color palette (hex strings). Atomic for thread-safe reads/writes. */
    private val _currentPalette = atomic<List<String>>(emptyList())

    /** Current tempo multiplier. Atomic for thread-safe reads/writes. */
    private val _currentTempoMultiplier = atomic(1.0f)

    override fun setEffect(layer: Int, effectId: String, params: Map<String, Float>): Boolean {
        val effect = effectRegistry.get(effectId) ?: return false

        // Build EffectParams from the float map
        val effectParams = params.entries.fold(EffectParams.EMPTY) { acc, (key, value) ->
            acc.with(key, value)
        }

        // Synchronized: layerCount→addLayer→layers[layer]→setLayer must be atomic
        synchronized(lock) {
            // Ensure we have enough layers
            while (effectStack.layerCount <= layer) {
                val defaultEffect = effectRegistry.all().firstOrNull() ?: return false
                effectStack.addLayer(EffectLayer(effect = defaultEffect, enabled = false))
            }

            val existingLayer = effectStack.layers[layer]
            effectStack.setLayer(
                layer,
                existingLayer.copy(effect = effect, params = effectParams, enabled = true)
            )
        }
        return true
    }

    override fun setBlendMode(layer: Int, mode: String) {
        val blendMode = try {
            BlendMode.valueOf(mode)
        } catch (_: IllegalArgumentException) {
            BlendMode.NORMAL
        }
        // Synchronized: check-then-act on layerCount→layers[layer]→setLayer
        synchronized(lock) {
            if (layer >= effectStack.layerCount) return
            val existingLayer = effectStack.layers[layer]
            effectStack.setLayer(layer, existingLayer.copy(blendMode = blendMode))
        }
    }

    override fun setMasterDimmer(value: Float) {
        effectStack.masterDimmer = value
    }

    override fun setColorPalette(colors: List<String>) {
        _currentPalette.value = colors
    }

    override fun setTempoMultiplier(multiplier: Float) {
        _currentTempoMultiplier.value = multiplier
    }

    override fun captureScene(): Scene {
        val layers = effectStack.layers.map { layer ->
            Scene.LayerConfig(
                effectId = layer.effect.id,
                params = layer.params.toMap()
                    .filterValues { it is Number }
                    .mapValues { (_, v) -> (v as Number).toFloat() },
                blendMode = layer.blendMode.name,
                opacity = layer.opacity
            )
        }
        return Scene(
            name = "",
            layers = layers,
            masterDimmer = effectStack.masterDimmer,
            colorPalette = _currentPalette.value,
            tempoMultiplier = _currentTempoMultiplier.value
        )
    }

    override fun applyScene(scene: Scene) {
        // Build all layers first, then swap atomically to avoid
        // the engine seeing a partially-applied scene.
        val newLayers = scene.layers.mapNotNull { layerConfig ->
            val effect = effectRegistry.get(layerConfig.effectId) ?: return@mapNotNull null
            val params = layerConfig.params.entries.fold(EffectParams.EMPTY) { acc, (key, value) ->
                acc.with(key, value)
            }
            val blendMode = try {
                BlendMode.valueOf(layerConfig.blendMode)
            } catch (_: IllegalArgumentException) {
                BlendMode.NORMAL
            }
            EffectLayer(
                effect = effect,
                params = params,
                blendMode = blendMode,
                opacity = layerConfig.opacity,
                enabled = true
            )
        }
        effectStack.replaceLayers(newLayers)

        effectStack.masterDimmer = scene.masterDimmer
        _currentPalette.value = scene.colorPalette
        _currentTempoMultiplier.value = scene.tempoMultiplier
    }

    override fun capturePreset(name: String): ScenePreset {
        val layers = effectStack.layers.map { layer ->
            EffectLayerConfig(
                effectId = layer.effect.id,
                params = layer.params,
                blendMode = layer.blendMode,
                opacity = layer.opacity,
                enabled = layer.enabled
            )
        }
        val id = name.lowercase().replace(" ", "_") + "_" + (hashCode() % 1000)
        return ScenePreset(
            id = id,
            name = name,
            genre = Genre.CUSTOM,
            layers = layers,
            masterDimmer = effectStack.masterDimmer,
            createdAt = 0L, // Placeholder
            thumbnailColors = layers.mapNotNull { config ->
                config.params.getColor("color")
            }.take(4).ifEmpty { listOf(Color.WHITE) }
        )
    }

    override fun applyPreset(preset: ScenePreset) {
        val newLayers = preset.layers.mapNotNull { config ->
            val effect = effectRegistry.get(config.effectId) ?: return@mapNotNull null
            EffectLayer(
                effect = effect,
                params = config.params,
                blendMode = config.blendMode,
                opacity = config.opacity,
                enabled = config.enabled
            )
        }
        effectStack.replaceLayers(newLayers)
        effectStack.masterDimmer = preset.masterDimmer
    }
}
