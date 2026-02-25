package com.chromadmx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromadmx.networking.model.DmxNode
import com.chromadmx.ui.screen.network.NodeHealth
import com.chromadmx.ui.screen.network.NodeHealthIndicator
import com.chromadmx.ui.screen.network.nodeHealth

/**
 * Compact network health display for the top bar.
 * Shows up to 3 hearts representing node health, with an overflow count.
 * Tap to trigger [onExpand].
 */
@Composable
fun NodeHealthCompact(
    nodes: List<DmxNode>,
    currentTimeMs: Long,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable { onExpand() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val displayNodes = nodes.take(3)
        val overflow = nodes.size - displayNodes.size

        displayNodes.forEach { node ->
            NodeHealthIndicator(
                health = nodeHealth(node, currentTimeMs),
                size = 14
            )
        }

        if (overflow > 0) {
            Text(
                text = "+$overflow",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontFamily = MaterialTheme.typography.labelSmall.fontFamily // Should be pixel font if configured
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (nodes.isEmpty()) {
            NodeHealthIndicator(
                health = NodeHealth.OFFLINE,
                size = 14
            )
        }
    }
}
