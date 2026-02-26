package com.chromadmx.ui.screen.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.components.PixelBottomSheet
import com.chromadmx.ui.components.PixelCard
import com.chromadmx.ui.components.PixelIconButton
import com.chromadmx.ui.components.PixelTextField
import com.chromadmx.ui.state.ChatMessage
import com.chromadmx.ui.state.MascotAnimState
import com.chromadmx.ui.state.MascotEvent
import com.chromadmx.ui.theme.PixelDesign
import com.chromadmx.ui.viewmodel.MascotViewModelV2

/**
 * Pixel-art styled chat panel for the mascot AI assistant.
 *
 * Rendered as a [PixelBottomSheet] that expands to ~75% of the screen height.
 * Contains a heading, scrollable message list with [ChatMessageBubble]s, a
 * [ThinkingBubble] indicator when the agent is processing, and an input row
 * with a [PixelTextField] and send button.
 *
 * @param viewModel The UDF mascot ViewModel providing consolidated state and event dispatch.
 * @param modifier  Optional [Modifier] forwarded to the [PixelBottomSheet].
 */
@Composable
fun ChatPanel(
    viewModel: MascotViewModelV2,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(state.chatHistory.size) {
        if (state.chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(state.chatHistory.size - 1)
        }
    }

    PixelBottomSheet(
        visible = state.isChatOpen,
        onDismiss = { viewModel.onEvent(MascotEvent.ToggleChat) },
        modifier = modifier,
    ) {
        // Sheet content at ~75% height
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f),
        ) {
            // Header
            Text(
                "Chat with Chroma",
                style = MaterialTheme.typography.headlineSmall,
                color = PixelDesign.colors.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            // Message list
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.chatHistory) { message ->
                    ChatMessageBubble(message)
                }
                // Thinking indicator
                if (state.animState == MascotAnimState.THINKING) {
                    item { ThinkingBubble() }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Input row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PixelTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = "Ask Chroma...",
                    singleLine = true,
                )
                PixelIconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.onEvent(MascotEvent.SendChatMessage(inputText.trim()))
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && state.animState != MascotAnimState.THINKING,
                ) {
                    Text("Send", color = PixelDesign.colors.primary)
                }
            }
        }
    }
}

/**
 * A PixelCard styled message bubble.
 *
 * User messages are right-aligned with a primary-tinted background;
 * assistant messages are left-aligned with the default surface color.
 */
@Composable
private fun ChatMessageBubble(message: ChatMessage) {
    val isUser = message.isFromUser
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (isUser) {
        PixelDesign.colors.primary.copy(alpha = 0.15f)
    } else {
        PixelDesign.colors.surface
    }
    val borderColor = if (isUser) PixelDesign.colors.primary else PixelDesign.colors.outline

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        PixelCard(
            backgroundColor = bgColor,
            borderColor = borderColor,
            modifier = Modifier.fillMaxWidth(0.8f),
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = PixelDesign.colors.onSurface,
            )
        }
    }
}

/**
 * Animated thinking indicator shown when the agent is processing a request.
 */
@Composable
private fun ThinkingBubble() {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        PixelCard(modifier = Modifier.fillMaxWidth(0.4f)) {
            Text(
                "thinking...",
                style = MaterialTheme.typography.bodySmall,
                color = PixelDesign.colors.onSurfaceVariant,
            )
        }
    }
}
