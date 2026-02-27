package com.chromadmx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.chromadmx.ui.theme.PixelDesign
import com.chromadmx.ui.theme.pixelGrid

@Composable
fun PixelSurface(
    modifier: Modifier = Modifier,
    color: Color = PixelDesign.colors.background,
    contentColor: Color = PixelDesign.colors.onBackground,
    showGrid: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    // Subtle Matcha Grid: Use primary color (green) but very low opacity
    val gridColor = if (showGrid) {
        Modifier.pixelGrid(
            pixelSize = PixelDesign.spacing.pixelSize * 2,
            opacity = 0.05f // Subtle but visible
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .background(color)
            .then(gridColor),
        content = content
    )
}
