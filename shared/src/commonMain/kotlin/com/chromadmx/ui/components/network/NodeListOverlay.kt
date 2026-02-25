package com.chromadmx.ui.components.network

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.components.PixelButton
import com.chromadmx.ui.components.PixelCard
import com.chromadmx.ui.theme.NodeOffline
import com.chromadmx.ui.theme.NodeOnline
import com.chromadmx.ui.theme.NodeWarning
import com.chromadmx.ui.theme.PixelFontFamily

/**
 * Full-screen overlay displaying detailed node status cards.
 *
 * Shows each discovered node with its name, IP, universe count, and
 * health status. Each card has a "Diagnose" button that fires [onDiagnose]
 * with the node's IP address.
 *
 * The overlay is dismissed by tapping outside the card list or
 * pressing back (handled by the caller via [onDismiss]).
 *
 * @param nodes       All discovered node statuses.
 * @param onDismiss   Called when the overlay should close.
 * @param onDiagnose  Called with the node IP when "Diagnose" is tapped.
 * @param modifier    Optional modifier.
 */
@Composable
fun NodeListOverlay(
    nodes: List<NodeStatus>,
    onDismiss: () -> Unit,
    onDiagnose: (ip: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Scrim: semi-transparent background; tap to dismiss.
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Card list container — tap inside does NOT dismiss.
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* consume click */ },
                )
                .padding(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(nodes, key = { it.ip }) { node ->
                NodeCard(
                    node = node,
                    onDiagnose = { onDiagnose(node.ip) },
                )
            }
        }
    }
}

// ── Single node card ───────────────────────────────────────────────

@Composable
private fun NodeCard(
    node: NodeStatus,
    onDiagnose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val healthColor: Color = when (node.health) {
        NodeHealth.HEALTHY -> NodeOnline
        NodeHealth.DEGRADED -> NodeWarning
        NodeHealth.LOST -> NodeOffline
    }
    val healthLabel: String = when (node.health) {
        NodeHealth.HEALTHY -> "Online"
        NodeHealth.DEGRADED -> "Degraded"
        NodeHealth.LOST -> "Lost"
    }

    PixelCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = healthColor.copy(alpha = 0.7f),
        glowColor = healthColor.copy(alpha = 0.15f),
    ) {
        Column {
            // Header row: heart + name
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelHeart(
                    health = node.health,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = node.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = PixelFontFamily),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Details
            DetailRow(label = "IP", value = node.ip)
            DetailRow(label = "Universes", value = node.universes.joinToString(", ").ifEmpty { "none" })
            DetailRow(label = "Status", value = healthLabel, valueColor = healthColor)

            Spacer(modifier = Modifier.height(8.dp))

            // Diagnose button
            PixelButton(
                onClick = onDiagnose,
                backgroundColor = healthColor.copy(alpha = 0.3f),
                contentColor = healthColor,
                borderColor = healthColor,
            ) {
                Text("Diagnose")
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(modifier = Modifier.padding(vertical = 1.dp)) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = PixelFontFamily),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = PixelFontFamily),
            color = valueColor,
        )
    }
}
