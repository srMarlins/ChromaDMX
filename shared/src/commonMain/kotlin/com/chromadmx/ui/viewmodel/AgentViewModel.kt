package com.chromadmx.ui.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A single message in the agent chat.
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
 * Manages conversation history and agent interaction state.
 * The actual LightingAgent integration will be wired in when the agent
 * module (Issue #9) is completed. For now, provides a placeholder
 * interface with local echo.
 */
class AgentViewModel(
    private val scope: CoroutineScope,
) {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _isAgentAvailable = MutableStateFlow(false)
    val isAgentAvailable: StateFlow<Boolean> = _isAgentAvailable.asStateFlow()

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(role = ChatRole.USER, content = text)
        _messages.value = _messages.value + userMessage

        // Placeholder response until agent module is wired
        val response = ChatMessage(
            role = ChatRole.ASSISTANT,
            content = "Agent module not yet connected. Your message: \"$text\"",
        )
        _messages.value = _messages.value + response
    }

    fun clearHistory() {
        _messages.value = emptyList()
    }
}
