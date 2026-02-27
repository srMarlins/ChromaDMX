package com.chromadmx.agent

import kotlinx.coroutines.flow.StateFlow

/**
 * Contract for interacting with the AI lighting agent.
 *
 * Implementations include:
 * - [LightingAgent] — legacy single-instance agent (deprecated lifecycle)
 * - [LightingAgentService] — correct per-message lifecycle via [AIAgentService]
 *
 * Consumers (e.g. MascotViewModelV2) depend on this interface, not concrete classes.
 */
interface LightingAgentInterface {
    /** Reactive stream of all conversation messages. */
    val conversationHistory: StateFlow<List<ChatMessage>>

    /** Whether the agent is currently processing a request. */
    val isProcessing: StateFlow<Boolean>

    /** Whether the agent has a valid API key and can make LLM requests. */
    val isAvailable: Boolean

    /**
     * Send a user message to the agent and return the assistant's response.
     *
     * @param userMessage The user's natural-language instruction.
     * @return The agent's response text, or an error message string.
     */
    suspend fun send(userMessage: String): String
}
