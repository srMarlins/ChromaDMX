package com.chromadmx.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Retain for backward compatibility during migration, but map to new system where possible
@Immutable
data class PixelThemeData(
    val pixelSize: Dp = 4.dp,
    val borderWidth: Dp = 4.dp,
    val gridOpacity: Float = 0.05f
)

val LocalPixelTheme = staticCompositionLocalOf { PixelThemeData() }

@Composable
fun ChromaDmxTheme(
    pixelThemeData: PixelThemeData = PixelThemeData(), // Legacy support
    content: @Composable () -> Unit
) {
    // Construct the new design system
    val pixelSystem = PixelSystem(
        spacing = PixelSpacing(
            pixelSize = pixelThemeData.pixelSize
        )
    )

    // Map PixelColors to Material3 ColorScheme
    val colorScheme = darkColorScheme(
        primary = pixelSystem.colors.primary,
        onPrimary = pixelSystem.colors.onPrimary,
        primaryContainer = pixelSystem.colors.primary.copy(alpha = 0.2f),
        onPrimaryContainer = pixelSystem.colors.primary,
        secondary = pixelSystem.colors.secondary,
        onSecondary = pixelSystem.colors.onSecondary,
        secondaryContainer = pixelSystem.colors.secondary.copy(alpha = 0.2f),
        onSecondaryContainer = pixelSystem.colors.secondary,
        tertiary = pixelSystem.colors.tertiary,
        onTertiary = pixelSystem.colors.onTertiary,
        background = pixelSystem.colors.background,
        onBackground = pixelSystem.colors.onBackground,
        surface = pixelSystem.colors.surface,
        onSurface = pixelSystem.colors.onSurface,
        surfaceVariant = pixelSystem.colors.surfaceVariant,
        onSurfaceVariant = pixelSystem.colors.onSurfaceVariant,
        error = pixelSystem.colors.error,
        onError = pixelSystem.colors.onError,
        outline = pixelSystem.colors.outline,
        outlineVariant = pixelSystem.colors.outlineVariant
    )

    CompositionLocalProvider(
        LocalPixelTheme provides pixelThemeData,
        LocalPixelSystem provides pixelSystem
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DmxTypography, // Assuming this exists from previous listing
            content = content,
        )
    }
}

/**
 * Applies a subtle pixel grid texture to the background.
 */
fun Modifier.pixelGrid(
    pixelSize: Dp = 8.dp,
    opacity: Float = 0.05f
): Modifier = drawWithCache {
    val ps = pixelSize.toPx()
    val color = Color.White.copy(alpha = opacity)

    // Pre-compute line endpoints
    val verticalLines = mutableListOf<Pair<Offset, Offset>>()
    var x = 0f
    while (x < size.width) {
        verticalLines.add(Offset(x, 0f) to Offset(x, size.height))
        x += ps
    }

    val horizontalLines = mutableListOf<Pair<Offset, Offset>>()
    var y = 0f
    while (y < size.height) {
        horizontalLines.add(Offset(0f, y) to Offset(size.width, y))
        y += ps
    }

    onDrawBehind {
        for ((start, end) in verticalLines) {
            drawLine(color, start, end, strokeWidth = 1f)
        }
        for ((start, end) in horizontalLines) {
            drawLine(color, start, end, strokeWidth = 1f)
        }
    }
}
