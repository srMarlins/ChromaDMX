package com.chromadmx.ui.screen.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.chromadmx.agent.ToolCallRecord
import com.chromadmx.ui.components.pixelBorder
import com.chromadmx.ui.theme.NeonGreen
import com.chromadmx.ui.theme.NeonPurple

/**
 * Collapsible pixel-art tool call visualization card.
 *
 * Shows the tool name and execution status in a compact header.
 * When expanded, displays the tool arguments (JSON) and result.
 * Running tools show a pixel spinner animation in the header.
 *
 * @param toolCall The tool call record to display.
 * @param isRunning Whether this tool call is currently executing.
 */
@Composable
fun PixelToolCallCard(
    toolCall: ToolCallRecord,
    modifier: Modifier = Modifier,
    isRunning: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }

    val borderColor = if (isRunning) {
        NeonPurple.copy(alpha = 0.8f)
    } else {
        NeonPurple.copy(alpha = 0.4f)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .pixelBorder(color = borderColor, pixelSize = 2.dp)
            .background(NeonPurple.copy(alpha = 0.06f))
            .padding(2.dp) // border padding
            .clickable { expanded = !expanded },
    ) {
        // Header row: status indicator + tool name + expand/collapse
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Status indicator
            if (isRunning) {
                PixelSpinner(
                    color = NeonPurple,
                    modifier = Modifier.size(12.dp),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (toolCall.result.isNotEmpty()) {
                                NeonGreen.copy(alpha = 0.8f) // success green
                            } else {
                                NeonPurple.copy(alpha = 0.5f)
                            },
                        ),
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = toolCall.toolName,
                style = MaterialTheme.typography.labelLarge,
                color = NeonPurple,
                modifier = Modifier.weight(1f),
            )

            Text(
                text = if (expanded) "[-]" else "[+]",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Expandable details
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                if (toolCall.arguments.isNotEmpty()) {
                    Text(
                        text = "ARGS:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = toolCall.arguments,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(6.dp),
                    )
                }

                if (toolCall.result.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "RESULT:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = toolCall.result,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(6.dp),
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

/**
 * Tiny pixel spinner: a single square that cycles through opacity.
 */
@Composable
private fun PixelSpinner(
    modifier: Modifier = Modifier,
    color: Color = NeonPurple,
) {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400),
            repeatMode = RepeatMode.Reverse,
        ),
    )

    Box(
        modifier = modifier.background(color.copy(alpha = alpha)),
    )
}
