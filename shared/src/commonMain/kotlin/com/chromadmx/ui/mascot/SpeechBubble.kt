package com.chromadmx.ui.mascot

/**
 * Types of speech bubbles the mascot can show.
 */
enum class BubbleType {
    INFO,    // General tips and status
    ACTION,  // Actionable suggestion with button
    ALERT    // Urgent notification
}

/**
 * A speech bubble displayed by the mascot.
 *
 * @property text The message text.
 * @property type The bubble type (affects styling).
 * @property actionLabel Optional action button label (for ACTION type).
 * @property autoDismissMs Auto-dismiss after this many ms (0 = manual dismiss only).
 */
data class SpeechBubble(
    val text: String,
    val type: BubbleType = BubbleType.INFO,
    val actionLabel: String? = null,
    val autoDismissMs: Long = 4000L
)
