package com.chromadmx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.theme.PixelDesign

@Composable
fun PixelCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = PixelDesign.colors.surface,
    borderColor: Color = PixelDesign.colors.outline,
    glowColor: Color? = null,
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

        Box(
            modifier = Modifier
                .then(
                    if (glowColor != null) {
                        Modifier.drawBehind {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(glowColor.copy(alpha = 0.3f), Color.Transparent),
                                    center = Offset(size.width / 2f, size.height / 2f),
                                    radius = size.minDimension
                                ),
                                size = size
                            )
                        }
                    } else Modifier
                )
                .pixelBorder(color = borderColor, pixelSize = pixelSize)
                .background(backgroundColor)
                .padding(pixelSize) // Padding for border inset visual
                .padding(12.dp)
        ) {
            content()
        }
    }
}
