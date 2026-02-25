package com.chromadmx.ui.screen.network

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chromadmx.networking.model.DmxNode

/**
 * Pixel-styled card displaying information about a discovered DMX node.
 *
 * Shows: node name, IP address, universes, latency, uptime,
 * and health indicator.
 */
@Composable
fun NodeCard(
    node: DmxNode,
    health: NodeHealth,
    currentTimeMs: Long,
    onDiagnose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = when (health) {
        NodeHealth.HEALTHY -> com.chromadmx.ui.theme.NodeOnline
        NodeHealth.DEGRADED -> com.chromadmx.ui.theme.NodeWarning
        NodeHealth.LOST -> com.chromadmx.ui.theme.NodeOffline
        NodeHealth.UNKNOWN -> com.chromadmx.ui.theme.NodeUnknown
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            // Header: name + health indicator + diagnose button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NodeHealthIndicator(health = health)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = node.shortName.ifEmpty { node.longName.ifEmpty { "Unknown Node" } },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Button(
                    onClick = onDiagnose,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Diagnose", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Details
            DetailRow("IP Address", node.ipAddress)
            if (node.universes.isNotEmpty()) {
                DetailRow("Universes", node.universes.joinToString(", "))
            }

            Spacer(Modifier.height(8.dp))

            // Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Latency: ${node.latencyMs}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (node.latencyMs > 100) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )

                val uptimeSeconds = if (node.firstSeenMs > 0) (currentTimeMs - node.firstSeenMs) / 1000 else 0
                Text(
                    text = "Uptime: ${formatUptime(uptimeSeconds)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

