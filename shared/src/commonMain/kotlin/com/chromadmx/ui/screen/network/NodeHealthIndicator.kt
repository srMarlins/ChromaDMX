package com.chromadmx.ui.screen.network

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chromadmx.core.model.DmxNode
import com.chromadmx.ui.theme.NodeOffline
import com.chromadmx.ui.theme.NodeOnline
import com.chromadmx.ui.theme.NodeWarning

enum class HealthLevel {
    FULL, HALF, EMPTY
}

/** Derive [HealthLevel] from a node's latency and last-seen time. */
fun DmxNode.healthLevel(currentTimeMs: Long): HealthLevel {
    val timeSinceLastSeen = currentTimeMs - lastSeenMs
    return when {
        timeSinceLastSeen >= DmxNode.LOST_TIMEOUT_MS -> HealthLevel.EMPTY
        latencyMs >= DmxNode.LATENCY_THRESHOLD_MS || timeSinceLastSeen >= DmxNode.DEGRADED_TIMEOUT_MS -> HealthLevel.HALF
        else -> HealthLevel.FULL
    }
}

/**
 * A pixel-art heart icon representing node health.
 *
 * @param level The health level (FULL, HALF, EMPTY)
 * @param modifier Modifier for sizing and layout
 * @param size Target size of the heart
 */
@Composable
fun NodeHealthIndicator(
    level: HealthLevel,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
) {
    val color = when (level) {
        HealthLevel.FULL -> NodeOnline
        HealthLevel.HALF -> NodeWarning
        HealthLevel.EMPTY -> NodeOffline
    }

    Canvas(modifier = modifier.size(size)) {
        val pixelSize = size.toPx() / 7f // 7x7 pixel grid for the heart

        // Pixel heart shape in 7x7 grid
        // . X X . X X .
        // X X X X X X X
        // X X X X X X X
        // . X X X X X .
        // . . X X X . .
        // . . . X . . .
        val pixels = listOf(
            Pair(1, 0), Pair(2, 0), Pair(4, 0), Pair(5, 0),
            Pair(0, 1), Pair(1, 1), Pair(2, 1), Pair(3, 1), Pair(4, 1), Pair(5, 1), Pair(6, 1),
            Pair(0, 2), Pair(1, 2), Pair(2, 2), Pair(3, 2), Pair(4, 2), Pair(5, 2), Pair(6, 2),
            Pair(1, 3), Pair(2, 3), Pair(3, 3), Pair(4, 3), Pair(5, 3),
            Pair(2, 4), Pair(3, 4), Pair(4, 4),
            Pair(3, 5)
        )

        pixels.forEach { (px, py) ->
            val isFilled = when (level) {
                HealthLevel.FULL -> true
                HealthLevel.HALF -> px <= 3
                HealthLevel.EMPTY -> false
            }

            val isOutline = (px == 1 && py == 0) || (px == 2 && py == 0) || (px == 4 && py == 0) || (px == 5 && py == 0) ||
                            (px == 0 && py == 1) || (px == 6 && py == 1) ||
                            (px == 0 && py == 2) || (px == 6 && py == 2) ||
                            (px == 1 && py == 3) || (px == 5 && py == 3) ||
                            (px == 2 && py == 4) || (px == 4 && py == 4) ||
                            (px == 3 && py == 5)

            if (isFilled || isOutline) {
                drawRect(
                    color = color,
                    topLeft = androidx.compose.ui.geometry.Offset(px * pixelSize, py * pixelSize),
                    size = androidx.compose.ui.geometry.Size(pixelSize, pixelSize),
                    alpha = if (isFilled) 1.0f else 0.4f
                )
            }
        }
    }
}
