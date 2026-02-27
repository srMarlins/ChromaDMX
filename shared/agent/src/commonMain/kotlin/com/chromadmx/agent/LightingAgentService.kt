package com.chromadmx.agent

import ai.koog.agents.core.agent.AIAgentService
import ai.koog.agents.core.agent.GraphAIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.agent.reActStrategy
import ai.koog.agents.features.eventHandler.feature.EventHandler
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleAnthropicExecutor
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.params.LLMParams
import com.chromadmx.agent.config.AgentConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeout

/**
 * Correct Koog lifecycle implementation of [LightingAgentInterface].
 *
 * Instead of reusing a single [AIAgent] instance (which is single-run),
 * this service uses [AIAgentService.createAgentAndRun] to create a **fresh
 * agent per message**. Conversation context from the [ConversationStore] is
 * injected into the system prompt so the agent sees prior exchanges.
 *
 * @param config Agent configuration (API key, model, temperature, etc.).
 * @param toolRegistry Registry of tools available to the agent.
 */
class LightingAgentService(
    private val config: AgentConfig,
    private val toolRegistry: ToolRegistry,
) : LightingAgentInterface {

    private val conversationStore = ConversationStore()

    override val conversationHistory: StateFlow<List<ChatMessage>>
        get() = conversationStore.messages

    private val _isProcessing = MutableStateFlow(false)
    override val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _toolCallsInFlight = MutableStateFlow<List<ToolCallRecord>>(emptyList())
    val toolCallsInFlight: StateFlow<List<ToolCallRecord>> = _toolCallsInFlight.asStateFlow()

    override val isAvailable: Boolean get() = config.isAvailable

    /**
     * The [AIAgentService] is created once and reused — it's a factory for
     * per-request agents, not a single-run agent itself.
     */
    private val agentService: AIAgentService<String, String, GraphAIAgent<String, String>> by lazy {
        val executor = if (config.isGoogleModel) {
            simpleGoogleAIExecutor(config.apiKey)
        } else {
            simpleAnthropicExecutor(config.apiKey)
        }

        AIAgentService(
            promptExecutor = executor,
            agentConfig = buildAgentConfig(),
            strategy = reActStrategy(reasoningInterval = 1),
            toolRegistry = toolRegistry,
        ) {
            install(EventHandler) {
                onToolCallStarting { event ->
                    val record = ToolCallRecord(
                        toolName = event.toolName,
                        arguments = event.toolArgs.toString()
                    )
                    _toolCallsInFlight.update { it + record }
                }
                onToolCallCompleted { event ->
                    _toolCallsInFlight.update { inflight ->
                        inflight.filterNot { it.toolName == event.toolName }
                    }
                    conversationStore.addSystemMessage(
                        "Tool ${event.toolName}: ${event.toolResult}"
                    )
                }
                onAgentExecutionFailed { event ->
                    conversationStore.addSystemMessage(
                        "Agent error: ${event.throwable.message}"
                    )
                    _toolCallsInFlight.value = emptyList()
                }
            }
        }
    }

    override suspend fun send(userMessage: String): String {
        if (!config.isAvailable) {
            return "Agent unavailable - no API key configured. Set GOOGLE_API_KEY or ANTHROPIC_API_KEY."
        }

        _isProcessing.value = true
        conversationStore.addUserMessage(userMessage)

        return try {
            val contextPrompt = buildContextualPrompt(userMessage)
            val response = withTimeout(TIMEOUT_MS) {
                agentService.createAgentAndRun(contextPrompt)
            }
            conversationStore.addAssistantMessage(response)
            response
        } catch (e: Exception) {
            val error = "Error: ${e.message}"
            conversationStore.addAssistantMessage(error)
            error
        } finally {
            _isProcessing.value = false
            _toolCallsInFlight.value = emptyList()
        }
    }

    fun clearHistory() {
        conversationStore.clear()
    }

    // ── Private helpers ──────────────────────────────────────────────

    private fun buildAgentConfig(): AIAgentConfig = AIAgentConfig(
        prompt = prompt(
            "lighting-agent",
            params = LLMParams(temperature = config.temperature.toDouble()),
        ) {
            system(AgentSystemPrompt.PROMPT)
        },
        model = resolveModel(),
        maxAgentIterations = config.maxIterations,
    )

    /**
     * Build the input for the agent, including recent conversation context.
     *
     * The agent sees the last N messages formatted as a preamble before the
     * current user message, giving it continuity without relying on a
     * persistent LLM session.
     */
    private fun buildContextualPrompt(userMessage: String): String {
        val recent = conversationStore.getRecent(CONTEXT_MESSAGE_COUNT)
        // Exclude the current user message (just added) from context
        val context = recent.dropLast(1)
        if (context.isEmpty()) return userMessage

        val contextBlock = context.joinToString("\n") { msg ->
            val prefix = when (msg.role) {
                ChatRole.USER -> "User"
                ChatRole.ASSISTANT -> "Assistant"
                ChatRole.SYSTEM -> "System"
                ChatRole.TOOL -> "Tool"
            }
            "$prefix: ${msg.content}"
        }

        return """
            |Previous conversation:
            |$contextBlock
            |
            |Current request: $userMessage
        """.trimMargin()
    }

    private fun resolveModel(): LLModel = when (config.modelId) {
        // Google Gemini
        "gemini_2_0_flash" -> GoogleModels.Gemini2_0Flash
        "gemini_2_5_flash" -> GoogleModels.Gemini2_5Flash
        "gemini_2_5_pro" -> GoogleModels.Gemini2_5Pro
        // Anthropic Claude
        "haiku_4_5" -> AnthropicModels.Haiku_4_5
        "sonnet_4" -> AnthropicModels.Sonnet_4
        "sonnet_4_5" -> AnthropicModels.Sonnet_4_5
        "opus_4" -> AnthropicModels.Opus_4
        "opus_4_1" -> AnthropicModels.Opus_4_1
        "opus_4_5" -> AnthropicModels.Opus_4_5
        else -> GoogleModels.Gemini2_5Flash
    }

    companion object {
        /** Per-request timeout in milliseconds. */
        const val TIMEOUT_MS = 15_000L

        /** Number of recent messages to include as context. */
        const val CONTEXT_MESSAGE_COUNT = 10
    }
}
