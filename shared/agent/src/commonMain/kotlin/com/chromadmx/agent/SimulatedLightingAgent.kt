package com.chromadmx.agent

import com.chromadmx.agent.controller.EngineController
import com.chromadmx.agent.controller.StateController
import com.chromadmx.engine.effect.EffectRegistry
import com.chromadmx.engine.preset.PresetLibrary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Keyword-matching agent that executes REAL tool calls without an LLM.
 *
 * Makes the mascot chat functional on every device, even without an API key.
 * Parses user messages for keywords and dispatches real operations to the
 * engine controllers, preset library, and effect registry.
 *
 * @param engineController Controls effects, dimmer, palette, and presets.
 * @param stateController  Queries current engine/beat/network state.
 * @param presetLibrary    Lists and loads scene presets.
 * @param effectRegistry   Lists available effects.
 */
class SimulatedLightingAgent(
    private val engineController: EngineController,
    private val stateController: StateController,
    private val presetLibrary: PresetLibrary,
    private val effectRegistry: EffectRegistry,
) : LightingAgentInterface {

    private val conversationStore = ConversationStore()

    override val conversationHistory: StateFlow<List<ChatMessage>>
        get() = conversationStore.messages

    private val _isProcessing = MutableStateFlow(false)
    override val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    /** Always available — no API key required. */
    override val isAvailable: Boolean = true

    override suspend fun send(userMessage: String): String {
        _isProcessing.value = true
        conversationStore.addUserMessage(userMessage)

        return try {
            // Simulate a brief processing delay for UX
            delay(SIMULATED_DELAY_MS)

            val response = processMessage(userMessage)
            conversationStore.addAssistantMessage(response)
            response
        } catch (e: Exception) {
            val error = "Oops, something went wrong: ${e.message}"
            conversationStore.addSystemMessage(error)
            error
        } finally {
            _isProcessing.value = false
        }
    }

    /** Clear conversation history. */
    fun clearHistory() {
        conversationStore.clear()
    }

    // ── Command dispatch ────────────────────────────────────────────────

    /**
     * Parse the user message and dispatch to the appropriate handler.
     *
     * Keywords are matched case-insensitively. The first matching category
     * wins, checked in order: help, status, effects list, presets list,
     * dimmer, color, preset load.
     */
    internal fun processMessage(message: String): String {
        val lower = message.lowercase().trim()

        return when {
            // Queries (checked first so "help" doesn't match color/preset patterns)
            lower.containsAny("help", "commands", "what can you do") -> handleHelp()
            lower.containsAny("status", "what's happening", "state", "current") -> handleStatus()
            lower.containsAny("list effect", "available effect", "show effect", "what effect") -> handleListEffects()
            lower.containsAny("list preset", "available preset", "show preset", "what preset") -> handleListPresets()

            // Dimmer commands
            lower.containsAny("dim", "dimmer", "brightness", "blackout", "full bright") -> handleDimmer(lower)

            // Color commands
            lower.containsAny("blue", "red", "green", "white", "warm", "amber", "cyan", "magenta", "yellow", "purple", "orange", "pink") -> handleColor(lower)

            // Preset commands
            lower.containsAny("party", "strobe") -> handlePresetLoad("builtin_strobe_storm", "Strobe Storm")
            lower.containsAny("chill", "ambient", "relax", "sunset") -> handlePresetLoad("builtin_sunset_sweep", "Sunset Sweep")
            lower.containsAny("ocean", "wave", "sea") -> handlePresetLoad("builtin_ocean_waves", "Ocean Waves")
            lower.containsAny("rainbow") -> handlePresetLoad("builtin_midnight_rainbow", "Midnight Rainbow")
            lower.containsAny("neon", "pulse", "techno") -> handlePresetLoad("builtin_neon_pulse", "Neon Pulse")
            lower.containsAny("fire", "ice") -> handlePresetLoad("builtin_fire_and_ice", "Fire & Ice")

            // Unknown
            else -> handleUnknown(message)
        }
    }

    // ── Query handlers ──────────────────────────────────────────────────

    private fun handleHelp(): String = """
        |Here's what I can do:
        |
        |COLORS: Say a color name (blue, red, green, white, warm, cyan, etc.) to set solid lighting.
        |DIMMER: "dim to 50%" or "blackout" or "full brightness" to control brightness.
        |PRESETS: "party", "chill", "ocean", "rainbow", "neon", or "fire and ice" to load a preset.
        |STATUS: "status" to see current engine state.
        |EFFECTS: "list effects" to see available effects.
        |PRESETS: "list presets" to see saved presets.
    """.trimMargin()

    private fun handleStatus(): String {
        val engine = stateController.getEngineState()
        val beat = stateController.getBeatState()
        val network = stateController.getNetworkState()

        val engineStatus = if (engine.isRunning) "running" else "stopped"
        val effectList = engine.effectIds.joinToString(", ").ifEmpty { "none" }
        val outputStatus = if (network.isOutputActive) "active" else "inactive"
        val beatStatus = if (beat.isRunning) "${beat.bpm} BPM" else "stopped"

        return "Engine: $engineStatus | Dimmer: ${formatPercent(engine.masterDimmer)} | " +
            "Effects: $effectList | Fixtures: ${engine.fixtureCount} | " +
            "Beat: $beatStatus | Network: ${network.nodeCount} nodes, output $outputStatus"
    }

    private fun handleListEffects(): String {
        val ids = effectRegistry.ids()
        return if (ids.isEmpty()) {
            "No effects registered."
        } else {
            "Available effects (${ids.size}): ${ids.sorted().joinToString(", ")}"
        }
    }

    private fun handleListPresets(): String {
        val presets = presetLibrary.listPresets()
        return if (presets.isEmpty()) {
            "No presets saved."
        } else {
            "Available presets (${presets.size}): ${presets.joinToString(", ") { it.name }}"
        }
    }

    // ── Action handlers ─────────────────────────────────────────────────

    private fun handleDimmer(lower: String): String {
        val value = when {
            lower.contains("blackout") -> 0f
            lower.contains("full bright") || lower.contains("full on") -> 1f
            else -> {
                // Try to parse percentage: "dim to 50%", "50%", "dim 75", etc.
                val percentMatch = PERCENT_REGEX.find(lower)
                if (percentMatch != null) {
                    percentMatch.groupValues[1].toFloat() / 100f
                } else {
                    // Just "dim" with no number — set to 50%
                    0.5f
                }
            }
        }.coerceIn(0f, 1f)

        engineController.setMasterDimmer(value)
        return "Set master dimmer to ${formatPercent(value)}."
    }

    private fun handleColor(lower: String): String {
        val (hex, name) = when {
            lower.contains("blue") -> "#0000FF" to "blue"
            lower.contains("red") -> "#FF0000" to "red"
            lower.contains("green") -> "#00FF00" to "green"
            lower.contains("white") -> "#FFFFFF" to "white"
            lower.containsAny("warm", "amber") -> "#FFBF00" to "warm amber"
            lower.contains("cyan") -> "#00FFFF" to "cyan"
            lower.contains("magenta") -> "#FF00FF" to "magenta"
            lower.contains("yellow") -> "#FFFF00" to "yellow"
            lower.contains("purple") -> "#8000FF" to "purple"
            lower.contains("orange") -> "#FF8000" to "orange"
            lower.contains("pink") -> "#FF69B4" to "pink"
            else -> "#FFFFFF" to "white"
        }

        // Set solid color effect on layer 0 + update palette
        engineController.setEffect(0, SOLID_COLOR_EFFECT_ID, mapOf("r" to hexR(hex), "g" to hexG(hex), "b" to hexB(hex)))
        engineController.setColorPalette(listOf(hex))
        return "Set solid $name ($hex) on layer 0."
    }

    private fun handlePresetLoad(presetId: String, displayName: String): String {
        val preset = presetLibrary.getPreset(presetId)
        return if (preset != null) {
            engineController.applyPreset(preset)
            "Loaded preset \"$displayName\" with ${preset.layers.size} layers at ${formatPercent(preset.masterDimmer)} dimmer."
        } else {
            "Preset \"$displayName\" ($presetId) not found. Try \"list presets\" to see what's available."
        }
    }

    private fun handleUnknown(original: String): String =
        "I didn't understand \"$original\". Say \"help\" to see what I can do!"

    // ── Utility ─────────────────────────────────────────────────────────

    private fun formatPercent(value: Float): String = "${(value * 100).toInt()}%"

    /** Extension: does this string contain any of the given keywords? */
    private fun String.containsAny(vararg keywords: String): Boolean =
        keywords.any { this.contains(it) }

    companion object {
        /** Simulated processing delay to make the UI feel responsive. */
        const val SIMULATED_DELAY_MS = 300L

        /** Effect ID for the solid color effect. */
        const val SOLID_COLOR_EFFECT_ID = "solid-color"

        /** Regex for extracting percentages like "50%", "75 %", "50 percent". */
        private val PERCENT_REGEX = Regex("""(\d+)\s*%?""")

        // ── Hex color component helpers ─────────────────────────────────
        private fun hexR(hex: String): Float = hex.substring(1, 3).toInt(16) / 255f
        private fun hexG(hex: String): Float = hex.substring(3, 5).toInt(16) / 255f
        private fun hexB(hex: String): Float = hex.substring(5, 7).toInt(16) / 255f
    }
}
