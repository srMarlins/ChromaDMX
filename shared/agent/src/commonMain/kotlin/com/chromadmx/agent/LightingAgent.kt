package com.chromadmx.agent

import ai.koog.agents.core.agent.AIAgent
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
import kotlinx.serialization.json.Json

/**
 * The AI lighting director agent.
 *
 * When an API key is configured, wraps a Koog [AIAgent] with a ReAct strategy
 * that autonomously reasons and calls tools. When offline, tools are still
 * accessible via [dispatchTool] for direct programmatic use.
 */
class LightingAgent(
    private val config: AgentConfig,
    val toolRegistry: ToolRegistry,
) {
    private val _conversationHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val conversationHistory: StateFlow<List<ChatMessage>> = _conversationHistory.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _toolCallsInFlight = MutableStateFlow<List<ToolCallRecord>>(emptyList())
    val toolCallsInFlight: StateFlow<List<ToolCallRecord>> = _toolCallsInFlight.asStateFlow()

    val isAvailable: Boolean get() = config.isAvailable

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

    private val koogAgent: AIAgent<String, String>? by lazy {
        if (!config.isAvailable) return@lazy null

        val executor = if (config.isGoogleModel) {
            simpleGoogleAIExecutor(config.apiKey)
        } else {
            simpleAnthropicExecutor(config.apiKey)
        }
        val model = resolveModel()

        AIAgent(
            promptExecutor = executor,
            strategy = reActStrategy(reasoningInterval = 1),
            agentConfig = AIAgentConfig(
                prompt = prompt(
                    "lighting-agent",
                    params = LLMParams(temperature = config.temperature.toDouble()),
                ) {
                    system(AgentSystemPrompt.PROMPT)
                },
                model = model,
                maxAgentIterations = config.maxIterations,
            ),
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
                    _conversationHistory.update {
                        it + ChatMessage(
                            role = ChatRole.TOOL,
                            content = event.toolResult.toString(),
                            toolCalls = listOf(
                                ToolCallRecord(
                                    toolName = event.toolName,
                                    result = event.toolResult.toString()
                                )
                            )
                        )
                    }
                }
                onAgentExecutionFailed { event ->
                    _conversationHistory.update {
                        it + ChatMessage(
                            role = ChatRole.SYSTEM,
                            content = "Agent error: ${event.throwable.message}"
                        )
                    }
                    _toolCallsInFlight.value = emptyList()
                }
            }
        }
    }

    suspend fun send(userMessage: String): String {
        if (!config.isAvailable) {
            return "Agent unavailable - no API key configured. Set GOOGLE_API_KEY or ANTHROPIC_API_KEY."
        }

        _isProcessing.value = true
        _conversationHistory.update { it + ChatMessage(role = ChatRole.USER, content = userMessage) }

        return try {
            val response = koogAgent!!.run(userMessage)
            _conversationHistory.update { it + ChatMessage(role = ChatRole.ASSISTANT, content = response) }
            response
        } catch (e: Exception) {
            val error = "Error: ${e.message}"
            _conversationHistory.update { it + ChatMessage(role = ChatRole.ASSISTANT, content = error) }
            error
        } finally {
            _isProcessing.value = false
            _toolCallsInFlight.value = emptyList()
        }
    }

    /**
     * Dispatch a tool call directly (bypasses LLM).
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun dispatchTool(toolName: String, argsJson: String = "{}"): String {
        val tool = toolRegistry.tools.find { it.name == toolName }
            ?: return "Unknown tool: '$toolName'. Available: ${toolNames.joinToString(", ")}"
        return try {
            val jsonDecoder = Json { ignoreUnknownKeys = true }
            val args = jsonDecoder.decodeFromString(
                tool.argsSerializer as kotlinx.serialization.KSerializer<Any>,
                argsJson.ifBlank { "{}" }
            )
            val result = (tool as ai.koog.agents.core.tools.Tool<Any, Any>).execute(args)
            tool.encodeResultToString(result)
        } catch (e: Exception) {
            "Error executing tool '$toolName': ${e.message}"
        }
    }

    val toolNames: List<String> get() = toolRegistry.tools.map { it.name }

    fun clearHistory() {
        _conversationHistory.value = emptyList()
    }
}
