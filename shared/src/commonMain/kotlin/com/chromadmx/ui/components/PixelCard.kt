package com.chromadmx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A pixel-art styled card component with a stepped "pixel" border.
 */
@Composable
fun PixelCard(
    modifier: Modifier = Modifier,
    pixelSize: Dp = 2.dp,
    title: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

    Column(modifier = modifier) {
        if (title != null) {
            Box(modifier = Modifier.padding(bottom = 8.dp)) {
                title()
            }
        }

        Box(
            modifier = Modifier
                .background(backgroundColor)
                .drawPixelBorder(borderColor, pixelSize)
                .padding(16.dp)
        ) {
            content()
        }
    }
}

private fun Modifier.drawPixelBorder(color: Color, pixelSizeDp: Dp) = this.then(
    Modifier.drawWithContent {
        drawContent()

        val pixelSize = pixelSizeDp.toPx()
        val width = size.width
        val height = size.height

        // Draw top border
        var x = 0f
        while (x < width) {
            drawRect(color, Offset(x, 0f), Size(pixelSize, pixelSize))
            x += pixelSize * 2
        }

        // Draw bottom border
        x = 0f
        while (x < width) {
            drawRect(color, Offset(x, height - pixelSize), Size(pixelSize, pixelSize))
            x += pixelSize * 2
        }

        // Draw left border
        var y = 0f
        while (y < height) {
            drawRect(color, Offset(0f, y), Size(pixelSize, pixelSize))
            y += pixelSize * 2
        }

        // Draw right border
        y = 0f
        while (y < height) {
            drawRect(color, Offset(width - pixelSize, y), Size(pixelSize, pixelSize))
            y += pixelSize * 2
        }
    }
)
