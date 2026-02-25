package com.chromadmx.ui.screen.agent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * "Generate Scenes" UI panel: genre text input, count selector,
 * progress bar, and generate button.
 */
@Composable
fun PreGenPanel(
    isGenerating: Boolean,
    progress: Float,
    onGenerate: (genre: String, count: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var genre by remember { mutableStateOf("") }
    var selectedCount by remember { mutableStateOf(10) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = "Generate Scene Presets",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = genre,
                onValueChange = { genre = it },
                label = { Text("Genre / Style") },
                placeholder = { Text("e.g., EDM, Jazz, Rock") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(Modifier.height(12.dp))

            // Count selector
            Text(
                text = "Number of scenes",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                listOf(5, 10, 20).forEach { count ->
                    val isSelected = count == selectedCount
                    if (isSelected) {
                        FilledTonalButton(onClick = { }) {
                            Text("$count")
                        }
                    } else {
                        OutlinedButton(onClick = { selectedCount = count }) {
                            Text("$count")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (isGenerating) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surface,
                )
                Spacer(Modifier.height(8.dp))
            }

            FilledTonalButton(
                onClick = { onGenerate(genre, selectedCount) },
                enabled = genre.isNotBlank() && !isGenerating,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(if (isGenerating) "Generating..." else "Generate")
            }
        }
    }
}
