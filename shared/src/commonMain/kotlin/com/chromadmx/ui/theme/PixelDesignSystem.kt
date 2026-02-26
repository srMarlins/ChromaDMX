package com.chromadmx.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// --- Matcha Palette ---
// Modern Japanese aesthetic: Soft, earthy greens, sakura pinks, yuzu yellows.
// Optimized for dark mode (stage environment) but with a softer, "cutesy" feel.

val MatchaPrimary = Color(0xFF9CCC65)      // Fresh Matcha Leaf
val MatchaDark = Color(0xFF33691E)         // Deep Tea Green
val MatchaLight = Color(0xFFDCEDC8)        // Milk Tea Green

val SakuraPink = Color(0xFFF48FB1)         // Cherry Blossom
val YuzuYellow = Color(0xFFFFF59D)         // Citrus Yellow
val AzukiRed = Color(0xFFE57373)           // Red Bean (Error/Alert)

val DarkMatchaBg = Color(0xFF1B261D)       // Very Dark Green-Black
val DarkMatchaSurface = Color(0xFF263228)  // Dark Moss Green
val DarkMatchaSurfaceVariant = Color(0xFF37473A) // Lighter Moss

@Immutable
data class PixelColors(
    val primary: Color = MatchaPrimary,
    val onPrimary: Color = Color(0xFF1B261D), // Dark text on light green
    val secondary: Color = SakuraPink,
    val onSecondary: Color = Color(0xFF3E2723), // Dark brown on pink
    val tertiary: Color = YuzuYellow,
    val onTertiary: Color = Color(0xFF3E2723), // Dark brown on yellow
    val background: Color = DarkMatchaBg,
    val onBackground: Color = MatchaLight,
    val surface: Color = DarkMatchaSurface,
    val onSurface: Color = MatchaLight,
    val surfaceVariant: Color = DarkMatchaSurfaceVariant,
    val onSurfaceVariant: Color = MatchaLight.copy(alpha = 0.8f),
    val error: Color = AzukiRed,
    val onError: Color = Color(0xFF3E2723),
    val success: Color = MatchaPrimary,
    val warning: Color = YuzuYellow,
    val info: Color = Color(0xFF81D4FA), // Soft Sky Blue
    val outline: Color = MatchaPrimary.copy(alpha = 0.6f),
    val outlineVariant: Color = MatchaPrimary.copy(alpha = 0.3f),
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
