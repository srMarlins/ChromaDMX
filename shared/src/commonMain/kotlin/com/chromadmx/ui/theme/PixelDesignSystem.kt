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
    /** Electric blues, hot pinks, deep purple-black. */
    NeonCyberpunk,
    /** Deep navy, teal, aquamarine. Calm underwater. */
    OceanDepths,
    /** Amber, coral, warm dark browns. Golden hour. */
    SunsetWarm,
    /** Grayscale with single cyan accent. Minimal, professional. */
    MonochromePro,
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
    val scrim: Color = Color.Black.copy(alpha = 0.45f),
    val outline: Color = MatchaPrimary.copy(alpha = 0.6f),
    val outlineVariant: Color = MatchaPrimary.copy(alpha = 0.3f),
    val glow: Color = MatchaPrimary,
    // Stage infrastructure colors
    val stageBackground: Color = Color(0xFF060612),
    val stageFloor: Color = Color(0xFF0A0A14),
    val stageHorizon: Color = Color(0xFF1A1A30),
    val trussColor: Color = Color(0xFF2A2A3E),
    val trussBorder: Color = Color(0xFF3A3A52),
    val fixtureHousing: Color = Color(0xFF1A1A2E),
    val fixtureHousingBorder: Color = Color(0xFF2A2A3E),
    val scanlineColor: Color = Color.White.copy(alpha = 0.02f),
    val gridLineColor: Color = Color.White.copy(alpha = 0.05f),
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
    stageBackground = Color(0xFFE8E8E0),
    stageFloor = Color(0xFFD8D8D0),
    stageHorizon = Color(0xFFC8C8C0),
    trussColor = Color(0xFFB0B0A8),
    trussBorder = Color(0xFFC0C0B8),
    fixtureHousing = Color(0xFFA0A098),
    fixtureHousingBorder = Color(0xFFB8B8B0),
    scanlineColor = Color.Black.copy(alpha = 0.02f),
    gridLineColor = Color.Black.copy(alpha = 0.04f),
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
    stageBackground = Color(0xFF000000),
    stageFloor = Color(0xFF0A0A0A),
    stageHorizon = Color(0xFFFFFFFF),
    trussColor = Color(0xFFCCCCCC),
    trussBorder = Color(0xFFFFFFFF),
    fixtureHousing = Color(0xFF333333),
    fixtureHousingBorder = Color(0xFFFFFFFF),
    scanlineColor = Color.White.copy(alpha = 0.05f),
    gridLineColor = Color.White.copy(alpha = 0.1f),
)

/** Neon Cyberpunk — electric blues, hot pinks, deep purple-black. */
val NeonCyberpunkColors = PixelColors(
    primary = Color(0xFF00D4FF),
    onPrimary = Color(0xFF0D0221),
    secondary = Color(0xFFFF2E97),
    onSecondary = Color(0xFF0D0221),
    tertiary = Color(0xFFBF00FF),
    onTertiary = Color(0xFF0D0221),
    background = Color(0xFF0D0221),
    onBackground = Color(0xFFE0D4FF),
    surface = Color(0xFF1A0A3E),
    onSurface = Color(0xFFE0D4FF),
    surfaceVariant = Color(0xFF261450),
    onSurfaceVariant = Color(0xFFE0D4FF).copy(alpha = 0.8f),
    error = Color(0xFFFF4444),
    onError = Color(0xFF0D0221),
    success = Color(0xFF00FF88),
    warning = Color(0xFFFFD600),
    info = Color(0xFF00D4FF),
    onSurfaceDim = Color(0xFFE0D4FF).copy(alpha = 0.6f),
    primaryDark = Color(0xFF0088AA),
    primaryLight = Color(0xFF66E5FF),
    scrim = Color(0xFF0D0221).copy(alpha = 0.7f),
    outline = Color(0xFF00D4FF).copy(alpha = 0.6f),
    outlineVariant = Color(0xFF00D4FF).copy(alpha = 0.3f),
    glow = Color(0xFF00D4FF),
    stageBackground = Color(0xFF0A0118),
    stageFloor = Color(0xFF0D0221),
    stageHorizon = Color(0xFF2A1050),
    trussColor = Color(0xFF2E1A5E),
    trussBorder = Color(0xFF4A2880),
    fixtureHousing = Color(0xFF1A0A3E),
    fixtureHousingBorder = Color(0xFF2E1A5E),
    scanlineColor = Color(0xFF00D4FF).copy(alpha = 0.03f),
    gridLineColor = Color(0xFFBF00FF).copy(alpha = 0.05f),
)

/** Ocean Depths — deep navy, teal, aquamarine. Calm underwater. */
val OceanDepthsColors = PixelColors(
    primary = Color(0xFF00B4D8),
    onPrimary = Color(0xFF03045E),
    secondary = Color(0xFF90E0EF),
    onSecondary = Color(0xFF03045E),
    tertiary = Color(0xFF48CAE4),
    onTertiary = Color(0xFF03045E),
    background = Color(0xFF03045E),
    onBackground = Color(0xFFCAF0F8),
    surface = Color(0xFF0A1128),
    onSurface = Color(0xFFCAF0F8),
    surfaceVariant = Color(0xFF122040),
    onSurfaceVariant = Color(0xFFCAF0F8).copy(alpha = 0.8f),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF03045E),
    success = Color(0xFF00E676),
    warning = Color(0xFFFFCA28),
    info = Color(0xFF00B4D8),
    onSurfaceDim = Color(0xFFCAF0F8).copy(alpha = 0.6f),
    primaryDark = Color(0xFF007B9E),
    primaryLight = Color(0xFF66D4EB),
    scrim = Color(0xFF03045E).copy(alpha = 0.7f),
    outline = Color(0xFF00B4D8).copy(alpha = 0.6f),
    outlineVariant = Color(0xFF00B4D8).copy(alpha = 0.3f),
    glow = Color(0xFF00B4D8),
    stageBackground = Color(0xFF020338),
    stageFloor = Color(0xFF03045E),
    stageHorizon = Color(0xFF0A2472),
    trussColor = Color(0xFF1A3A6E),
    trussBorder = Color(0xFF2A5080),
    fixtureHousing = Color(0xFF0A1840),
    fixtureHousingBorder = Color(0xFF1A3060),
    scanlineColor = Color(0xFF00B4D8).copy(alpha = 0.02f),
    gridLineColor = Color(0xFF90E0EF).copy(alpha = 0.04f),
)

/** Sunset Warm — amber, coral, warm dark browns. Golden hour. */
val SunsetWarmColors = PixelColors(
    primary = Color(0xFFFF9E00),
    onPrimary = Color(0xFF1A0F0A),
    secondary = Color(0xFFFF6B6B),
    onSecondary = Color(0xFF1A0F0A),
    tertiary = Color(0xFFFFD54F),
    onTertiary = Color(0xFF1A0F0A),
    background = Color(0xFF1A0F0A),
    onBackground = Color(0xFFFFE0C0),
    surface = Color(0xFF2A1A10),
    onSurface = Color(0xFFFFE0C0),
    surfaceVariant = Color(0xFF3A2418),
    onSurfaceVariant = Color(0xFFFFE0C0).copy(alpha = 0.8f),
    error = Color(0xFFFF4444),
    onError = Color(0xFF1A0F0A),
    success = Color(0xFF8BC34A),
    warning = Color(0xFFFFD54F),
    info = Color(0xFFFFB74D),
    onSurfaceDim = Color(0xFFFFE0C0).copy(alpha = 0.6f),
    primaryDark = Color(0xFFCC7E00),
    primaryLight = Color(0xFFFFBE4D),
    scrim = Color(0xFF1A0F0A).copy(alpha = 0.7f),
    outline = Color(0xFFFF9E00).copy(alpha = 0.6f),
    outlineVariant = Color(0xFFFF9E00).copy(alpha = 0.3f),
    glow = Color(0xFFFF9E00),
    stageBackground = Color(0xFF120A06),
    stageFloor = Color(0xFF1A0F0A),
    stageHorizon = Color(0xFF3A2418),
    trussColor = Color(0xFF4A3020),
    trussBorder = Color(0xFF5A3C28),
    fixtureHousing = Color(0xFF2A1810),
    fixtureHousingBorder = Color(0xFF4A3020),
    scanlineColor = Color(0xFFFF9E00).copy(alpha = 0.02f),
    gridLineColor = Color(0xFFFF6B6B).copy(alpha = 0.04f),
)

/** Monochrome Pro — grayscale with single cyan accent. Minimal, professional. */
val MonochromeProColors = PixelColors(
    primary = Color(0xFFE0E0E0),
    onPrimary = Color(0xFF1A1A1A),
    secondary = Color(0xFF00BCD4),
    onSecondary = Color(0xFF1A1A1A),
    tertiary = Color(0xFF80DEEA),
    onTertiary = Color(0xFF1A1A1A),
    background = Color(0xFF1A1A1A),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF252525),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF303030),
    onSurfaceVariant = Color(0xFFE0E0E0).copy(alpha = 0.8f),
    error = Color(0xFFFF5252),
    onError = Color(0xFF1A1A1A),
    success = Color(0xFF69F0AE),
    warning = Color(0xFFFFD740),
    info = Color(0xFF00BCD4),
    onSurfaceDim = Color(0xFFE0E0E0).copy(alpha = 0.6f),
    primaryDark = Color(0xFFB0B0B0),
    primaryLight = Color(0xFFF0F0F0),
    scrim = Color.Black.copy(alpha = 0.6f),
    outline = Color(0xFFE0E0E0).copy(alpha = 0.5f),
    outlineVariant = Color(0xFFE0E0E0).copy(alpha = 0.25f),
    glow = Color(0xFF00BCD4),
    stageBackground = Color(0xFF111111),
    stageFloor = Color(0xFF1A1A1A),
    stageHorizon = Color(0xFF333333),
    trussColor = Color(0xFF3D3D3D),
    trussBorder = Color(0xFF505050),
    fixtureHousing = Color(0xFF2A2A2A),
    fixtureHousingBorder = Color(0xFF3D3D3D),
    scanlineColor = Color.White.copy(alpha = 0.02f),
    gridLineColor = Color.White.copy(alpha = 0.04f),
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
    PixelColorTheme.NeonCyberpunk -> NeonCyberpunkColors
    PixelColorTheme.OceanDepths -> OceanDepthsColors
    PixelColorTheme.SunsetWarm -> SunsetWarmColors
    PixelColorTheme.MonochromePro -> MonochromeProColors
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
     * All themes are dark except [PixelColorTheme.MatchaLight].
     *
     * Use this for conditional rendering (e.g., glow intensity, scanline color).
     */
    val isDarkTheme: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalPixelColorTheme.current != PixelColorTheme.MatchaLight

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
