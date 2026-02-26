package com.chromadmx.ui.screen.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chromadmx.agent.ChatMessage
import com.chromadmx.agent.ChatRole
import com.chromadmx.ui.components.pixelBorder
import com.chromadmx.ui.theme.PixelDesign

/**
 * Pixel-art styled chat message bubble.
 *
 * - **User** messages: right-aligned, blue/cyan pixel border.
 * - **Assistant** messages: left-aligned, green pixel border.
 * - **System** messages: centered, gray, smaller text.
 * - **Tool** messages: left-aligned, purple border (tool execution output).
 *
 * Uses [pixelBorder] for the retro look consistent with the design system.
 */
@Composable
fun PixelChatBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
) {
    when (message.role) {
        ChatRole.SYSTEM -> SystemBubble(message = message, modifier = modifier)
        ChatRole.USER -> UserBubble(message = message, modifier = modifier)
        ChatRole.ASSISTANT -> AssistantBubble(message = message, modifier = modifier)
        ChatRole.TOOL -> ToolBubble(message = message, modifier = modifier)
    }
}

@Composable
private fun UserBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        val colors = PixelDesign.colors
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .pixelBorder(color = colors.primary.copy(alpha = 0.8f), pixelSize = 3.dp)
                .background(colors.primary.copy(alpha = 0.15f))
                .padding(3.dp) // border padding
                .padding(12.dp),
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
            )
        }
    }
}

@Composable
private fun AssistantBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
    ) {
        val colors = PixelDesign.colors
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .pixelBorder(color = colors.success.copy(alpha = 0.8f), pixelSize = 3.dp)
                .background(colors.success.copy(alpha = 0.1f))
                .padding(3.dp)
                .padding(12.dp),
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
            )
        }
    }
}

@Composable
private fun SystemBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message.content,
            style = MaterialTheme.typography.bodySmall,
            color = PixelDesign.colors.onSurfaceDim,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = 240.dp)
                .padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun ToolBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
    ) {
        val colors = PixelDesign.colors
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .pixelBorder(color = colors.info.copy(alpha = 0.6f), pixelSize = 2.dp)
                .background(colors.info.copy(alpha = 0.08f))
                .padding(2.dp)
                .padding(8.dp),
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}
