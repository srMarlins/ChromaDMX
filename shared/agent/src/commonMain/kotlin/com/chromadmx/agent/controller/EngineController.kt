package com.chromadmx.agent.controller

import com.chromadmx.agent.scene.Scene

/**
 * Abstraction over the effect engine for agent tool operations.
 *
 * Implementations bridge this interface to the real [EffectEngine] / [EffectStack]
 * or to a fake for testing.
 */
interface EngineController {
    /** Apply an effect to a specific layer with parameters. */
    fun setEffect(layer: Int, effectId: String, params: Map<String, Float>)

    /** Set the blend mode for a specific layer. */
    fun setBlendMode(layer: Int, mode: String)

    /** Set the master dimmer (0.0-1.0). */
    fun setMasterDimmer(value: Float)

    /** Set the active color palette as hex strings. */
    fun setColorPalette(colors: List<String>)

    /** Set the tempo multiplier. */
    fun setTempoMultiplier(multiplier: Float)

    /** Capture the current engine state as a [Scene]. */
    fun captureScene(): Scene

    /** Apply a saved [Scene] to the engine. */
    fun applyScene(scene: Scene)
}
