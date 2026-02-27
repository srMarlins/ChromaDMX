package com.chromadmx.ui.components.network

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.theme.PixelDesign
import com.chromadmx.ui.theme.PixelFontFamily

/**
 * Compact composable showing pixel-heart icons for each discovered node.
 *
 * - Filled heart  = HEALTHY  (NodeOnline green)
 * - Half heart    = DEGRADED (NodeWarning yellow)
 * - Empty heart   = LOST     (NodeOffline red)
 *
 * When more than [MAX_VISIBLE_HEARTS] nodes are present, shows the first
 * three hearts followed by a "+N" overflow label.
 *
 * Tapping the bar invokes [onExpand] to open the full node list overlay.
 *
 * @param nodes     Current node statuses to display.
 * @param onExpand  Called when the user taps the bar.
 * @param modifier  Optional modifier.
 */
@Composable
fun NodeHealthBar(
    nodes: List<NodeStatus>,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (nodes.isEmpty()) return

    val visible = nodes.take(MAX_VISIBLE_HEARTS)
    val overflowText = compactOverflowText(nodes.size)

    Row(
        modifier = modifier
            .clickable(onClick = onExpand)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (node in visible) {
            PixelHeart(
                health = node.health,
                modifier = Modifier.size(20.dp),
            )
        }
        if (overflowText != null) {
            Text(
                text = overflowText,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = PixelFontFamily),
                color = PixelDesign.colors.onSurface,
                modifier = Modifier.padding(start = 2.dp),
            )
        }
    }
}

// ── Pixel Heart composable ─────────────────────────────────────────

/**
 * Pixel-art heart icon on a 7x6 grid.
 *
 * Heart shape:
 * ```
 *   .##.##.
 *   #######
 *   #######
 *   .#####.
 *   ..###..
 *   ...#...
 * ```
 *
 * Fill depends on [NodeHealth]:
 * - HEALTHY:  fully filled with [NodeOnline]
 * - DEGRADED: bottom half filled with [NodeWarning], top outline only
 * - LOST:     empty outline in [NodeOffline]
 */
@Composable
internal fun PixelHeart(
    health: NodeHealth,
    modifier: Modifier = Modifier,
) {
    val colors = PixelDesign.colors
    val color: Color = when (health) {
        NodeHealth.HEALTHY -> colors.success
        NodeHealth.DEGRADED -> colors.warning
        NodeHealth.LOST -> colors.error
    }
    val fillFraction: Float = when (health) {
        NodeHealth.HEALTHY -> 1f
        NodeHealth.DEGRADED -> 0.5f
        NodeHealth.LOST -> 0f
    }

    Box(
        modifier = modifier.drawBehind {
            val pxW = size.width / HEART_GRID_COLS
            val pxH = size.height / HEART_GRID_ROWS

            val filledRows = (HEART_GRID_ROWS * fillFraction).toInt()
            val outlineAlpha = if (fillFraction == 0f) 0.5f else 0.25f

            for (row in HEART_PIXELS.indices) {
                for (col in HEART_PIXELS[row].indices) {
                    if (!HEART_PIXELS[row][col]) continue

                    // Rows numbered top-to-bottom; filled rows count from bottom.
                    val isFilled = row >= (HEART_GRID_ROWS - filledRows)
                    val drawColor = if (isFilled) color else color.copy(alpha = outlineAlpha)

                    drawRect(
                        color = drawColor,
                        topLeft = Offset(col * pxW, row * pxH),
                        size = Size(pxW, pxH),
                    )
                }
            }
        }
    )
}

// Heart pixel map: 7 columns x 6 rows
private const val HEART_GRID_COLS = 7
private const val HEART_GRID_ROWS = 6
private val HEART_PIXELS: List<List<Boolean>> = listOf(
    listOf(false, true, true, false, true, true, false),
    listOf(true, true, true, true, true, true, true),
    listOf(true, true, true, true, true, true, true),
    listOf(false, true, true, true, true, true, false),
    listOf(false, false, true, true, true, false, false),
    listOf(false, false, false, true, false, false, false),
)
