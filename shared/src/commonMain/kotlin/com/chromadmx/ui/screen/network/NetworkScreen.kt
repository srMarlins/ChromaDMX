package com.chromadmx.ui.screen.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.chromadmx.ui.util.currentTimeMillis
import com.chromadmx.ui.viewmodel.NetworkViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Network screen showing discovered DMX nodes.
 *
 * Displays a list of node cards with health indicators.
 * Provides scan start/stop controls and a manual poll trigger.
 */
@Composable
fun NetworkScreen(
    viewModel: NetworkViewModel,
) {
    val nodesMap by viewModel.nodes.collectAsState()
    val nodes = nodesMap.values.toList()
    val isScanning by viewModel.isScanning.collectAsState()

    // Real clock for health calculation — updates every second
    var currentTimeMs by remember { mutableStateOf(currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            currentTimeMs = currentTimeMillis()
            delay(1_000L)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "DMX Network",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "${nodes.size} node${if (nodes.size != 1) "s" else ""} discovered",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isScanning) {
                    OutlinedButton(onClick = { viewModel.stopScan() }) {
                        Text("Stop")
                    }
                } else {
                    FilledTonalButton(onClick = { viewModel.startScan() }) {
                        Text("Scan")
                    }
                }
                OutlinedButton(onClick = { viewModel.triggerPoll() }) {
                    Text("Poll")
                }
            }
        }

        // Scanning indicator
        if (isScanning) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }

        Spacer(Modifier.height(4.dp))

        // Node list
        if (nodes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No DMX Nodes Found",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Tap 'Scan' to discover Art-Net nodes on the network.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                items(nodes, key = { it.nodeKey }) { node ->
                    val health = nodeHealth(node, currentTimeMs)
                    NodeCard(
                        node = node,
                        health = health,
                        currentTimeMs = currentTimeMs,
                        onDiagnose = { viewModel.diagnoseNode(node.nodeKey) }
                    )
                }
            }
        }
    }
}
