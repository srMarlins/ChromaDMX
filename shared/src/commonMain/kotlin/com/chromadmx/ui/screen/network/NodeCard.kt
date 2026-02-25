package com.chromadmx.ui.screen.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
 * Card displaying information about a discovered DMX node.
 *
 * Shows: node name, IP address, universes, firmware version,
 * health indicator, and port count.
 */
@Composable
fun NodeCard(
    node: DmxNode,
    health: NodeHealth,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            // Header: name + health indicator
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

                Text(
                    text = healthLabel(health),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(8.dp))

            // Details
            DetailRow("IP Address", node.ipAddress)
            if (node.macAddress.isNotEmpty()) {
                DetailRow("MAC", node.macAddress)
            }
            DetailRow("Ports", "${node.numPorts}")
            if (node.universes.isNotEmpty()) {
                DetailRow("Universes", node.universes.joinToString(", "))
            }
            if (node.firmwareVersion > 0) {
                DetailRow("Firmware", "v${node.firmwareVersion}")
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun healthLabel(health: NodeHealth): String = when (health) {
    NodeHealth.ONLINE -> "Online"
    NodeHealth.WARNING -> "Warning"
    NodeHealth.OFFLINE -> "Offline"
    NodeHealth.UNKNOWN -> "Unknown"
}
