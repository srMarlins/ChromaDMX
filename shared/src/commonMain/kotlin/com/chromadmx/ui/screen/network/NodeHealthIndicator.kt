package com.chromadmx.ui.screen.network

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
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
 * Small pixel heart indicating node health status.
 * Full = online, Half = warning, Empty = offline/unknown.
 */
@Composable
fun NodeHealthIndicator(
    health: NodeHealth,
    modifier: Modifier = Modifier,
    size: Int = 12
) {
    val color = when (health) {
        NodeHealth.ONLINE -> NodeOnline
        NodeHealth.WARNING -> NodeWarning
        NodeHealth.OFFLINE -> NodeOffline
        NodeHealth.UNKNOWN -> NodeUnknown
    }

    Canvas(
        modifier = modifier
            .size(size.dp)
            .semantics { contentDescription = "Node health: ${health.name.lowercase()}" }
    ) {
        drawPixelHeart(health, color)
    }
}

private fun DrawScope.drawPixelHeart(health: NodeHealth, color: Color) {
    val pixelCount = 7
    val pSize = size.width / pixelCount

    // Heart grid (7x7)
    // 0 1 1 0 1 1 0
    // 1 1 1 1 1 1 1
    // 1 1 1 1 1 1 1
    // 1 1 1 1 1 1 1
    // 0 1 1 1 1 1 0
    // 0 0 1 1 1 0 0
    // 0 0 0 1 0 0 0

    val heartMap = arrayOf(
        intArrayOf(0, 1, 1, 0, 1, 1, 0),
        intArrayOf(1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1),
        intArrayOf(0, 1, 1, 1, 1, 1, 0),
        intArrayOf(0, 0, 1, 1, 1, 0, 0),
        intArrayOf(0, 0, 0, 1, 0, 0, 0)
    )

    for (y in 0 until pixelCount) {
        for (x in 0 until pixelCount) {
            if (heartMap[y][x] == 1) {
                val isFilled = when (health) {
                    NodeHealth.ONLINE -> true
                    NodeHealth.WARNING -> x < pixelCount / 2 + (if (y % 2 == 0) 1 else 0) // Roughly half
                    NodeHealth.OFFLINE, NodeHealth.UNKNOWN -> false
                }

                if (isFilled) {
                    drawRect(
                        color = color,
                        topLeft = Offset(x * pSize, y * pSize),
                        size = Size(pSize, pSize)
                    )
                } else {
                    // Outline for empty/half
                    // Check if it's on the edge of the heart
                    val isEdge = isEdge(x, y, heartMap)
                    if (isEdge) {
                        drawRect(
                            color = color.copy(alpha = 0.5f),
                            topLeft = Offset(x * pSize, y * pSize),
                            size = Size(pSize, pSize)
                        )
                    }
                }
            }
        }
    }
}

private fun isEdge(x: Int, y: Int, map: Array<IntArray>): Boolean {
    if (map[y][x] == 0) return false
    val dx = intArrayOf(-1, 1, 0, 0)
    val dy = intArrayOf(0, 0, -1, 1)
    for (i in 0 until 4) {
        val nx = x + dx[i]
        val ny = y + dy[i]
        if (nx < 0 || nx >= 7 || ny < 0 || ny >= 7 || map[ny][nx] == 0) return true
    }
    return false
}
