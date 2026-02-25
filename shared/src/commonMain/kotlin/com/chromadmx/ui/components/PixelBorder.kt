package com.chromadmx.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Draws a pixelated border around a component.
 *
 * Note: This is a non-composable Modifier extension, so it cannot read from
 * LocalPixelTheme.current. Callers in @Composable contexts should pass
 * LocalPixelTheme.current.pixelSize explicitly if they want theme-driven sizing.
 */
fun Modifier.pixelBorder(
    width: Dp = 4.dp,
    color: Color = Color.White,
    pixelSize: Dp = 4.dp
): Modifier = drawBehind {
    val w = width.toPx()
    val ps = pixelSize.toPx()
    val layers = (w / ps).toInt().coerceAtLeast(1)

    for (i in 0 until layers) {
        val inset = i * ps
        // Top border
        drawRect(color, Offset(ps + inset, inset), Size(size.width - 2 * (ps + inset), ps))
        // Bottom border
        drawRect(color, Offset(ps + inset, size.height - ps - inset), Size(size.width - 2 * (ps + inset), ps))
        // Left border
        drawRect(color, Offset(inset, ps + inset), Size(ps, size.height - 2 * (ps + inset)))
        // Right border
        drawRect(color, Offset(size.width - ps - inset, ps + inset), Size(ps, size.height - 2 * (ps + inset)))
    }
}
