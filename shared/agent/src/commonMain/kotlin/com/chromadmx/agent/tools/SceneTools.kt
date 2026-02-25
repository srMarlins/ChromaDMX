package com.chromadmx.agent.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import com.chromadmx.agent.controller.EngineController
import com.chromadmx.engine.preset.PresetLibrary
import kotlinx.serialization.Serializable

class SetEffectTool(private val controller: EngineController) : SimpleTool<SetEffectTool.Args>(
    argsSerializer = Args.serializer(),
    name = "setEffect",
    description = "Apply a spatial lighting effect to a layer. Effects: solid_color, gradient_sweep_3d, rainbow_sweep_3d, strobe, chase_3d, wave_3d, radial_pulse_3d, perlin_noise_3d, particle_burst_3d."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Layer index (0-based) to apply the effect to")
        val layer: Int,
        @property:LLMDescription("Effect ID from the registry (e.g. solid_color, strobe, wave_3d)")
        val effectId: String,
        @property:LLMDescription("Optional effect parameters as name-value pairs (e.g. speed=2.0, intensity=0.8)")
        val params: Map<String, Float> = emptyMap()
    )

    override suspend fun execute(args: Args): String {
        val success = controller.setEffect(args.layer, args.effectId, args.params)
        if (!success) return "Effect '${args.effectId}' not found in registry."
        return "Applied effect '${args.effectId}' to layer ${args.layer}" +
            if (args.params.isNotEmpty()) " with params ${args.params}" else ""
    }
}

class SetBlendModeTool(private val controller: EngineController) : SimpleTool<SetBlendModeTool.Args>(
    argsSerializer = Args.serializer(),
    name = "setBlendMode",
    description = "Set the blend mode for a layer. Modes: NORMAL, ADDITIVE, MULTIPLY, OVERLAY, SCREEN."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Layer index (0-based)")
        val layer: Int,
        @property:LLMDescription("Blend mode: NORMAL, ADDITIVE, MULTIPLY, OVERLAY, or SCREEN")
        val mode: String
    )

    override suspend fun execute(args: Args): String {
        val upperMode = args.mode.uppercase()
        if (upperMode !in VALID_BLEND_MODES) {
            return "Error: invalid blend mode '${args.mode}'. Valid modes: ${VALID_BLEND_MODES.joinToString(", ")}"
        }
        controller.setBlendMode(args.layer, upperMode)
        return "Set blend mode for layer ${args.layer} to $upperMode"
    }

    companion object {
        val VALID_BLEND_MODES = listOf("NORMAL", "ADDITIVE", "MULTIPLY", "OVERLAY", "SCREEN")
    }
}

class SetMasterDimmerTool(private val controller: EngineController) : SimpleTool<SetMasterDimmerTool.Args>(
    argsSerializer = Args.serializer(),
    name = "setMasterDimmer",
    description = "Set the master dimmer level. 0.0 = blackout, 1.0 = full brightness."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Dimmer level from 0.0 (blackout) to 1.0 (full brightness)")
        val value: Float
    )

    override suspend fun execute(args: Args): String {
        val clamped = args.value.coerceIn(0f, 1f)
        controller.setMasterDimmer(clamped)
        return "Set master dimmer to $clamped"
    }
}

class SetColorPaletteTool(private val controller: EngineController) : SimpleTool<SetColorPaletteTool.Args>(
    argsSerializer = Args.serializer(),
    name = "setColorPalette",
    description = "Set the active color palette used by effects. Provide hex color strings."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("List of hex color strings (e.g. #FF0000, #00FF00, #0000FF)")
        val colors: List<String>
    )

    override suspend fun execute(args: Args): String {
        controller.setColorPalette(args.colors)
        return "Set color palette to ${args.colors.size} colors: ${args.colors.joinToString(", ")}"
    }
}

class SetTempoMultiplierTool(private val controller: EngineController) : SimpleTool<SetTempoMultiplierTool.Args>(
    argsSerializer = Args.serializer(),
    name = "setTempoMultiplier",
    description = "Set the tempo multiplier for beat-synced effects. 1.0 = normal speed, 2.0 = double speed, 0.5 = half speed. Clamped to 0.1-16.0."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Tempo multiplier (e.g. 0.5 for half speed, 2.0 for double)")
        val multiplier: Float
    )

    override suspend fun execute(args: Args): String {
        val clamped = args.multiplier.coerceIn(0.1f, 16.0f)
        controller.setTempoMultiplier(clamped)
        return "Set tempo multiplier to ${clamped}x"
    }
}

class CreateSceneTool(
    private val controller: EngineController,
    private val presetLibrary: PresetLibrary
) : SimpleTool<CreateSceneTool.Args>(
    argsSerializer = Args.serializer(),
    name = "createScene",
    description = "Capture the current engine state (effects, dimmer, palette, tempo) and save it as a named scene for later recall."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Name for the saved scene (e.g. 'dark_techno', 'warm_ambient')")
        val name: String
    )

    override suspend fun execute(args: Args): String {
        val preset = controller.capturePreset(args.name)
        presetLibrary.savePreset(preset)
        return "Created scene '${args.name}' with ${preset.layers.size} layers"
    }
}

class LoadSceneTool(
    private val controller: EngineController,
    private val presetLibrary: PresetLibrary
) : SimpleTool<LoadSceneTool.Args>(
    argsSerializer = Args.serializer(),
    name = "loadScene",
    description = "Load a previously saved scene by name and apply it to the engine."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Name of the scene to load")
        val name: String
    )

    override suspend fun execute(args: Args): String {
        val presets = presetLibrary.listPresets()
        val preset = presets.find { it.name.equals(args.name, ignoreCase = true) }
            ?: return "Scene '${args.name}' not found. Available: ${presets.joinToString(", ") { it.name }}"
        presetLibrary.loadPreset(preset.id)
        return "Loaded scene '${args.name}' with ${preset.layers.size} layers, dimmer=${preset.masterDimmer}"
    }
}
