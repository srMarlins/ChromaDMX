package com.chromadmx.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
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
): Modifier = drawBehind {
    val ps = pixelSize.toPx()
    val color = Color.White.copy(alpha = opacity)

    // Draw vertical lines
    var x = 0f
    while (x < size.width) {
        drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        x += ps
    }

    // Draw horizontal lines
    var y = 0f
    while (y < size.height) {
        drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += ps
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
