package com.chromadmx.agent

import kotlinx.serialization.Serializable

/**
 * Role of a participant in the agent conversation.
 */
@Serializable
enum class ChatRole {
    USER,
    ASSISTANT,
    SYSTEM
}

/**
 * A single message in the agent conversation history.
 *
 * @property role      The role of the message sender.
 * @property content   The text content of the message.
 * @property toolCalls Names of tools that were called (for assistant messages).
 */
@Serializable
data class ChatMessage(
    val role: ChatRole,
    val content: String,
    val toolCalls: List<String> = emptyList()
)
