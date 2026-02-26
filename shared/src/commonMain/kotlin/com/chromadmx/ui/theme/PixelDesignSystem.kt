package com.chromadmx.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// --- Pixel Specific Colors ---
val PixelBackground = Color(0xFF060612)
val PixelSurface = Color(0xFF0A0A1E)
val PixelSurfaceVariant = Color(0xFF141428)
val NeonRed = Color(0xFFFF0000)

@Immutable
data class PixelColors(
    val primary: Color = NeonCyan,
    val onPrimary: Color = Color.Black,
    val secondary: Color = NeonMagenta,
    val onSecondary: Color = Color.Black,
    val tertiary: Color = NeonYellow,
    val onTertiary: Color = Color.Black,
    val background: Color = PixelBackground,
    val onBackground: Color = Color.White,
    val surface: Color = PixelSurface,
    val onSurface: Color = Color.White,
    val surfaceVariant: Color = PixelSurfaceVariant,
    val onSurfaceVariant: Color = Color.White.copy(alpha = 0.7f),
    val error: Color = NeonRed,
    val onError: Color = Color.Black,
    val success: Color = NeonGreen,
    val warning: Color = NodeWarning,
    val info: Color = NeonCyan,
    val outline: Color = Color.White.copy(alpha = 0.5f),
    val outlineVariant: Color = Color.White.copy(alpha = 0.2f),
)

// --- Spacing ---
@Immutable
data class PixelSpacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
    val screenPadding: Dp = 16.dp,
    val pixelSize: Dp = 4.dp // Base unit for borders/grid
)

// --- System ---
@Immutable
data class PixelSystem(
    val colors: PixelColors = PixelColors(),
    val spacing: PixelSpacing = PixelSpacing()
)

val LocalPixelSystem = staticCompositionLocalOf { PixelSystem() }

object PixelDesign {
    val colors: PixelColors
        @Composable
        @ReadOnlyComposable
        get() = LocalPixelSystem.current.colors

    val spacing: PixelSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalPixelSystem.current.spacing
}
