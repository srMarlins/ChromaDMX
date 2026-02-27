package com.chromadmx.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromadmx.core.model.DmxNode
import com.chromadmx.ui.screen.network.HealthLevel
import com.chromadmx.ui.screen.network.healthLevel
import com.chromadmx.ui.theme.PixelDesign
import com.chromadmx.ui.theme.PixelFontFamily

/**
 * Compact network health summary for the top bar.
 *
 * Shows node counts grouped by health status with colored pixel indicators.
 * Scales cleanly to any number of nodes. Tapping opens the expanded view.
 *
 * Examples:
 * - All healthy:  `■ 4`
 * - Mixed:        `■ 3  ■ 1`
 * - No nodes:     `SIM` or `No Nodes`
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
    modifier: Modifier = Modifier,
    isSimulationMode: Boolean = false,
) {
    val colors = PixelDesign.colors

    Row(
        modifier = modifier
            .clickable { onClick() }
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (nodes.isEmpty()) {
            val (label, color) = if (isSimulationMode) {
                "SIM" to colors.info
            } else {
                "No Nodes" to colors.warning
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = PixelFontFamily,
                ),
                color = color,
            )
        } else {
            val healthy = nodes.count { it.healthLevel(currentTimeMs) == HealthLevel.FULL }
            val degraded = nodes.count { it.healthLevel(currentTimeMs) == HealthLevel.HALF }
            val lost = nodes.count { it.healthLevel(currentTimeMs) == HealthLevel.EMPTY }

            if (healthy > 0) {
                NodeStatusSegment(count = healthy, color = colors.success)
            }
            if (degraded > 0) {
                NodeStatusSegment(count = degraded, color = colors.warning)
            }
            if (lost > 0) {
                NodeStatusSegment(count = lost, color = colors.error)
            }
        }
    }
}

/**
 * A single health-status segment: colored pixel square + count label.
 */
@Composable
private fun NodeStatusSegment(
    count: Int,
    color: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Canvas(modifier = Modifier.size(8.dp)) {
            drawRect(color = color)
        }
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = PixelFontFamily,
                fontSize = 11.sp,
            ),
            color = color,
        )
    }
}
