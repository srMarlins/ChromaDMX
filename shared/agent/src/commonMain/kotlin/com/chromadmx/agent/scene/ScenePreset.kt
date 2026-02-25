package com.chromadmx.agent.scene

import kotlinx.serialization.Serializable

/**
 * A named snapshot of the current effect stack state.
 *
 * ScenePresets capture all the parameters needed to reproduce a lighting look:
 * which effects are on which layers, their parameters, blend modes,
 * and the global master dimmer, color palette, and tempo multiplier.
 */
@Serializable
data class ScenePreset(
    val name: String,
    val layers: List<EffectLayerConfig> = emptyList(),
    val masterDimmer: Float = 1.0f,
    val colorPalette: List<String> = emptyList(),
    val tempoMultiplier: Float = 1.0f,
    val isBuiltIn: Boolean = false,
    val thumbnailColors: List<String> = emptyList(),
)

/**
 * Configuration for a single effect layer within a scene.
 */
@Serializable
data class EffectLayerConfig(
    val effectId: String = "",
    val params: Map<String, Float> = emptyMap(),
    val stringParams: Map<String, String> = emptyMap(),
    val colors: Map<String, String> = emptyMap(),
    val colorLists: Map<String, List<String>> = emptyMap(),
    val blendMode: String = "NORMAL",
    val opacity: Float = 1.0f,
    val enabled: Boolean = true,
)
