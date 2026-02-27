package com.chromadmx.agent

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Bounded, reactive conversation history store.
 *
 * Manages a rolling window of [ChatMessage]s, automatically evicting the oldest
 * messages when [maxMessages] is exceeded. Exposes history via a [StateFlow] so
 * UI layers can observe changes reactively.
 *
 * @param maxMessages Maximum number of messages to retain (default 50).
 */
class ConversationStore(
    private val maxMessages: Int = DEFAULT_MAX_MESSAGES,
) {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())

    /** Reactive stream of the current conversation history. */
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    /** Add a user message to the history. */
    fun addUserMessage(content: String) {
        append(ChatMessage(role = ChatRole.USER, content = content))
    }

    /** Add an assistant message to the history. */
    fun addAssistantMessage(content: String) {
        append(ChatMessage(role = ChatRole.ASSISTANT, content = content))
    }

    /** Add a system message to the history. */
    fun addSystemMessage(content: String) {
        append(ChatMessage(role = ChatRole.SYSTEM, content = content))
    }

    /**
     * Return the most recent [count] messages, preserving chronological order.
     */
    fun getRecent(count: Int = DEFAULT_RECENT_COUNT): List<ChatMessage> {
        val current = _messages.value
        return current.takeLast(count)
    }

    /** Clear all messages. */
    fun clear() {
        _messages.value = emptyList()
    }

    /** Current number of messages in the store. */
    val size: Int get() = _messages.value.size

    private fun append(message: ChatMessage) {
        _messages.update { current ->
            val updated = current + message
            if (updated.size > maxMessages) {
                updated.drop(updated.size - maxMessages)
            } else {
                updated
            }
        }
    }

    companion object {
        const val DEFAULT_MAX_MESSAGES = 50
        const val DEFAULT_RECENT_COUNT = 10
    }
}
