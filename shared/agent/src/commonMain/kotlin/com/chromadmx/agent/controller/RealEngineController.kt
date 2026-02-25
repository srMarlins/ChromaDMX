package com.chromadmx.agent.controller

import com.chromadmx.agent.scene.ScenePreset
import com.chromadmx.agent.scene.EffectLayerConfig
import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BlendMode
import com.chromadmx.core.model.Color
import com.chromadmx.engine.effect.EffectLayer
import com.chromadmx.engine.util.ColorUtils
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

    override fun captureScene(): ScenePreset {
        val layers = effectStack.layers.map { layer ->
            val paramsMap = layer.params.toMap()

            EffectLayerConfig(
                effectId = layer.effect.id,
                params = paramsMap
                    .filterValues { it is Number }
                    .mapValues { (_, v) -> (v as Number).toFloat() },
                stringParams = paramsMap
                    .filterValues { it is String }
                    .mapValues { (_, v) -> v as String },
                colors = paramsMap
                    .filterValues { it is Color }
                    .mapValues { (_, v) -> ColorUtils.toHex(v as Color) },
                colorLists = paramsMap
                    .filterValues { it is List<*> && it.firstOrNull() is Color }
                    .mapValues { (_, v) -> (v as List<Color>).map { ColorUtils.toHex(it) } },
                blendMode = layer.blendMode.name,
                opacity = layer.opacity,
                enabled = layer.enabled
            )
        }
        return ScenePreset(
            name = "",
            layers = layers,
            masterDimmer = effectStack.masterDimmer,
            colorPalette = _currentPalette.value,
            tempoMultiplier = _currentTempoMultiplier.value
        )
    }

    override fun applyScene(scene: ScenePreset) {
        // Build all layers first, then swap atomically to avoid
        // the engine seeing a partially-applied scene.
        val newLayers = scene.layers.mapNotNull { layerConfig ->
            val effect = effectRegistry.get(layerConfig.effectId) ?: return@mapNotNull null

            var params = layerConfig.params.entries.fold(EffectParams.EMPTY) { acc, (key, value) ->
                acc.with(key, value)
            }

            layerConfig.stringParams.forEach { (key, value) ->
                params = params.with(key, value)
            }

            layerConfig.colors.forEach { (key, value) ->
                params = params.with(key, ColorUtils.parseHex(value))
            }

            layerConfig.colorLists.forEach { (key, value) ->
                params = params.with(key, value.map { ColorUtils.parseHex(it) })
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
                enabled = layerConfig.enabled
            )
        }
        effectStack.replaceLayers(newLayers)

        effectStack.masterDimmer = scene.masterDimmer
        _currentPalette.value = scene.colorPalette
        _currentTempoMultiplier.value = scene.tempoMultiplier
    }
}
