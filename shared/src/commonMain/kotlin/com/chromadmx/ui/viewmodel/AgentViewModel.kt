package com.chromadmx.ui.viewmodel

import com.chromadmx.agent.LightingAgent
import com.chromadmx.agent.pregen.PreGenProgress
import com.chromadmx.agent.pregen.PreGenerationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.chromadmx.agent.ChatMessage as AgentChatMessage

/**
 * A single message in the agent chat UI.
 */
data class ChatMessage(
    val role: ChatRole,
    val content: String,
    val toolCalls: List<ToolCallInfo> = emptyList(),
)

enum class ChatRole { USER, ASSISTANT }

/**
 * Information about a tool call made by the agent.
 */
data class ToolCallInfo(
    val toolName: String,
    val parameters: String = "",
    val result: String = "",
)

/**
 * ViewModel for the Agent screen.
 *
 * Delegates to the real [LightingAgent] for conversation and tool dispatch,
 * and to [PreGenerationService] for batch scene generation.
 */
class AgentViewModel(
    private val agent: LightingAgent,
    private val preGenService: PreGenerationService,
    private val scope: CoroutineScope,
) {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    val isAgentAvailable: Boolean get() = agent.isAvailable

    val preGenProgress: StateFlow<PreGenProgress> = preGenService.progress

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(role = ChatRole.USER, content = text)
        _messages.value = _messages.value + userMessage

        _isProcessing.value = true
        scope.launch {
            try {
                val response = agent.send(text)
                _messages.value = _messages.value + ChatMessage(
                    role = ChatRole.ASSISTANT,
                    content = response,
                )
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage(
                    role = ChatRole.ASSISTANT,
                    content = "Error: ${e.message}",
                )
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun dispatchTool(toolName: String, argsJson: String = "{}") {
        _isProcessing.value = true
        scope.launch {
            try {
                val result = agent.dispatchTool(toolName, argsJson)
                _messages.value = _messages.value + ChatMessage(
                    role = ChatRole.ASSISTANT,
                    content = result,
                    toolCalls = listOf(ToolCallInfo(toolName = toolName, parameters = argsJson, result = result)),
                )
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage(
                    role = ChatRole.ASSISTANT,
                    content = "Tool error: ${e.message}",
                )
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun generateScenes(genre: String, count: Int) {
        scope.launch {
            preGenService.generate(genre, count)
        }
    }

    fun cancelGeneration() {
        preGenService.cancel()
    }

    fun clearHistory() {
        _messages.value = emptyList()
        agent.clearHistory()
    }
}
