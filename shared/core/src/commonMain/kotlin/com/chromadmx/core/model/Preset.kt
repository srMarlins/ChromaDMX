package com.chromadmx.core.model

import com.chromadmx.core.EffectParams
import kotlinx.serialization.Serializable

/**
 * Musical genres for categorizing presets.
 */
@Serializable
enum class Genre { TECHNO, HOUSE, DNB, AMBIENT, HIPHOP, POP, ROCK, CUSTOM }

/**
 * Configuration for a single effect layer in a preset.
 * Maps directly to engine's EffectLayer but is serializable.
 */
@Serializable
data class EffectLayerConfig(
    val effectId: String,
    val params: EffectParams = EffectParams.EMPTY,
    val blendMode: BlendMode = BlendMode.NORMAL,
    val opacity: Float = 1.0f,
    val enabled: Boolean = true
)

/**
 * A full scene preset that can be saved to and loaded from disk.
 */
typealias Preset = ScenePreset

@Serializable
data class ScenePreset(
    val id: String,
    val name: String,
    val genre: Genre?,
    val layers: List<EffectLayerConfig>,
    val masterDimmer: Float,
    val isBuiltIn: Boolean = false,
    val createdAt: Long,
    val thumbnailColors: List<Color>
)
