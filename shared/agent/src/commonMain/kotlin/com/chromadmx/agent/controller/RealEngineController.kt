package com.chromadmx.agent.controller

import com.chromadmx.agent.scene.Scene
import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BlendMode
import com.chromadmx.engine.effect.EffectLayer
import com.chromadmx.engine.effect.EffectRegistry
import com.chromadmx.engine.effect.EffectStack

/**
 * Real [EngineController] bridging to the effect engine.
 *
 * Translates agent tool calls into [EffectStack] and [EffectRegistry] operations.
 *
 * @param effectStack    The compositing effect stack.
 * @param effectRegistry Registry for looking up effects by ID.
 */
class RealEngineController(
    private val effectStack: EffectStack,
    private val effectRegistry: EffectRegistry,
) : EngineController {

    /** Current color palette (hex strings). */
    private var currentPalette: List<String> = emptyList()

    /** Current tempo multiplier. */
    private var currentTempoMultiplier: Float = 1.0f

    override fun setEffect(layer: Int, effectId: String, params: Map<String, Float>) {
        val effect = effectRegistry.get(effectId) ?: return

        // Build EffectParams from the float map
        val effectParams = params.entries.fold(EffectParams.EMPTY) { acc, (key, value) ->
            acc.with(key, value)
        }

        // Ensure we have enough layers
        while (effectStack.layerCount <= layer) {
            val defaultEffect = effectRegistry.all().firstOrNull() ?: return
            effectStack.addLayer(EffectLayer(effect = defaultEffect, enabled = false))
        }

        val existingLayer = effectStack.layers[layer]
        effectStack.setLayer(
            layer,
            existingLayer.copy(effect = effect, params = effectParams, enabled = true)
        )
    }

    override fun setBlendMode(layer: Int, mode: String) {
        if (layer >= effectStack.layerCount) return
        val blendMode = try {
            BlendMode.valueOf(mode)
        } catch (_: IllegalArgumentException) {
            BlendMode.NORMAL
        }
        val existingLayer = effectStack.layers[layer]
        effectStack.setLayer(layer, existingLayer.copy(blendMode = blendMode))
    }

    override fun setMasterDimmer(value: Float) {
        effectStack.masterDimmer = value
    }

    override fun setColorPalette(colors: List<String>) {
        currentPalette = colors
    }

    override fun setTempoMultiplier(multiplier: Float) {
        currentTempoMultiplier = multiplier
    }

    override fun captureScene(): Scene {
        val layers = effectStack.layers.map { layer ->
            Scene.LayerConfig(
                effectId = layer.effect.id,
                params = layer.params.toMap().mapValues { (_, v) ->
                    when (v) {
                        is Float -> v
                        is Double -> v.toFloat()
                        is Int -> v.toFloat()
                        is Number -> v.toFloat()
                        else -> 0f
                    }
                },
                blendMode = layer.blendMode.name,
                opacity = layer.opacity
            )
        }
        return Scene(
            name = "",
            layers = layers,
            masterDimmer = effectStack.masterDimmer,
            colorPalette = currentPalette,
            tempoMultiplier = currentTempoMultiplier
        )
    }

    override fun applyScene(scene: Scene) {
        effectStack.clearLayers()

        for (layerConfig in scene.layers) {
            val effect = effectRegistry.get(layerConfig.effectId) ?: continue
            val params = layerConfig.params.entries.fold(EffectParams.EMPTY) { acc, (key, value) ->
                acc.with(key, value)
            }
            val blendMode = try {
                BlendMode.valueOf(layerConfig.blendMode)
            } catch (_: IllegalArgumentException) {
                BlendMode.NORMAL
            }
            effectStack.addLayer(
                EffectLayer(
                    effect = effect,
                    params = params,
                    blendMode = blendMode,
                    opacity = layerConfig.opacity,
                    enabled = true
                )
            )
        }

        effectStack.masterDimmer = scene.masterDimmer
        currentPalette = scene.colorPalette
        currentTempoMultiplier = scene.tempoMultiplier
    }
}
