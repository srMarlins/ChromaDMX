package com.chromadmx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.theme.PixelDesign

@Composable
fun PixelCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = PixelDesign.colors.surface,
    borderColor: Color = PixelDesign.colors.outline,
    elevation: Dp = 4.dp, // Physical elevation
    pixelSize: Dp = PixelDesign.spacing.pixelSize,
    title: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier) {
        if (title != null) {
            Box(modifier = Modifier.padding(bottom = 8.dp)) {
                title()
            }
        }

        Box(modifier = Modifier) {
            // Hard Shadow
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = elevation, y = elevation)
                    .pixelBorder(color = Color.Black.copy(alpha = 0.2f), pixelSize = pixelSize)
                    .background(Color.Black.copy(alpha = 0.2f))
            )

            // Card Body
            Box(
                modifier = Modifier
                    .pixelBorder(color = borderColor, pixelSize = pixelSize)
                    .background(backgroundColor)
                    .padding(16.dp) // Generous padding
            ) {
                content()
            }
        }
    }
}
