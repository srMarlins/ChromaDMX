package com.chromadmx.ui.screen.agent

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.chromadmx.ui.viewmodel.AgentViewModel

/**
 * Agent screen with a chat interface and pre-generation panel.
 *
 * Shows conversation history in a scrollable list, with a text input
 * and send button at the bottom. An "Agent unavailable" banner is shown
 * when no API key is configured.
 */
@Composable
fun AgentScreen(
    viewModel: AgentViewModel,
) {
    val messages by viewModel.messages.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val isAgentAvailable = viewModel.isAgentAvailable
    val preGenProgress by viewModel.preGenProgress.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var showPreGen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Agent unavailable banner
        if (!isAgentAvailable) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Text(
                    text = "No API key configured. Tool dispatch still works — try 'Pre-Generate' for offline scene creation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        // Pre-generation panel (toggle)
        if (showPreGen) {
            PreGenPanel(
                isGenerating = preGenProgress.isRunning,
                progress = if (preGenProgress.total > 0) {
                    preGenProgress.current.toFloat() / preGenProgress.total
                } else 0f,
                onGenerate = { genre, count ->
                    viewModel.generateScenes(genre, count)
                },
                onCancel = { viewModel.cancelGeneration() },
            )
        }

        // Chat history
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
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "ChromaDMX Agent",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Ask the AI to create lighting scenes, adjust effects, or generate presets.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            itemsIndexed(messages, key = { index, _ -> index }) { _, message ->
                ChatBubble(
                    message = message,
                    modifier = Modifier.padding(vertical = 4.dp),
                )

                // Show tool call cards for assistant messages
                message.toolCalls.forEach { toolCall ->
                    ToolCallCard(toolCall = toolCall)
                }
            }

            if (isProcessing) {
                item {
                    Text(
                        text = "Thinking...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }

        // Input area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Message the agent...") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                },
            ) {
                Text("Send")
            }
        }
    }
}
