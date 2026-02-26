package com.chromadmx.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chromadmx.core.model.DmxNode
import com.chromadmx.ui.screen.network.NodeHealthIndicator
import com.chromadmx.ui.screen.network.healthLevel

/**
 * Compact network health summary for the top bar.
 *
 * Shows up to 3 pixel heart icons. If more than 3 nodes are present,
 * shows "+N" overflow count. Tapping the component triggers the expanded view.
 *
 * @param nodes Current list of discovered nodes
 * @param currentTimeMs Current system time for health calculation
 * @param onClick Action when the component is tapped
 */
@Composable
fun NodeHealthCompact(
    nodes: List<DmxNode>,
    currentTimeMs: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable { onClick() }
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val displayNodes = nodes.take(3)
        val overflow = if (nodes.size > 3) nodes.size - 3 else 0

        displayNodes.forEach { node ->
            NodeHealthIndicator(level = node.healthLevel(currentTimeMs))
        }

        if (overflow > 0) {
            Text(
                text = "+$overflow",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 2.dp)
            )
        }

        if (nodes.isEmpty()) {
            Text(
                text = "No Nodes",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
