package com.chromadmx.agent.tools

import com.chromadmx.agent.controller.EngineController
import com.chromadmx.agent.scene.ScenePreset
import com.chromadmx.agent.scene.EffectLayerConfig

/**
 * Fake [EngineController] for testing tools without the real engine.
 *
 * Records the last values set by each method for assertion.
 */
class FakeEngineController : EngineController {
    var lastSetEffectId: String = ""
    var lastSetEffectLayer: Int = -1
    var lastSetEffectParams: Map<String, Float> = emptyMap()
    var lastMasterDimmer: Float = 1.0f
    var lastPalette: List<String> = emptyList()
    var lastTempoMultiplier: Float = 1.0f
    var lastBlendMode: String = "NORMAL"
    var lastBlendModeLayer: Int = -1
    var lastAppliedScene: ScenePreset? = null

    override fun setEffect(layer: Int, effectId: String, params: Map<String, Float>): Boolean {
        lastSetEffectId = effectId
        lastSetEffectLayer = layer
        lastSetEffectParams = params
        return true
    }

    override fun setBlendMode(layer: Int, mode: String) {
        lastBlendMode = mode
        lastBlendModeLayer = layer
    }

    override fun setMasterDimmer(value: Float) {
        lastMasterDimmer = value.coerceIn(0f, 1f)
    }

    override fun setColorPalette(colors: List<String>) {
        lastPalette = colors
    }

    override fun setTempoMultiplier(multiplier: Float) {
        lastTempoMultiplier = multiplier
    }

    override fun captureScene(): ScenePreset = ScenePreset(
        name = "capture",
        masterDimmer = lastMasterDimmer,
        colorPalette = lastPalette,
        tempoMultiplier = lastTempoMultiplier
    )

    override fun applyScene(scene: ScenePreset) {
        lastAppliedScene = scene
        lastMasterDimmer = scene.masterDimmer
        lastPalette = scene.colorPalette
        lastTempoMultiplier = scene.tempoMultiplier
    }
}
