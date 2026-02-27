package com.chromadmx.ui.screen.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.chromadmx.agent.ChatMessage as AgentChatMessage
import com.chromadmx.agent.ChatRole
import com.chromadmx.ui.components.PixelBottomSheet
import com.chromadmx.ui.components.PixelButton
import com.chromadmx.ui.components.PixelButtonVariant
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
 * Uses [PixelChatBubble] for message rendering and [PixelButton] for the send action.
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f),
        ) {
            // Header
            Text(
                "CHAT WITH CHROMA",
                style = MaterialTheme.typography.titleMedium,
                color = PixelDesign.colors.primary,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            // Quick actions
            QuickActionBar(
                onGenerateScenes = {
                    viewModel.onEvent(MascotEvent.SendChatMessage("Generate some scene presets for tonight"))
                },
                onDiagnoseNetwork = {
                    viewModel.onEvent(MascotEvent.SendChatMessage("Diagnose the network connection"))
                },
                onSuggestEffects = {
                    viewModel.onEvent(MascotEvent.SendChatMessage("Suggest some effects for the current setup"))
                },
            )

            Spacer(Modifier.height(4.dp))

            // Message list
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(state.chatHistory, key = { it.id }) { message ->
                    PixelChatBubble(message = message.toAgentChatMessage())
                }
                // Thinking indicator
                if (state.animState == MascotAnimState.THINKING) {
                    item(key = "thinking") { ThinkingBubble() }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Input row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
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
                PixelButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.onEvent(MascotEvent.SendChatMessage(inputText.trim()))
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && state.animState != MascotAnimState.THINKING,
                    variant = PixelButtonVariant.Primary,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text("Send")
                }
            }
        }
    }
}

/**
 * Animated thinking indicator shown when the agent is processing a request.
 */
@Composable
private fun ThinkingBubble() {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        PixelChatBubble(
            message = AgentChatMessage(
                role = ChatRole.SYSTEM,
                content = "thinking...",
            )
        )
    }
}

/**
 * Bridge from UI ChatMessage to agent ChatMessage for PixelChatBubble rendering.
 */
private fun ChatMessage.toAgentChatMessage(): AgentChatMessage = AgentChatMessage(
    role = if (isFromUser) ChatRole.USER else ChatRole.ASSISTANT,
    content = text,
)
