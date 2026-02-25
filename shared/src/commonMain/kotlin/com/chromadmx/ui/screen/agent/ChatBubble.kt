package com.chromadmx.ui.screen.agent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.viewmodel.ChatMessage
import com.chromadmx.ui.viewmodel.ChatRole

/**
 * Chat bubble for a single message in the agent conversation.
 *
 * User messages are right-aligned with the primary color.
 * Assistant messages are left-aligned with the surface color.
 */
@Composable
fun ChatBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
) {
    val isUser = message.role == ChatRole.USER

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp,
            ),
            color = if (isUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )

                // Show tool call badges if present
                if (message.toolCalls.isNotEmpty()) {
                    message.toolCalls.forEach { toolCall ->
                        ToolCallBadge(toolName = toolCall.toolName)
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolCallBadge(toolName: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.padding(top = 4.dp),
    ) {
        Text(
            text = toolName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
