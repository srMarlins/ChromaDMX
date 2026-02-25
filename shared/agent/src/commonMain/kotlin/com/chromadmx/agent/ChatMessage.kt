package com.chromadmx.agent

import kotlinx.serialization.Serializable

@Serializable
enum class ChatRole {
    USER,
    ASSISTANT,
    SYSTEM,
    TOOL
}

/**
 * A single message in the agent conversation history.
 */
@Serializable
data class ChatMessage(
    val role: ChatRole,
    val content: String,
    val toolCalls: List<ToolCallRecord> = emptyList()
)

/**
 * Record of a tool call made by the agent.
 */
@Serializable
data class ToolCallRecord(
    val toolName: String,
    val arguments: String = "",
    val result: String = "",
)
