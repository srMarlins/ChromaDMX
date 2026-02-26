package com.chromadmx.ui.state

import androidx.compose.runtime.Immutable
import com.chromadmx.ui.mascot.BubbleType

/**
 * Mascot animation states.
 */
enum class MascotAnimState {
    IDLE,
    THINKING,
    HAPPY,
    ALERT,
    CONFUSED,
    DANCING
}

/**
 * A message in the mascot chat.
 */
@Immutable
data class ChatMessage(
    val id: String,
    val text: String,
    val isFromUser: Boolean,
    val timestampMs: Long
)

/**
 * Speech bubble shown above the mascot.
 */
@Immutable
data class SpeechBubble(
    val text: String,
    val type: BubbleType = BubbleType.INFO,
    val autoDismissMs: Long = 5000L,
    val actionLabel: String? = null,
    val actionId: String? = null
)

/**
 * UI State for the Mascot.
 */
@Immutable
data class MascotUiState(
    val animState: MascotAnimState = MascotAnimState.IDLE,
    val currentFrameIndex: Int = 0,
    val currentBubble: SpeechBubble? = null,
    val isChatOpen: Boolean = false,
    val chatHistory: List<ChatMessage> = emptyList()
)

/**
 * Events for the Mascot.
 */
sealed interface MascotEvent {
    data class ShowBubble(val bubble: SpeechBubble) : MascotEvent
    data object DismissBubble : MascotEvent
    data class OnBubbleAction(val actionId: String?) : MascotEvent
    data object TriggerHappy : MascotEvent
    data class TriggerAlert(val message: String) : MascotEvent
    data object TriggerThinking : MascotEvent
    data class TriggerConfused(val message: String) : MascotEvent
    data object TriggerDancing : MascotEvent
    data object ReturnToIdle : MascotEvent
    data object ToggleChat : MascotEvent
    data class SendChatMessage(val text: String) : MascotEvent
}
