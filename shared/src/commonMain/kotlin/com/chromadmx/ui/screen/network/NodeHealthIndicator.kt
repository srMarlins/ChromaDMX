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
    HEALTHY,    // Full heart, green
    DEGRADED,   // Half heart, yellow
    LOST,       // Empty heart, red
    UNKNOWN,    // Gray
}

/**
 * Determine the health status of a node based on elapsed time since last seen and latency.
 */
fun nodeHealth(node: DmxNode, currentTimeMs: Long): NodeHealth {
    if (node.lastSeenMs == 0L) return NodeHealth.UNKNOWN
    val elapsed = currentTimeMs - node.lastSeenMs

    return when {
        elapsed > 10_000L -> NodeHealth.LOST
        elapsed > 5_000L || node.latencyMs > 100L -> NodeHealth.DEGRADED
        else -> NodeHealth.HEALTHY
    }
}

/**
 * Pixel-styled heart icon indicating node health status.
 * Full = healthy, Half = degraded, Empty = lost.
 */
@Composable
fun NodeHealthIndicator(
    health: NodeHealth,
    modifier: Modifier = Modifier,
) {
    PixelHeart(
        health = health,
        modifier = modifier
            .size(16.dp)
            .semantics { contentDescription = "Node health: ${health.name.lowercase()}" }
    )
}

/**
 * Draws a pixelated heart icon.
 */
@Composable
fun PixelHeart(
    health: NodeHealth,
    modifier: Modifier = Modifier,
) {
    val color = when (health) {
        NodeHealth.HEALTHY -> NodeOnline
        NodeHealth.DEGRADED -> NodeWarning
        NodeHealth.LOST -> NodeOffline
        NodeHealth.UNKNOWN -> NodeUnknown
    }

    Canvas(modifier = modifier) {
        val pixelSize = size.width / 7f

        fun drawPixel(x: Int, y: Int) {
            drawRect(
                color = color,
                topLeft = Offset(x * pixelSize, y * pixelSize),
                size = androidx.compose.ui.geometry.Size(pixelSize, pixelSize)
            )
        }

        fun drawOutlinePixel(x: Int, y: Int) {
            drawRect(
                color = color.copy(alpha = 0.5f),
                topLeft = Offset(x * pixelSize, y * pixelSize),
                size = androidx.compose.ui.geometry.Size(pixelSize, pixelSize),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
            )
        }

        // Heart shape (7x7 pixels)
        //   0 1 2 3 4 5 6
        // 0   X X   X X
        // 1 X X X X X X X
        // 2 X X X X X X X
        // 3 X X X X X X X
        // 4   X X X X X
        // 5     X X X
        // 6       X

        val heartPixels = listOf(
            1 to 0, 2 to 0, 4 to 0, 5 to 0,
            0 to 1, 1 to 1, 2 to 1, 3 to 1, 4 to 1, 5 to 1, 6 to 1,
            0 to 2, 1 to 2, 2 to 2, 3 to 2, 4 to 2, 5 to 2, 6 to 2,
            0 to 3, 1 to 3, 2 to 3, 3 to 3, 4 to 3, 5 to 3, 6 to 3,
            1 to 4, 2 to 4, 3 to 4, 4 to 4, 5 to 4,
            2 to 5, 3 to 5, 4 to 5,
            3 to 6
        )

        heartPixels.forEach { (x, y) ->
            when (health) {
                NodeHealth.HEALTHY -> drawPixel(x, y)
                NodeHealth.DEGRADED -> {
                    if (x < 4) drawPixel(x, y) else drawOutlinePixel(x, y)
                }
                NodeHealth.LOST, NodeHealth.UNKNOWN -> drawOutlinePixel(x, y)
            }
        }
    }
}
