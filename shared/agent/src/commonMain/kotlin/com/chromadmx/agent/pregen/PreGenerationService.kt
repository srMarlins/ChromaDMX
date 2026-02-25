package com.chromadmx.agent.pregen

import com.chromadmx.agent.LightingAgent
import com.chromadmx.agent.scene.Scene
import com.chromadmx.agent.scene.SceneStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

/**
 * Progress state for batch scene generation.
 */
@Serializable
data class PreGenProgress(
    val current: Int = 0,
    val total: Int = 0,
    val isRunning: Boolean = false
)

/**
 * Service for batch-generating scenes for a given genre.
 *
 * Orchestrates repeated "create a scene for [genre]" requests, using the
 * agent's createScene tool to build and save scenes to the [SceneStore].
 *
 * When the full LLM pipeline is wired, this will send natural language
 * prompts to the agent. For now, it generates deterministic template
 * scenes based on genre presets.
 */
class PreGenerationService(
    private val agent: LightingAgent,
    private val sceneStore: SceneStore,
) {
    private val _progress = MutableStateFlow(PreGenProgress())
    val progress: StateFlow<PreGenProgress> = _progress.asStateFlow()

    @Volatile
    private var cancelled = false

    /**
     * Generate [count] scenes for the given [genre].
     *
     * Each scene is saved to the [SceneStore] and returned.
     *
     * @param genre The music genre to generate scenes for.
     * @param count Number of scenes to generate.
     * @return List of generated scenes.
     */
    suspend fun generate(genre: String, count: Int): List<Scene> {
        if (count <= 0) return emptyList()

        cancelled = false
        _progress.value = PreGenProgress(current = 0, total = count, isRunning = true)

        val scenes = mutableListOf<Scene>()

        for (i in 1..count) {
            if (cancelled) break

            val sceneName = "${genre}_scene_$i"
            val scene = generateSceneForGenre(genre, sceneName, i)
            sceneStore.save(scene)
            scenes.add(scene)

            _progress.value = PreGenProgress(current = i, total = count, isRunning = i < count && !cancelled)
        }

        _progress.value = _progress.value.copy(isRunning = false)
        return scenes
    }

    /**
     * Cancel an in-progress generation.
     */
    fun cancel() {
        cancelled = true
    }

    /**
     * Generate a single scene based on genre templates.
     *
     * When the LLM pipeline is connected, this will send a prompt like:
     * "Create a ${genre} lighting scene named $name with dramatic effects"
     * and parse the agent's tool calls into a Scene.
     *
     * For now, uses deterministic genre-based presets.
     */
    private fun generateSceneForGenre(genre: String, name: String, index: Int): Scene {
        val presets = genrePresets[genre.lowercase()] ?: genrePresets["default"]!!
        val preset = presets[index % presets.size]

        return Scene(
            name = name,
            layers = preset.layers,
            masterDimmer = preset.masterDimmer,
            colorPalette = preset.colorPalette,
            tempoMultiplier = preset.tempoMultiplier
        )
    }

    companion object {
        /** Built-in genre presets for offline scene generation. */
        private val genrePresets: Map<String, List<Scene>> = mapOf(
            "techno" to listOf(
                Scene(
                    name = "techno_template_1",
                    layers = listOf(
                        Scene.LayerConfig(effectId = "strobe", params = mapOf("speed" to 4.0f), blendMode = "NORMAL", opacity = 0.7f),
                        Scene.LayerConfig(effectId = "radial_pulse_3d", params = mapOf("speed" to 2.0f), blendMode = "ADDITIVE", opacity = 0.5f)
                    ),
                    masterDimmer = 0.9f,
                    colorPalette = listOf("#FF0000", "#000000", "#FF4400"),
                    tempoMultiplier = 1.0f
                ),
                Scene(
                    name = "techno_template_2",
                    layers = listOf(
                        Scene.LayerConfig(effectId = "chase_3d", params = mapOf("speed" to 3.0f), blendMode = "NORMAL", opacity = 0.8f),
                        Scene.LayerConfig(effectId = "perlin_noise_3d", params = mapOf("scale" to 0.5f), blendMode = "MULTIPLY", opacity = 0.4f)
                    ),
                    masterDimmer = 0.85f,
                    colorPalette = listOf("#0000FF", "#FF00FF", "#000088"),
                    tempoMultiplier = 2.0f
                )
            ),
            "ambient" to listOf(
                Scene(
                    name = "ambient_template_1",
                    layers = listOf(
                        Scene.LayerConfig(effectId = "gradient_sweep_3d", params = mapOf("speed" to 0.3f), blendMode = "NORMAL", opacity = 0.6f)
                    ),
                    masterDimmer = 0.4f,
                    colorPalette = listOf("#0044FF", "#004488", "#002244"),
                    tempoMultiplier = 0.25f
                ),
                Scene(
                    name = "ambient_template_2",
                    layers = listOf(
                        Scene.LayerConfig(effectId = "perlin_noise_3d", params = mapOf("scale" to 0.2f, "speed" to 0.1f), blendMode = "NORMAL", opacity = 0.5f)
                    ),
                    masterDimmer = 0.3f,
                    colorPalette = listOf("#220044", "#440088", "#110022"),
                    tempoMultiplier = 0.5f
                )
            ),
            "house" to listOf(
                Scene(
                    name = "house_template_1",
                    layers = listOf(
                        Scene.LayerConfig(effectId = "wave_3d", params = mapOf("speed" to 1.5f), blendMode = "NORMAL", opacity = 0.7f),
                        Scene.LayerConfig(effectId = "rainbow_sweep_3d", params = mapOf("speed" to 1.0f), blendMode = "ADDITIVE", opacity = 0.3f)
                    ),
                    masterDimmer = 0.75f,
                    colorPalette = listOf("#FF8800", "#FFAA00", "#FF6600"),
                    tempoMultiplier = 1.0f
                ),
                Scene(
                    name = "house_template_2",
                    layers = listOf(
                        Scene.LayerConfig(effectId = "radial_pulse_3d", params = mapOf("speed" to 1.0f), blendMode = "NORMAL", opacity = 0.6f)
                    ),
                    masterDimmer = 0.7f,
                    colorPalette = listOf("#FF0088", "#FF00FF", "#8800FF"),
                    tempoMultiplier = 1.0f
                )
            ),
            "default" to listOf(
                Scene(
                    name = "default_template_1",
                    layers = listOf(
                        Scene.LayerConfig(effectId = "solid_color", params = mapOf("r" to 1.0f, "g" to 1.0f, "b" to 1.0f), blendMode = "NORMAL", opacity = 1.0f)
                    ),
                    masterDimmer = 0.8f,
                    colorPalette = listOf("#FFFFFF"),
                    tempoMultiplier = 1.0f
                )
            )
        )
    }
}
