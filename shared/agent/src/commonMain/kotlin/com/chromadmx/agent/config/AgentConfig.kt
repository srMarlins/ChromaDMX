package com.chromadmx.agent.config

/**
 * Configuration for the AI lighting agent.
 *
 * @property apiKey    The Anthropic API key. When blank, the agent operates in offline mode.
 * @property maxIterations Maximum tool-calling iterations per request.
 * @property temperature LLM sampling temperature (0.0 = deterministic, 1.0 = creative).
 */
data class AgentConfig(
    val apiKey: String = "",
    val maxIterations: Int = 30,
    val temperature: Float = 0.7f,
) {
    /** Whether the agent has a valid API key and can make LLM requests. */
    val isAvailable: Boolean get() = apiKey.isNotBlank()
}
