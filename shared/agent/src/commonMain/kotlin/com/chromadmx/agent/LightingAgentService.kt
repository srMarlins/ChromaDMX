package com.chromadmx.agent

import ai.koog.agents.core.agent.AIAgentService
import ai.koog.agents.core.agent.GraphAIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteMultipleTools
import ai.koog.agents.core.dsl.extension.nodeLLMRequestMultiple
import ai.koog.agents.core.dsl.extension.nodeLLMSendMultipleToolResults
import ai.koog.agents.core.dsl.extension.onMultipleAssistantMessages
import ai.koog.agents.core.dsl.extension.onMultipleToolCalls
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

/**
 * LLM-powered agent using Koog's graph strategy with explicit tool execution nodes.
 *
 * Uses a custom graph strategy with nodeLLMRequestMultiple -> nodeExecuteMultipleTools ->
 * nodeLLMSendMultipleToolResults, following the pattern from Koog's official examples.
 *
 * Key design decisions:
 * - Uses [AIAgentService] with a custom graph strategy (not reActStrategy)
 * - Fresh agent created per [send] call (correct Koog lifecycle -- agents are single-run)
 * - Conversation context injected into system prompt for continuity
 * - Parallel tool execution enabled for faster multi-tool responses
 *
 * @param config Agent configuration (API key, model, temperature, etc.).
 * @param toolRegistry Registry of tools available to the agent.
 */
class LightingAgentService(
    private val config: AgentConfig,
    private val toolRegistry: ToolRegistry,
) : LightingAgentInterface {

    private val conversationStore = ConversationStore()
    private val sendMutex = Mutex()

    override val conversationHistory: StateFlow<List<ChatMessage>>
        get() = conversationStore.messages

    private val _isProcessing = MutableStateFlow(false)
    override val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _toolCallsInFlight = MutableStateFlow<List<ToolCallRecord>>(emptyList())
    val toolCallsInFlight: StateFlow<List<ToolCallRecord>> = _toolCallsInFlight.asStateFlow()

    override val isAvailable: Boolean get() = config.isAvailable

    /**
     * The [AIAgentService] is created once and reused -- it's a factory for
     * per-request agents, not a single-run agent itself.
     */
    private val agentService by lazy {
        val executor = if (config.isGoogleModel) {
            simpleGoogleAIExecutor(config.apiKey)
        } else {
            simpleAnthropicExecutor(config.apiKey)
        }

        val lightingStrategy = strategy<String, String>("lighting-strategy") {
            val nodeRequestLLM by nodeLLMRequestMultiple()
            val nodeExecuteTools by nodeExecuteMultipleTools(parallelTools = true)
            val nodeSendToolResults by nodeLLMSendMultipleToolResults()

            edge(nodeStart forwardTo nodeRequestLLM)

            // LLM responded with tool calls -> execute them
            edge(
                nodeRequestLLM forwardTo nodeExecuteTools
                    onMultipleToolCalls { true }
            )

            // LLM responded with assistant message -> finish
            edge(
                nodeRequestLLM forwardTo nodeFinish
                    onMultipleAssistantMessages { true }
                    transformed { it.first().content }
            )

            // Tool results -> send back to LLM
            edge(nodeExecuteTools forwardTo nodeSendToolResults)

            // After tool results, LLM may call more tools
            edge(
                nodeSendToolResults forwardTo nodeExecuteTools
                    onMultipleToolCalls { true }
            )

            // After tool results, LLM may respond with final message
            edge(
                nodeSendToolResults forwardTo nodeFinish
                    onMultipleAssistantMessages { true }
                    transformed { it.first().content }
            )
        }

        AIAgentService(
            promptExecutor = executor,
            agentConfig = buildAgentConfig(),
            strategy = lightingStrategy,
            toolRegistry = toolRegistry,
        ) {
            handleEvents {
                onToolCallStarting { event ->
                    val record = ToolCallRecord(
                        toolName = event.toolName,
                        arguments = event.toolArgs.toString()
                    )
                    _toolCallsInFlight.update { it + record }
                }
                onToolCallCompleted { event ->
                    _toolCallsInFlight.update { inflight ->
                        val index = inflight.indexOfFirst { it.toolName == event.toolName }
                        if (index != -1) {
                            inflight.toMutableList().apply { removeAt(index) }
                        } else {
                            inflight
                        }
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
            return "Agent unavailable — no API key configured."
        }

        return sendMutex.withLock {
            _isProcessing.value = true

            try {
                conversationStore.addUserMessage(userMessage)
                val contextPrompt = buildContextualPrompt(userMessage)

                val response = withTimeout(TIMEOUT_MS) {
                    agentService.createAgentAndRun(contextPrompt)
                }

                val cleanResponse = response.ifBlank {
                    "I've completed the requested actions. Check the stage to see the changes!"
                }
                conversationStore.addAssistantMessage(cleanResponse)
                cleanResponse
            } catch (e: Exception) {
                val error = "Error: ${e.message}"
                conversationStore.addSystemMessage(error)
                error
            } finally {
                _isProcessing.value = false
                _toolCallsInFlight.value = emptyList()
            }
        }
    }

    internal fun clearHistory() {
        conversationStore.clear()
    }

    // -- Private helpers --

    private fun buildAgentConfig(): AIAgentConfig = AIAgentConfig(
        prompt = prompt(
            "lighting-agent",
            params = LLMParams(temperature = config.temperature.toDouble()),
        ) {
            system(AgentSystemPrompt.PROMPT)
        },
        model = resolveModel(),
        maxAgentIterations = MAX_ITERATIONS,
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
        val context = recent.dropLast(1) // exclude the just-added user message
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

        return buildString {
            appendLine("## Recent conversation context")
            appendLine(contextBlock)
            appendLine()
            appendLine("## Current user request")
            appendLine(userMessage)
        }
    }

    private fun resolveModel(): LLModel = when (config.modelId) {
        "gemini_2_0_flash" -> GoogleModels.Gemini2_0Flash
        "gemini_2_5_flash" -> GoogleModels.Gemini2_5Flash
        "gemini_2_5_pro" -> GoogleModels.Gemini2_5Pro
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
        const val TIMEOUT_MS = 45_000L

        /** Number of recent messages to include as context. */
        const val CONTEXT_MESSAGE_COUNT = 10

        /** Maximum tool-calling iterations per agent run. */
        const val MAX_ITERATIONS = 50
    }
}
