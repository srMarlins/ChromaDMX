package com.chromadmx.engine.preset

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.*
import com.chromadmx.engine.effects.*

/**
 * The six universal presets that ship with ChromaDMX.
 *
 * Each preset is marked [ScenePreset.isBuiltIn] = true and uses a deterministic
 * id prefixed with `builtin_`. The [EffectLayerConfig.effectId] values correspond
 * to the companion-object IDs on the concrete [SpatialEffect] implementations
 * registered in [EffectRegistry].
 *
 * The list is cached as a top-level val so it is only created once.
 */
private val allBuiltInPresets: List<ScenePreset> = listOf(
    neonPulse(),
    sunsetSweep(),
    strobeStorm(),
    oceanWaves(),
    fireAndIce(),
    midnightRainbow()
)

fun builtInPresets(): List<ScenePreset> = allBuiltInPresets

// ── Helper ─────────────────────────────────────────────────────────────────

/**
 * Factory that stamps every built-in preset with a deterministic id prefix,
 * [ScenePreset.isBuiltIn] = true, and [ScenePreset.createdAt] = 0.
 */
private fun createBuiltInPreset(
    id: String,
    name: String,
    genre: Genre,
    layers: List<EffectLayerConfig>,
    masterDimmer: Float = 1.0f,
    thumbnailColors: List<Color>
) = ScenePreset(
    id = "builtin_$id",
    name = name,
    genre = genre,
    layers = layers,
    masterDimmer = masterDimmer,
    isBuiltIn = true,
    createdAt = 0L,
    thumbnailColors = thumbnailColors
)

// ── 1. Neon Pulse ──────────────────────────────────────────────────────────

private fun neonPulse() = createBuiltInPreset(
    id = "neon_pulse",
    name = "Neon Pulse",
    genre = Genre.TECHNO,
    layers = listOf(
        EffectLayerConfig(
            effectId = RadialPulse3DEffect.ID,
            params = EffectParams()
                .with("color", Color(0f, 1f, 1f))   // cyan
                .with("speed", 2.5f)
                .with("width", 0.4f),
            blendMode = BlendMode.NORMAL,
            opacity = 1.0f
        ),
        EffectLayerConfig(
            effectId = RadialPulse3DEffect.ID,
            params = EffectParams()
                .with("color", Color(1f, 0f, 1f))   // magenta
                .with("speed", 1.8f)
                .with("width", 0.35f)
                .with("centerX", 0.5f),
            blendMode = BlendMode.ADDITIVE,
            opacity = 0.7f
        )
    ),
    thumbnailColors = listOf(
        Color(0f, 1f, 1f),     // cyan
        Color(1f, 0f, 1f),     // magenta
        Color(0.5f, 0f, 0.5f)  // dark purple
    )
)

// ── 2. Sunset Sweep ────────────────────────────────────────────────────────

private fun sunsetSweep() = createBuiltInPreset(
    id = "sunset_sweep",
    name = "Sunset Sweep",
    genre = Genre.AMBIENT,
    layers = listOf(
        EffectLayerConfig(
            effectId = GradientSweep3DEffect.ID,
            params = EffectParams()
                .with("axis", "x")
                .with("speed", 0.3f)
                .with(
                    "palette", listOf(
                        Color(1.0f, 0.6f, 0f),   // amber
                        Color(1.0f, 0.15f, 0f),  // red-orange
                        Color(0.6f, 0f, 0.4f),   // purple
                        Color(1.0f, 0.6f, 0f)    // amber (wrap)
                    )
                ),
            blendMode = BlendMode.NORMAL,
            opacity = 1.0f
        )
    ),
    masterDimmer = 0.9f,
    thumbnailColors = listOf(
        Color(1.0f, 0.6f, 0f),
        Color(1.0f, 0.15f, 0f),
        Color(0.6f, 0f, 0.4f)
    )
)

// ── 3. Strobe Storm ────────────────────────────────────────────────────────

private fun strobeStorm() = createBuiltInPreset(
    id = "strobe_storm",
    name = "Strobe Storm",
    genre = Genre.DNB,
    layers = listOf(
        EffectLayerConfig(
            effectId = RainbowSweep3DEffect.ID,
            params = EffectParams()
                .with("axis", "x")
                .with("speed", 3.0f)
                .with("spread", 0.5f),
            blendMode = BlendMode.NORMAL,
            opacity = 0.4f
        ),
        EffectLayerConfig(
            effectId = StrobeEffect.ID,
            params = EffectParams()
                .with("color", Color.WHITE)
                .with("dutyCycle", 0.15f),
            blendMode = BlendMode.ADDITIVE,
            opacity = 1.0f
        )
    ),
    thumbnailColors = listOf(
        Color.WHITE,
        Color(1f, 0f, 0f),
        Color(0f, 0f, 1f),
        Color(0f, 1f, 0f)
    )
)

// ── 4. Ocean Waves ─────────────────────────────────────────────────────────

private fun oceanWaves() = createBuiltInPreset(
    id = "ocean_waves",
    name = "Ocean Waves",
    genre = Genre.AMBIENT,
    layers = listOf(
        EffectLayerConfig(
            effectId = WaveEffect3DEffect.ID,
            params = EffectParams()
                .with("axis", "x")
                .with("wavelength", 0.8f)
                .with("speed", 0.6f)
                .with(
                    "colors", listOf(
                        Color(0f, 0.1f, 0.4f),  // deep blue
                        Color(0f, 0.8f, 0.7f)   // teal
                    )
                ),
            blendMode = BlendMode.NORMAL,
            opacity = 1.0f
        ),
        EffectLayerConfig(
            effectId = PerlinNoise3DEffect.ID,
            params = EffectParams()
                .with("scale", 1.5f)
                .with("speed", 0.3f)
                .with(
                    "palette", listOf(
                        Color(0f, 0f, 0.15f),   // near-black blue
                        Color(0f, 0.5f, 0.6f),  // ocean mid-tone
                        Color(0f, 0.9f, 1.0f)   // bright aqua
                    )
                ),
            blendMode = BlendMode.OVERLAY,
            opacity = 0.5f
        )
    ),
    masterDimmer = 0.85f,
    thumbnailColors = listOf(
        Color(0f, 0.1f, 0.4f),
        Color(0f, 0.8f, 0.7f),
        Color(0f, 0.5f, 0.6f)
    )
)

// ── 5. Fire & Ice ──────────────────────────────────────────────────────────

private fun fireAndIce() = createBuiltInPreset(
    id = "fire_and_ice",
    name = "Fire & Ice",
    genre = Genre.HOUSE,
    layers = listOf(
        EffectLayerConfig(
            effectId = GradientSweep3DEffect.ID,
            params = EffectParams()
                .with("axis", "x")
                .with("speed", 0.8f)
                .with(
                    "palette", listOf(
                        Color(1.0f, 0.3f, 0f),  // warm orange
                        Color(1.0f, 0.05f, 0f), // hot red
                        Color(1.0f, 0.7f, 0f)   // fire yellow
                    )
                ),
            blendMode = BlendMode.NORMAL,
            opacity = 1.0f
        ),
        EffectLayerConfig(
            effectId = GradientSweep3DEffect.ID,
            params = EffectParams()
                .with("axis", "x")
                .with("speed", -0.6f) // opposing direction
                .with(
                    "palette", listOf(
                        Color(0f, 0.4f, 1.0f),  // cool blue
                        Color(0f, 0.8f, 1.0f),  // ice cyan
                        Color(0.2f, 0.1f, 0.6f) // deep indigo
                    )
                ),
            blendMode = BlendMode.ADDITIVE,
            opacity = 0.6f
        )
    ),
    thumbnailColors = listOf(
        Color(1.0f, 0.3f, 0f),
        Color(1.0f, 0.05f, 0f),
        Color(0f, 0.4f, 1.0f),
        Color(0f, 0.8f, 1.0f)
    )
)

// ── 6. Midnight Rainbow ────────────────────────────────────────────────────

private fun midnightRainbow() = createBuiltInPreset(
    id = "midnight_rainbow",
    name = "Midnight Rainbow",
    genre = Genre.CUSTOM,
    layers = listOf(
        EffectLayerConfig(
            effectId = SolidColorEffect.ID,
            params = EffectParams()
                .with("color", Color(0.02f, 0.02f, 0.05f)),  // near-black base
            blendMode = BlendMode.NORMAL,
            opacity = 1.0f
        ),
        EffectLayerConfig(
            effectId = RainbowSweep3DEffect.ID,
            params = EffectParams()
                .with("axis", "x")
                .with("speed", 0.4f)
                .with("spread", 1.5f),
            blendMode = BlendMode.ADDITIVE,
            opacity = 0.3f
        )
    ),
    masterDimmer = 0.8f,
    thumbnailColors = listOf(
        Color(0.02f, 0.02f, 0.05f),
        Color(0.3f, 0f, 0f),
        Color(0f, 0.3f, 0f),
        Color(0f, 0f, 0.3f)
    )
)
