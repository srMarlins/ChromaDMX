package com.chromadmx.agent.config

/**
 * Configuration for the AI lighting agent.
 *
 * @property apiKey    The Anthropic API key. When blank, the agent operates in offline mode.
 * @property modelId   Which Anthropic model to use. Options: "haiku_4_5", "sonnet_4", "sonnet_4_5", "opus_4", "opus_4_1", "opus_4_5".
 * @property maxIterations Maximum tool-calling iterations per request.
 * @property temperature LLM sampling temperature (0.0 = deterministic, 1.0 = creative).
 * @property historyCompressionThreshold Message count before triggering history compression.
 */
data class AgentConfig(
    val apiKey: String = "",
    val modelId: String = "sonnet_4_5",
    val maxIterations: Int = 30,
    val temperature: Float = 0.7f,
    val historyCompressionThreshold: Int = 50,
) {
    /** Whether the agent has a valid API key and can make LLM requests. */
    val isAvailable: Boolean get() = apiKey.isNotBlank()
}
