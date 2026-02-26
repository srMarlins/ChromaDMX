package com.chromadmx.ui.screen.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chromadmx.core.model.DmxNode
import com.chromadmx.ui.components.PixelButton
import com.chromadmx.ui.components.PixelCard
import com.chromadmx.ui.theme.NeonGreen
import com.chromadmx.ui.theme.NeonYellow
import com.chromadmx.ui.theme.NodeOffline
import com.chromadmx.ui.theme.NodeOnline
import com.chromadmx.ui.theme.NodeWarning

@Composable
fun NodeStatusCard(
    node: DmxNode,
    currentTimeMs: Long,
    onDiagnose: (DmxNode) -> Unit,
    modifier: Modifier = Modifier
) {
    val level = node.healthLevel(currentTimeMs)
    val color = when (level) {
        HealthLevel.FULL -> NodeOnline
        HealthLevel.HALF -> NodeWarning
        HealthLevel.EMPTY -> NodeOffline
    }

    val uptimeSec = (currentTimeMs - node.firstSeenMs) / 1000
    val uptimeStr = if (uptimeSec < 60) "${uptimeSec}s" else "${uptimeSec / 60}m ${uptimeSec % 60}s"

    PixelCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = color,
        // glowColor removed
        title = {
            Text(
                text = node.shortName.ifEmpty { "Unknown Node" },
                style = MaterialTheme.typography.titleMedium,
                color = color
            )
        }
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("IP:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(node.ipAddress, style = MaterialTheme.typography.bodySmall)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Latency:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${node.latencyMs}ms", style = MaterialTheme.typography.bodySmall, color = if (node.latencyMs >= DmxNode.LATENCY_THRESHOLD_MS) NeonYellow else NeonGreen)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Uptime:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(uptimeStr, style = MaterialTheme.typography.bodySmall)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Universes:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(node.universes.joinToString(", "), style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(12.dp))

            PixelButton(
                onClick = { onDiagnose(node) },
                modifier = Modifier.align(Alignment.End),
                backgroundColor = MaterialTheme.colorScheme.secondary
            ) {
                Text("DIAGNOSE", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
