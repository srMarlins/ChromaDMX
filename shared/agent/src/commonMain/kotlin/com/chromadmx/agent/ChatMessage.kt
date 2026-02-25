package com.chromadmx.agent

import kotlinx.serialization.Serializable

/**
 * A single message in the agent conversation history.
 *
 * @property role      "user", "assistant", or "system"
 * @property content   The text content of the message.
 * @property toolCalls Names of tools that were called (for assistant messages).
 */
@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
    val toolCalls: List<String> = emptyList()
)
