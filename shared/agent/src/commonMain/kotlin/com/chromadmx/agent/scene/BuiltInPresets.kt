package com.chromadmx.agent.scene

/**
 * Definitions for the 6 built-in scene presets that ship with the app.
 * These are tuned for high visual impact across all rig sizes.
 */
object BuiltInPresets {
    val ALL = listOf(
        ScenePreset(
            name = "Neon Pulse",
            isBuiltIn = true,
            thumbnailColors = listOf("#00FFFF", "#FF00FF"),
            layers = listOf(
                EffectLayerConfig(
                    effectId = "radial-pulse-3d",
                    params = mapOf("speed" to 1.5f, "width" to 0.4f),
                    colorLists = mapOf("palette" to listOf("#00FFFF", "#FF00FF")),
                    blendMode = "ADDITIVE",
                    enabled = true
                )
            ),
            masterDimmer = 1.0f
        ),
        ScenePreset(
            name = "Sunset Sweep",
            isBuiltIn = true,
            thumbnailColors = listOf("#FFBF00", "#FF0000", "#800080"),
            layers = listOf(
                EffectLayerConfig(
                    effectId = "gradient-sweep-3d",
                    params = mapOf("speed" to 0.2f),
                    stringParams = mapOf("axis" to "x"),
                    colorLists = mapOf("palette" to listOf("#FFBF00", "#FF0000", "#800080")),
                    blendMode = "NORMAL",
                    enabled = true
                )
            ),
            masterDimmer = 0.8f
        ),
        ScenePreset(
            name = "Strobe Storm",
            isBuiltIn = true,
            thumbnailColors = listOf("#FFFFFF", "#FF0000", "#00FF00", "#0000FF"),
            layers = listOf(
                EffectLayerConfig(
                    effectId = "rainbow-sweep-3d",
                    params = mapOf("speed" to 1.0f, "spread" to 0.5f),
                    stringParams = mapOf("axis" to "x"),
                    blendMode = "NORMAL",
                    enabled = true
                ),
                EffectLayerConfig(
                    effectId = "strobe",
                    params = mapOf("dutyCycle" to 0.2f),
                    colors = mapOf("color" to "#FFFFFF"),
                    blendMode = "MULTIPLY",
                    enabled = true
                )
            ),
            masterDimmer = 1.0f
        ),
        ScenePreset(
            name = "Ocean Waves",
            isBuiltIn = true,
            thumbnailColors = listOf("#0000FF", "#00FFFF"),
            layers = listOf(
                EffectLayerConfig(
                    effectId = "wave-3d",
                    params = mapOf("speed" to 0.5f, "wavelength" to 2.0f),
                    stringParams = mapOf("axis" to "x"),
                    colorLists = mapOf("colors" to listOf("#000088", "#00FFFF")),
                    blendMode = "NORMAL",
                    enabled = true
                ),
                EffectLayerConfig(
                    effectId = "perlin-noise-3d",
                    params = mapOf("scale" to 0.3f, "speed" to 0.2f),
                    colorLists = mapOf("palette" to listOf("#000000", "#FFFFFF")),
                    blendMode = "OVERLAY",
                    opacity = 0.5f,
                    enabled = true
                )
            ),
            masterDimmer = 0.9f
        ),
        ScenePreset(
            name = "Fire & Ice",
            isBuiltIn = true,
            thumbnailColors = listOf("#FF4400", "#00FFFF"),
            layers = listOf(
                EffectLayerConfig(
                    effectId = "gradient-sweep-3d",
                    params = mapOf("speed" to 0.5f),
                    stringParams = mapOf("axis" to "x"),
                    colorLists = mapOf("palette" to listOf("#FF0000", "#FF8800")),
                    blendMode = "NORMAL",
                    enabled = true
                ),
                EffectLayerConfig(
                    effectId = "gradient-sweep-3d",
                    params = mapOf("speed" to -0.5f),
                    stringParams = mapOf("axis" to "x"),
                    colorLists = mapOf("palette" to listOf("#00FFFF", "#0000FF")),
                    blendMode = "ADDITIVE",
                    opacity = 0.6f,
                    enabled = true
                )
            ),
            masterDimmer = 1.0f
        ),
        ScenePreset(
            name = "Midnight Rainbow",
            isBuiltIn = true,
            thumbnailColors = listOf("#000000", "#FF00FF", "#00FFFF"),
            layers = listOf(
                EffectLayerConfig(
                    effectId = "solid-color",
                    colors = mapOf("color" to "#000000"),
                    blendMode = "NORMAL",
                    enabled = true
                ),
                EffectLayerConfig(
                    effectId = "rainbow-sweep-3d",
                    params = mapOf("speed" to 0.1f, "spread" to 0.2f),
                    stringParams = mapOf("axis" to "x"),
                    blendMode = "ADDITIVE",
                    opacity = 0.4f,
                    enabled = true
                )
            ),
            masterDimmer = 0.6f
        )
    )
}
