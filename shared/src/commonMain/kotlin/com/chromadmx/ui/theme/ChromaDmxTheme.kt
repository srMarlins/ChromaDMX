package com.chromadmx.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
    colorTheme: PixelColorTheme = PixelColorTheme.MatchaDark,
    pixelThemeData: PixelThemeData = PixelThemeData(), // Legacy support
    content: @Composable () -> Unit
) {
    // Resolve theme enum to its color palette
    val themeColors = colorTheme.toColors()

    // Construct the new design system with the selected theme's colors
    val pixelSystem = PixelSystem(
        colors = themeColors,
        spacing = PixelSpacing(
            pixelSize = pixelThemeData.pixelSize
        )
    )

    // Choose dark or light Material3 color scheme based on theme
    val isDark = colorTheme != PixelColorTheme.MatchaLight

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = themeColors.primary,
            onPrimary = themeColors.onPrimary,
            primaryContainer = themeColors.primary.copy(alpha = 0.2f),
            onPrimaryContainer = themeColors.primary,
            secondary = themeColors.secondary,
            onSecondary = themeColors.onSecondary,
            secondaryContainer = themeColors.secondary.copy(alpha = 0.2f),
            onSecondaryContainer = themeColors.secondary,
            tertiary = themeColors.tertiary,
            onTertiary = themeColors.onTertiary,
            background = themeColors.background,
            onBackground = themeColors.onBackground,
            surface = themeColors.surface,
            onSurface = themeColors.onSurface,
            surfaceVariant = themeColors.surfaceVariant,
            onSurfaceVariant = themeColors.onSurfaceVariant,
            error = themeColors.error,
            onError = themeColors.onError,
            outline = themeColors.outline,
            outlineVariant = themeColors.outlineVariant
        )
    } else {
        lightColorScheme(
            primary = themeColors.primary,
            onPrimary = themeColors.onPrimary,
            primaryContainer = themeColors.primary.copy(alpha = 0.15f),
            onPrimaryContainer = themeColors.primary,
            secondary = themeColors.secondary,
            onSecondary = themeColors.onSecondary,
            secondaryContainer = themeColors.secondary.copy(alpha = 0.15f),
            onSecondaryContainer = themeColors.secondary,
            tertiary = themeColors.tertiary,
            onTertiary = themeColors.onTertiary,
            background = themeColors.background,
            onBackground = themeColors.onBackground,
            surface = themeColors.surface,
            onSurface = themeColors.onSurface,
            surfaceVariant = themeColors.surfaceVariant,
            onSurfaceVariant = themeColors.onSurfaceVariant,
            error = themeColors.error,
            onError = themeColors.onError,
            outline = themeColors.outline,
            outlineVariant = themeColors.outlineVariant
        )
    }

    CompositionLocalProvider(
        LocalPixelTheme provides pixelThemeData,
        LocalPixelSystem provides pixelSystem,
        LocalPixelColorTheme provides colorTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DmxTypography,
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
