package com.chromadmx.agent.scene

import kotlinx.serialization.Serializable

/**
 * A named snapshot of the current effect stack state.
 *
 * Scenes capture all the parameters needed to reproduce a lighting look:
 * which effects are on which layers, their parameters, blend modes,
 * and the global master dimmer, color palette, and tempo multiplier.
 */
@Serializable
data class Scene(
    val name: String,
    val layers: List<LayerConfig> = emptyList(),
    val masterDimmer: Float = 1.0f,
    val colorPalette: List<String> = emptyList(),
    val tempoMultiplier: Float = 1.0f,
) {
    /**
     * Configuration for a single effect layer within a scene.
     */
    @Serializable
    data class LayerConfig(
        val effectId: String = "",
        val params: Map<String, Float> = emptyMap(),
        val blendMode: String = "NORMAL",
        val opacity: Float = 1.0f,
    )
}
