package com.chromadmx.ui.screen.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.chromadmx.agent.ChatRole
import com.chromadmx.ui.components.PixelButton
import com.chromadmx.ui.components.PixelProgressBar
import com.chromadmx.ui.components.PixelSlider
import com.chromadmx.ui.components.pixelBorder
import com.chromadmx.ui.theme.DmxPrimary
import com.chromadmx.ui.theme.NeonMagenta
import com.chromadmx.ui.viewmodel.AgentViewModel

/** Available genre options for the pre-generation panel. */
private val genres = listOf("Techno", "House", "DnB", "Ambient", "Hip-Hop", "Pop", "Rock")

/**
 * Slide-up chat panel overlay for the pixel mascot AI assistant.
 *
 * This composable renders:
 * 1. A semi-transparent scrim behind the panel (tap to dismiss).
 * 2. An [AnimatedVisibility] panel sliding up from the bottom.
 * 3. Inside the panel: drag handle, quick actions, message history,
 *    thinking indicator, and a text input row.
 *
 * The panel height starts at half the screen. The drag handle allows
 * pulling the panel to full screen or dismissing by swiping down.
 * Conversation state is preserved across open/close cycles because
 * [AgentViewModel] is resolved once and remembered by the caller.
 *
 * @param isOpen Whether the panel is currently visible.
 * @param agentViewModel The agent ViewModel providing chat state.
 * @param onDismiss Callback to close the panel.
 */
@Composable
fun ChatPanel(
    isOpen: Boolean,
    agentViewModel: AgentViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Scrim + panel container
    Box(modifier = modifier.fillMaxSize()) {
        // Scrim: semi-transparent background, tap to dismiss
        AnimatedVisibility(
            visible = isOpen,
            enter = slideInVertically(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                initialOffsetY = { 0 },
            ),
            exit = slideOutVertically(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                targetOffsetY = { 0 },
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
        }

        // Panel slides up from bottom
        AnimatedVisibility(
            visible = isOpen,
            enter = slideInVertically(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                initialOffsetY = { it }, // starts off-screen at bottom
            ),
            exit = slideOutVertically(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                targetOffsetY = { it }, // exits to bottom
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            ChatPanelContent(
                agentViewModel = agentViewModel,
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun ChatPanelContent(
    agentViewModel: AgentViewModel,
    onDismiss: () -> Unit,
) {
    val messages by agentViewModel.messages.collectAsState()
    val isProcessing by agentViewModel.isProcessing.collectAsState()
    val toolCallsInFlight by agentViewModel.toolCallsInFlight.collectAsState()
    val preGenProgress by agentViewModel.preGenProgress.collectAsState()
    val isAgentAvailable = agentViewModel.isAgentAvailable

    var inputText by remember { mutableStateOf("") }
    var showPreGen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    // Track panel height for drag-to-expand/dismiss
    var panelHeightPx by remember { mutableStateOf(0f) }
    var expandedFraction by remember { mutableStateOf(0.55f) } // half screen default

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(expandedFraction)
            .onSizeChanged { panelHeightPx = it.height.toFloat() }
            .pixelBorder(
                color = DmxPrimary.copy(alpha = 0.6f),
                pixelSize = 3.dp,
            )
            .background(MaterialTheme.colorScheme.surface)
            .padding(3.dp) // border inset
            .pointerInput(Unit) {
                // Drag handle: swipe down to dismiss, up to expand
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        val delta = dragAmount / (panelHeightPx.coerceAtLeast(1f) * 2f)
                        expandedFraction = (expandedFraction - delta).coerceIn(0.3f, 1f)
                    },
                    onDragEnd = {
                        // Snap to half, full, or dismiss
                        when {
                            expandedFraction < 0.35f -> onDismiss()
                            expandedFraction < 0.7f -> expandedFraction = 0.55f
                            else -> expandedFraction = 1f
                        }
                    },
                )
            },
    ) {
        // ── Drag handle bar ───────────────────────────────────────
        DragHandle()

        // ── Quick action buttons ──────────────────────────────────
        QuickActionBar(
            onGenerateScenes = { showPreGen = !showPreGen },
            onDiagnoseNetwork = {
                agentViewModel.sendMessage("Diagnose my network — check node health and connectivity.")
            },
            onSuggestEffects = {
                agentViewModel.sendMessage("Suggest effects for the current vibe.")
            },
        )

        // ── Inline pre-generation panel ───────────────────────────
        AnimatedVisibility(visible = showPreGen) {
            InlinePreGenPanel(
                isGenerating = preGenProgress.isRunning,
                progress = if (preGenProgress.total > 0) {
                    preGenProgress.current.toFloat() / preGenProgress.total
                } else {
                    0f
                },
                onGenerate = { genre, count ->
                    agentViewModel.generateScenes(genre, count)
                },
                onCancel = { agentViewModel.cancelGeneration() },
            )
        }

        // ── Agent unavailable banner ──────────────────────────────
        if (!isAgentAvailable) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f))
                    .padding(8.dp),
            ) {
                Text(
                    text = "No API key. Pre-Generate still works for offline scenes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }

        // ── Chat message history ──────────────────────────────────
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            state = listState,
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "ChromaDMX Agent",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Ask me to create lighting scenes, adjust effects, or generate presets.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            itemsIndexed(messages, key = { index, _ -> index }) { _, message ->
                PixelChatBubble(
                    message = message,
                    modifier = Modifier.padding(vertical = 4.dp),
                )

                // Show tool call cards for assistant messages
                if (message.role == ChatRole.ASSISTANT) {
                    message.toolCalls.forEach { toolCall ->
                        val isRunning = toolCallsInFlight.any { it.toolName == toolCall.toolName }
                        PixelToolCallCard(
                            toolCall = toolCall,
                            isRunning = isRunning,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
            }

            // Thinking indicator
            if (isProcessing) {
                item {
                    ThinkingIndicator()
                }
            }
        }

        // ── Input row ─────────────────────────────────────────────
        ChatInputRow(
            inputText = inputText,
            onInputChange = { inputText = it },
            onSend = {
                if (inputText.isNotBlank()) {
                    agentViewModel.sendMessage(inputText)
                    inputText = ""
                }
            },
            isProcessing = isProcessing,
        )
    }
}

/**
 * Pixel-art drag handle at the top of the panel.
 */
@Composable
private fun DragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(4.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)),
        )
    }
}

/**
 * Text input row with a send button.
 */
@Composable
private fun ChatInputRow(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    isProcessing: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Input field with pixel border
        Box(
            modifier = Modifier
                .weight(1f)
                .pixelBorder(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    pixelSize = 2.dp,
                )
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(2.dp),
        ) {
            androidx.compose.material3.OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                placeholder = {
                    Text(
                        text = "Type a message...",
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = DmxPrimary,
                ),
            )
        }

        Spacer(Modifier.width(8.dp))

        PixelButton(
            onClick = onSend,
            backgroundColor = if (inputText.isNotBlank() && !isProcessing) {
                DmxPrimary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (inputText.isNotBlank() && !isProcessing) {
                Color.Black
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            borderColor = if (inputText.isNotBlank() && !isProcessing) {
                DmxPrimary
            } else {
                MaterialTheme.colorScheme.outline
            },
        ) {
            Text("Send")
        }
    }
}

/**
 * A single genre chip rendered as a [PixelButton].
 *
 * Extracted to file-level to avoid creating a new function class on
 * each recomposition of the parent composable.
 */
@Composable
private fun GenreChip(
    genre: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    PixelButton(
        onClick = onClick,
        backgroundColor = if (isSelected) {
            NeonMagenta.copy(alpha = 0.6f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = if (isSelected) {
            Color.White
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        borderColor = if (isSelected) NeonMagenta else Color.DarkGray,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 8.dp,
            vertical = 4.dp,
        ),
    ) {
        Text(
            text = genre,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

/**
 * Inline pre-generation panel for the chat.
 *
 * Genre chips, count slider, generate button, and progress bar.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InlinePreGenPanel(
    isGenerating: Boolean,
    progress: Float,
    onGenerate: (genre: String, count: Int) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedGenre by remember { mutableStateOf("") }
    var sceneCount by remember { mutableStateOf(10f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .pixelBorder(
                color = NeonMagenta.copy(alpha = 0.4f),
                pixelSize = 2.dp,
            )
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(2.dp)
            .padding(12.dp),
    ) {
        Text(
            text = "Generate Scene Presets",
            style = MaterialTheme.typography.titleSmall,
            color = NeonMagenta,
        )
        Spacer(Modifier.height(8.dp))

        // Genre chips — responsive wrapping layout
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            genres.forEach { genre ->
                GenreChip(
                    genre = genre,
                    isSelected = genre == selectedGenre,
                    onClick = { selectedGenre = genre },
                )
            }
        }

        // Count slider
        Text(
            text = "Scenes: ${sceneCount.toInt()}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PixelSlider(
            value = sceneCount,
            onValueChange = { sceneCount = it },
            valueRange = 5f..20f,
            accentColor = NeonMagenta,
            modifier = Modifier.padding(vertical = 4.dp),
        )

        Spacer(Modifier.height(8.dp))

        // Progress bar during generation
        if (isGenerating) {
            PixelProgressBar(
                progress = progress,
                progressColor = NeonMagenta,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        // Generate / Cancel buttons
        val isEnabled = selectedGenre.isNotBlank() && !isGenerating

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            if (isGenerating) {
                PixelButton(
                    onClick = onCancel,
                    backgroundColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    borderColor = MaterialTheme.colorScheme.error,
                ) {
                    Text("Cancel")
                }
                Spacer(Modifier.width(8.dp))
            }

            PixelButton(
                onClick = {
                    if (isEnabled) {
                        onGenerate(selectedGenre, sceneCount.toInt())
                    }
                },
                backgroundColor = if (isEnabled) {
                    NeonMagenta.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (isEnabled) {
                    Color.White
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                borderColor = if (isEnabled) {
                    NeonMagenta
                } else {
                    Color.DarkGray
                },
            ) {
                Text(if (isGenerating) "Generating..." else "Generate")
            }
        }
    }
}
