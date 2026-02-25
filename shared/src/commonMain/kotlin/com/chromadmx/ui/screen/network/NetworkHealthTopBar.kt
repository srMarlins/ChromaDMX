package com.chromadmx.ui.screen.network

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.chromadmx.networking.model.DmxNode
import com.chromadmx.ui.util.currentTimeMillis
import com.chromadmx.ui.viewmodel.NetworkViewModel

/**
 * Compact network health display for the top bar.
 * Shows pixel hearts for each node and handles overflow.
 * Tapping expands a detailed node list overlay.
 */
@Composable
fun NetworkHealthTopBar(
    viewModel: NetworkViewModel,
    modifier: Modifier = Modifier,
) {
    val nodesMap by viewModel.nodes.collectAsState()
    val nodes = nodesMap.values.toList()
    var expanded by remember { mutableStateOf(false) }

    // Real clock for health calculation
    var currentTimeMs by remember { mutableStateOf(currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMs = currentTimeMillis()
            kotlinx.coroutines.delay(1_000L)
        }
    }

    Row(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { expanded = true },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val displayLimit = 3
        val displayNodes = nodes.take(displayLimit)
        val overflowCount = nodes.size - displayLimit

        displayNodes.forEach { node ->
            NodeHealthIndicator(health = nodeHealth(node, currentTimeMs))
        }

        if (overflowCount > 0) {
            Text(
                text = "+$overflowCount",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (nodes.isEmpty()) {
            Text(
                text = "No Nodes",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (expanded) {
        NodeListOverlay(
            nodes = nodes,
            currentTimeMs = currentTimeMs,
            onDiagnose = { viewModel.diagnoseNode(it) },
            onDismiss = { expanded = false }
        )
    }
}

@Composable
fun NodeListOverlay(
    nodes: List<DmxNode>,
    currentTimeMs: Long,
    onDiagnose: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(max = 500.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Network Status",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (nodes.isEmpty()) {
                    Text(
                        text = "No nodes discovered.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(nodes, key = { it.nodeKey }) { node ->
                        val health = nodeHealth(node, currentTimeMs)
                        NodeStatusCard(
                            node = node,
                            health = health,
                            currentTimeMs = currentTimeMs,
                            onDiagnose = { onDiagnose(node.nodeKey) }
                        )
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End).padding(top = 8.dp)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun NodeStatusCard(
    node: DmxNode,
    health: NodeHealth,
    currentTimeMs: Long,
    onDiagnose: () -> Unit,
) {
    val borderColor = when (health) {
        NodeHealth.HEALTHY -> com.chromadmx.ui.theme.NodeOnline
        NodeHealth.DEGRADED -> com.chromadmx.ui.theme.NodeWarning
        NodeHealth.LOST -> com.chromadmx.ui.theme.NodeOffline
        NodeHealth.UNKNOWN -> com.chromadmx.ui.theme.NodeUnknown
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NodeHealthIndicator(health = health)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = node.shortName.ifEmpty { "Node" },
                        style = MaterialTheme.typography.titleMedium
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

            Spacer(Modifier.height(8.dp))

            DetailRow("IP", node.ipAddress)
            if (node.universes.isNotEmpty()) {
                DetailRow("Universes", node.universes.joinToString(", "))
            }

            Spacer(Modifier.height(4.dp))

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

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatUptime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "${h}h ${m}m ${s}s" else if (m > 0) "${m}m ${s}s" else "${s}s"
}
