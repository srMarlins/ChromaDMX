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
 */
fun Modifier.pixelBorder(
    width: Dp = 4.dp,
    color: Color = Color.White,
    pixelSize: Dp = 4.dp
): Modifier = drawBehind {
    val w = width.toPx()
    val ps = pixelSize.toPx()

    // Top border
    drawRect(color, Offset(ps, 0f), Size(size.width - 2 * ps, ps))
    // Bottom border
    drawRect(color, Offset(ps, size.height - ps), Size(size.width - 2 * ps, ps))
    // Left border
    drawRect(color, Offset(0f, ps), Size(ps, size.height - 2 * ps))
    // Right border
    drawRect(color, Offset(size.width - ps, ps), Size(ps, size.height - 2 * ps))

    // Inset border if width > pixelSize
    if (w > ps) {
        // Additional layers could be added for thicker borders
    }
}
