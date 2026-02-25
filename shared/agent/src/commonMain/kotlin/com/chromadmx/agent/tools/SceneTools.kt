package com.chromadmx.agent.tools

import com.chromadmx.agent.controller.EngineController
import com.chromadmx.agent.scene.SceneStore
import kotlinx.serialization.Serializable

/**
 * Tool: Apply a spatial effect to a layer with optional parameters.
 */
class SetEffectTool(private val controller: EngineController) {
    @Serializable
    data class Args(
        val layer: Int,
        val effectId: String,
        val params: Map<String, Float> = emptyMap()
    )

    fun execute(args: Args): String {
        val success = controller.setEffect(args.layer, args.effectId, args.params)
        if (!success) return "Effect '${args.effectId}' not found in registry."
        return "Applied effect '${args.effectId}' to layer ${args.layer}" +
            if (args.params.isNotEmpty()) " with params ${args.params}" else ""
    }
}

/**
 * Tool: Set the blend mode for a layer.
 */
class SetBlendModeTool(private val controller: EngineController) {
    @Serializable
    data class Args(val layer: Int, val mode: String)

    fun execute(args: Args): String {
        controller.setBlendMode(args.layer, args.mode)
        return "Set blend mode for layer ${args.layer} to ${args.mode}"
    }
}

/**
 * Tool: Set the master dimmer (0.0-1.0).
 */
class SetMasterDimmerTool(private val controller: EngineController) {
    @Serializable
    data class Args(val value: Float)

    fun execute(args: Args): String {
        val clamped = args.value.coerceIn(0f, 1f)
        controller.setMasterDimmer(clamped)
        return "Set master dimmer to $clamped"
    }
}

/**
 * Tool: Set the active color palette.
 */
class SetColorPaletteTool(private val controller: EngineController) {
    @Serializable
    data class Args(val colors: List<String>)

    fun execute(args: Args): String {
        controller.setColorPalette(args.colors)
        return "Set color palette to ${args.colors.size} colors: ${args.colors.joinToString(", ")}"
    }
}

/**
 * Tool: Set the tempo multiplier.
 */
class SetTempoMultiplierTool(private val controller: EngineController) {
    @Serializable
    data class Args(val multiplier: Float)

    fun execute(args: Args): String {
        controller.setTempoMultiplier(args.multiplier)
        return "Set tempo multiplier to ${args.multiplier}x"
    }
}

/**
 * Tool: Capture the current engine state and save it as a named scene.
 */
class CreateSceneTool(
    private val controller: EngineController,
    private val sceneStore: SceneStore
) {
    @Serializable
    data class Args(val name: String)

    fun execute(args: Args): String {
        val scene = controller.captureScene().copy(name = args.name)
        sceneStore.save(scene)
        return "Created scene '${args.name}' with ${scene.layers.size} layers"
    }
}

/**
 * Tool: Load a named scene and apply it to the engine.
 */
class LoadSceneTool(
    private val controller: EngineController,
    private val sceneStore: SceneStore
) {
    @Serializable
    data class Args(val name: String)

    fun execute(args: Args): String {
        val scene = sceneStore.load(args.name)
            ?: return "Scene '${args.name}' not found. Available: ${sceneStore.list().joinToString(", ")}"
        controller.applyScene(scene)
        return "Loaded scene '${args.name}' with ${scene.layers.size} layers, dimmer=${scene.masterDimmer}"
    }
}
