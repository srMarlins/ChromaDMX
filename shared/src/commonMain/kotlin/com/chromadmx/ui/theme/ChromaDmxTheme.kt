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

@Immutable
data class PixelThemeData(
    val pixelSize: Dp = 4.dp,
    val borderWidth: Dp = 4.dp,
    val gridOpacity: Float = 0.05f
)

val LocalPixelTheme = staticCompositionLocalOf { PixelThemeData() }

private val DmxDarkColorScheme = darkColorScheme(
    primary = DmxPrimary,
    onPrimary = DmxOnPrimary,
    primaryContainer = DmxPrimaryContainer,
    secondary = DmxSecondary,
    onSecondary = DmxOnSecondary,
    background = DmxBackground,
    surface = DmxSurface,
    surfaceVariant = DmxSurfaceVariant,
    onBackground = DmxOnBackground,
    onSurface = DmxOnSurface,
    error = DmxError,
    onError = DmxOnError,
    outline = Color.White.copy(alpha = 0.5f),
    outlineVariant = Color.White.copy(alpha = 0.2f)
)

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

@Composable
fun ChromaDmxTheme(
    pixelThemeData: PixelThemeData = PixelThemeData(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalPixelTheme provides pixelThemeData
    ) {
        MaterialTheme(
            colorScheme = DmxDarkColorScheme,
            typography = DmxTypography,
            content = content,
        )
    }
}
