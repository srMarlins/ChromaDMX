package com.chromadmx.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ── Theme Enum ──────────────────────────────────────────────────────
/**
 * Available color themes for the ChromaDMX pixel design system.
 *
 * To add a new theme, define a new enum entry and a corresponding
 * [PixelColors] instance — no other code changes required.
 */
enum class PixelColorTheme {
    /** Dark matcha green — default stage environment theme. */
    MatchaDark,
    /** Light mint/cream — daytime or low-ambient-light variant. */
    MatchaLight,
    /** Pure black/white with maximum contrast — accessibility theme. */
    HighContrast,
}

// --- Matcha Palette ---
// Modern Japanese aesthetic: Soft, earthy greens, sakura pinks, yuzu yellows.
// Optimized for dark mode (stage environment) but with a softer, "cutesy" feel.

val MatchaPrimary = Color(0xFF9CCC65)      // Fresh Matcha Leaf
val MatchaDark = Color(0xFF33691E)         // Deep Tea Green
val MatchaLightColor = Color(0xFFDCEDC8)   // Milk Tea Green

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
    val onBackground: Color = MatchaLightColor,
    val surface: Color = DarkMatchaSurface,
    val onSurface: Color = MatchaLightColor,
    val surfaceVariant: Color = DarkMatchaSurfaceVariant,
    val onSurfaceVariant: Color = MatchaLightColor.copy(alpha = 0.8f),
    val error: Color = AzukiRed,
    val onError: Color = Color(0xFF3E2723),
    val success: Color = MatchaPrimary,
    val warning: Color = YuzuYellow,
    val info: Color = Color(0xFF81D4FA), // Soft Sky Blue
    val onSurfaceDim: Color = MatchaLightColor.copy(alpha = 0.6f),
    val primaryDark: Color = MatchaDark,
    val primaryLight: Color = MatchaLightColor,
    val scrim: Color = Color.Black.copy(alpha = 0.6f),
    val outline: Color = MatchaPrimary.copy(alpha = 0.6f),
    val outlineVariant: Color = MatchaPrimary.copy(alpha = 0.3f),
    val glow: Color = MatchaPrimary,
)

// ── Pre-defined Theme Palettes ──────────────────────────────────────

/** Default dark matcha palette (existing defaults in [PixelColors]). */
val MatchaDarkColors = PixelColors() // Uses data-class defaults

/** Light mint/cream palette for bright environments. */
val MatchaLightColors = PixelColors(
    primary = Color(0xFF558B2F),             // Dark matcha
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFAD1457),           // Deep sakura
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFFF9A825),            // Amber yuzu
    onTertiary = Color(0xFF3E2723),
    background = Color(0xFFF5F5F0),          // Warm cream
    onBackground = Color(0xFF1B261D),        // Dark green text
    surface = Color(0xFFFFFFFF),             // White
    onSurface = Color(0xFF263228),
    surfaceVariant = Color(0xFFE8F0E4),      // Pale green
    onSurfaceVariant = Color(0xFF263228).copy(alpha = 0.8f),
    error = Color(0xFFD32F2F),
    onError = Color(0xFFFFFFFF),
    success = Color(0xFF388E3C),
    warning = Color(0xFFF57F17),
    info = Color(0xFF1976D2),
    onSurfaceDim = Color(0xFF263228).copy(alpha = 0.5f),
    primaryDark = Color(0xFF33691E),         // Very dark matcha
    primaryLight = Color(0xFF8BC34A),        // Light matcha
    scrim = Color.Black.copy(alpha = 0.4f),
    outline = Color(0xFF8D9D85),             // Muted green (solid, no glow)
    outlineVariant = Color(0xFF8D9D85).copy(alpha = 0.5f),
    glow = Color(0xFF558B2F),
)

/** High-contrast accessibility palette — pure black/white, no glow. */
val HighContrastColors = PixelColors(
    primary = Color(0xFFFFFF00),             // Bright yellow (max contrast)
    onPrimary = Color(0xFF000000),
    secondary = Color(0xFF00FFFF),           // Cyan
    onSecondary = Color(0xFF000000),
    tertiary = Color(0xFFFF00FF),            // Magenta
    onTertiary = Color(0xFF000000),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFFFFFFFF),
    error = Color(0xFFFF0000),
    onError = Color(0xFFFFFFFF),
    success = Color(0xFF00FF00),
    warning = Color(0xFFFFFF00),
    info = Color(0xFF00FFFF),
    onSurfaceDim = Color(0xFFCCCCCC),        // Slightly dimmed white
    primaryDark = Color(0xFFCCCC00),         // Darker yellow
    primaryLight = Color(0xFFFFFF99),        // Lighter yellow
    scrim = Color.Black.copy(alpha = 0.8f),  // Stronger scrim for contrast
    outline = Color(0xFFFFFFFF),             // Full-opacity borders
    outlineVariant = Color(0xFFFFFFFF).copy(alpha = 0.7f),
    glow = Color(0xFFFFFF00),
)

/**
 * Resolves a [PixelColorTheme] to its [PixelColors] instance.
 *
 * To add a new theme, add an enum entry and a branch here — nothing else needed.
 */
fun PixelColorTheme.toColors(): PixelColors = when (this) {
    PixelColorTheme.MatchaDark -> MatchaDarkColors
    PixelColorTheme.MatchaLight -> MatchaLightColors
    PixelColorTheme.HighContrast -> HighContrastColors
}

// --- Spacing ---
@Immutable
data class PixelSpacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
    val screenPadding: Dp = 16.dp,
    val componentPadding: Dp = 12.dp,
    val sectionGap: Dp = 20.dp,
    val pixelSize: Dp = 4.dp // Base unit for borders/grid
)

// --- System ---
@Immutable
data class PixelSystem(
    val colors: PixelColors = PixelColors(),
    val spacing: PixelSpacing = PixelSpacing()
)

val LocalPixelSystem = staticCompositionLocalOf { PixelSystem() }

/**
 * CompositionLocal for the active [PixelColorTheme].
 *
 * Defaults to [PixelColorTheme.MatchaDark]. Provided by [ChromaDmxTheme].
 */
val LocalPixelColorTheme = staticCompositionLocalOf { PixelColorTheme.MatchaDark }

/**
 * CompositionLocal for the reduced-motion accessibility preference.
 *
 * Defaults to `false` (full motion). Platform-specific theme wrappers
 * should provide the actual system value via `CompositionLocalProvider`.
 *
 * On Android this maps to `Settings.Global.ANIMATOR_DURATION_SCALE == 0f`.
 * On iOS this maps to `UIAccessibility.isReduceMotionEnabled`.
 */
val LocalReduceMotion = staticCompositionLocalOf { false }

object PixelDesign {
    val colors: PixelColors
        @Composable
        @ReadOnlyComposable
        get() = LocalPixelSystem.current.colors

    val spacing: PixelSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalPixelSystem.current.spacing

    /**
     * The currently active color theme enum value.
     */
    val colorTheme: PixelColorTheme
        @Composable
        @ReadOnlyComposable
        get() = LocalPixelColorTheme.current

    /**
     * Whether the active theme uses a dark background.
     *
     * `true` for [PixelColorTheme.MatchaDark] and [PixelColorTheme.HighContrast];
     * `false` for [PixelColorTheme.MatchaLight].
     *
     * Use this for conditional rendering (e.g., glow intensity, scanline color).
     */
    val isDarkTheme: Boolean
        @Composable
        @ReadOnlyComposable
        get() = when (LocalPixelColorTheme.current) {
            PixelColorTheme.MatchaDark -> true
            PixelColorTheme.MatchaLight -> false
            PixelColorTheme.HighContrast -> true
        }

    /**
     * Whether the system requests reduced motion (accessibility preference).
     *
     * When `true`, animations should:
     * - Use instant springs (no bounce, very high stiffness)
     * - Skip continuous/looping animations (glow, sparkle, scanline)
     * - Keep only functional state transitions
     *
     * Use [ChromaAnimations.resolve] for motion-aware animation specs.
     */
    val reduceMotion: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalReduceMotion.current
}
