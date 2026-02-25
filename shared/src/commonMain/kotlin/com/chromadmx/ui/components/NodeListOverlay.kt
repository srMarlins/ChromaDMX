package com.chromadmx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.chromadmx.networking.model.DmxNode
import com.chromadmx.ui.screen.network.NodeCard
import com.chromadmx.ui.screen.network.nodeHealth
import com.chromadmx.ui.theme.DmxBackground

/**
 * Overlay showing the full list of discovered nodes and their status.
 */
@Composable
fun NodeListOverlay(
    nodes: List<DmxNode>,
    currentTimeMs: Long,
    onDismiss: () -> Unit,
    onDiagnose: (DmxNode) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        PixelCard(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DmxBackground)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Network Nodes",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    PixelButton(onClick = onDismiss) {
                        Text("CLOSE")
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (nodes.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No nodes discovered", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(nodes) { node ->
                            NodeCard(
                                node = node,
                                health = nodeHealth(node, currentTimeMs),
                                currentTimeMs = currentTimeMs,
                                onDiagnose = onDiagnose
                            )
                        }
                    }
                }
            }
        }
    }
}
