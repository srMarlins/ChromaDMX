package com.chromadmx.agent

import com.chromadmx.agent.config.AgentConfig
import com.chromadmx.agent.controller.EngineController
import com.chromadmx.agent.controller.FixtureController
import com.chromadmx.agent.controller.NetworkController
import com.chromadmx.agent.controller.StateController
import com.chromadmx.agent.scene.SceneStore
import com.chromadmx.agent.tools.ToolRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The AI lighting director agent.
 *
 * Manages a conversation with an LLM, dispatching tool calls to the
 * [ToolRegistry] and tracking conversation history. When no API key
 * is configured, returns an "unavailable" message gracefully.
 *
 * The agent architecture is designed to plug into Koog when available:
 * - [ToolRegistry] maps tool names to execution logic
 * - [AgentSystemPrompt] provides the system prompt
 * - [ChatMessage] tracks conversation history
 *
 * For now, the LLM connection is a placeholder -- tool dispatch works
 * end-to-end via [dispatchTool] for direct programmatic use.
 */
class LightingAgent(
    private val config: AgentConfig,
    engineController: EngineController,
    networkController: NetworkController,
    fixtureController: FixtureController,
    stateController: StateController,
    sceneStore: SceneStore,
) {
    /** The tool registry for dispatching tool calls. */
    internal val toolRegistry = ToolRegistry(
        engineController = engineController,
        networkController = networkController,
        fixtureController = fixtureController,
        stateController = stateController,
        sceneStore = sceneStore
    )

    /** All registered tool names — public read-only view. */
    val toolNames: List<String> get() = toolRegistry.toolNames

    private val _conversationHistory = MutableStateFlow<List<ChatMessage>>(emptyList())

    /** Observable conversation history. */
    val conversationHistory: StateFlow<List<ChatMessage>> = _conversationHistory.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)

    /** Whether the agent is currently processing a request. */
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    /** Whether the agent has an API key and can make LLM requests. */
    val isAvailable: Boolean get() = config.isAvailable

    /**
     * Send a user message to the agent.
     *
     * When the agent is available (has an API key), this will eventually
     * connect to the LLM. For now, it returns a placeholder indicating
     * that the Koog integration is pending.
     *
     * @return The agent's response string.
     */
    suspend fun send(userMessage: String): String {
        if (!config.isAvailable) {
            return "Agent unavailable - no API key configured. Use tools directly or configure an Anthropic API key."
        }

        _isProcessing.value = true
        _conversationHistory.update { it + ChatMessage(role = ChatRole.USER, content = userMessage) }

        return try {
            // TODO: Wire Koog AIAgent here when SDK is available
            val response = "LLM integration pending (Koog SDK). " +
                "Use dispatchTool() for direct tool access. " +
                "Available tools: ${toolRegistry.toolNames.joinToString(", ")}"
            _conversationHistory.update { it + ChatMessage(role = ChatRole.ASSISTANT, content = response) }
            response
        } catch (e: Exception) {
            val error = "Error: ${e.message}"
            _conversationHistory.update { it + ChatMessage(role = ChatRole.ASSISTANT, content = error) }
            error
        } finally {
            _isProcessing.value = false
        }
    }

    /**
     * Dispatch a tool call directly (bypasses LLM).
     *
     * This is the primary interface for programmatic use and testing.
     *
     * @param toolName The tool to execute.
     * @param argsJson JSON string of tool arguments.
     * @return The tool's response string.
     */
    suspend fun dispatchTool(toolName: String, argsJson: String = "{}"): String {
        return toolRegistry.dispatch(toolName, argsJson)
    }

    /** Clear the conversation history. */
    fun clearHistory() {
        _conversationHistory.value = emptyList()
    }
}
