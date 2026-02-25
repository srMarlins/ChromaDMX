package com.chromadmx.android.ui.agent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Agent tab — AI chat interface, tool visualization.
 *
 * Currently a placeholder; will host the agent chat UI and tool-call
 * visualizations once the AI agent module is integrated.
 */
@Composable
fun AgentScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Agent",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Chat interface, tool visualization",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
