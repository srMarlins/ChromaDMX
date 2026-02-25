package com.chromadmx.ui.screen.network

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.chromadmx.networking.model.DmxNode
import com.chromadmx.ui.theme.NodeOffline
import com.chromadmx.ui.theme.NodeOnline
import com.chromadmx.ui.theme.NodeUnknown
import com.chromadmx.ui.theme.NodeWarning

/**
 * Health status of a discovered DMX node.
 */
enum class NodeHealth {
    ONLINE,
    WARNING,
    OFFLINE,
    UNKNOWN,
}

/**
 * Determine the health status of a node based on elapsed time since last seen.
 */
fun nodeHealth(node: DmxNode, currentTimeMs: Long): NodeHealth {
    if (node.lastSeenMs == 0L) return NodeHealth.UNKNOWN
    val elapsed = currentTimeMs - node.lastSeenMs
    return when {
        elapsed < 5_000L -> NodeHealth.ONLINE
        elapsed < 15_000L -> NodeHealth.WARNING
        else -> NodeHealth.OFFLINE
    }
}

/**
 * Small colored dot indicating node health status.
 * Green = online, Yellow = warning, Red = offline, Gray = unknown.
 */
@Composable
fun NodeHealthIndicator(
    health: NodeHealth,
    modifier: Modifier = Modifier,
) {
    val color = when (health) {
        NodeHealth.ONLINE -> NodeOnline
        NodeHealth.WARNING -> NodeWarning
        NodeHealth.OFFLINE -> NodeOffline
        NodeHealth.UNKNOWN -> NodeUnknown
    }

    Canvas(
        modifier = modifier
            .size(12.dp)
            .semantics { contentDescription = "Node health: ${health.name.lowercase()}" }
    ) {
        drawCircle(
            color = color,
            radius = size.minDimension / 2f,
            center = Offset(size.width / 2f, size.height / 2f),
        )
    }
}
