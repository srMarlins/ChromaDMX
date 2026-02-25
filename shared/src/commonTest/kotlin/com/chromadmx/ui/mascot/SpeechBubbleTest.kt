package com.chromadmx.ui.mascot

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpeechBubbleTest {
    @Test
    fun bubbleTypesExist() {
        assertEquals(3, BubbleType.entries.size)
        assertTrue(BubbleType.entries.contains(BubbleType.INFO))
        assertTrue(BubbleType.entries.contains(BubbleType.ACTION))
        assertTrue(BubbleType.entries.contains(BubbleType.ALERT))
    }

    @Test
    fun speechBubbleCreation() {
        val bubble = SpeechBubble(
            text = "No lights found -- want a virtual stage?",
            type = BubbleType.ACTION,
            actionLabel = "Yes!",
            autoDismissMs = 5000L
        )
        assertEquals("No lights found -- want a virtual stage?", bubble.text)
        assertEquals(BubbleType.ACTION, bubble.type)
        assertEquals("Yes!", bubble.actionLabel)
    }

    @Test
    fun infoBubbleHasNoAction() {
        val bubble = SpeechBubble(
            text = "Looking good!",
            type = BubbleType.INFO
        )
        assertEquals(null, bubble.actionLabel)
    }
}
