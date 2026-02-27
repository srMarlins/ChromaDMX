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
import com.chromadmx.ui.theme.PixelDesign

@Composable
fun NodeStatusCard(
    node: DmxNode,
    currentTimeMs: Long,
    onDiagnose: (DmxNode) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PixelDesign.colors
    val level = node.healthLevel(currentTimeMs)
    val color = when (level) {
        HealthLevel.FULL -> colors.success
        HealthLevel.HALF -> colors.warning
        HealthLevel.EMPTY -> colors.error
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
                Text("IP:", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                Text(node.ipAddress, style = MaterialTheme.typography.bodySmall, color = colors.onSurface)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Latency:", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                Text("${node.latencyMs}ms", style = MaterialTheme.typography.bodySmall, color = if (node.latencyMs >= DmxNode.LATENCY_THRESHOLD_MS) colors.warning else colors.success)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Uptime:", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                Text(uptimeStr, style = MaterialTheme.typography.bodySmall, color = colors.onSurface)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Universes:", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                Text(node.universes.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = colors.onSurface)
            }

            Spacer(modifier = Modifier.height(12.dp))

            PixelButton(
                onClick = { onDiagnose(node) },
                modifier = Modifier.align(Alignment.End),
                backgroundColor = colors.secondary
            ) {
                Text("DIAGNOSE", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
