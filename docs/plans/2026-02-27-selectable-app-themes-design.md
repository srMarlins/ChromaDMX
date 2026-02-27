# Selectable App Themes — Design Document

**Date:** 2026-02-27
**Issue:** Settings theme picker with 7 selectable themes + full stage infrastructure theming

## Summary

Wire the existing theme persistence infrastructure (database column, repository flow, `ChromaDmxTheme` composable) to the settings UI, add 4 new theme palettes, expand `PixelColors` with stage infrastructure colors, and replace all hardcoded stage/renderer colors with theme-aware references.

## Theme Palettes (7 total)

### Existing (add stage infrastructure colors)

| Theme | Background | Primary | Secondary | Dark? |
|-------|-----------|---------|-----------|-------|
| **MatchaDark** | `#1B261D` | `#9CCC65` (matcha leaf) | `#F48FB1` (sakura pink) | Yes |
| **MatchaLight** | `#F5F5F0` | `#558B2F` (dark matcha) | `#AD1457` (deep sakura) | No |
| **HighContrast** | `#000000` | `#FFFF00` (bright yellow) | `#00FFFF` (cyan) | Yes |

### New

| Theme | Background | Primary | Secondary | Dark? |
|-------|-----------|---------|-----------|-------|
| **NeonCyberpunk** | `#0D0221` (deep purple-black) | `#00D4FF` (electric blue) | `#FF2E97` (hot pink) | Yes |
| **OceanDepths** | `#03045E` (deep navy) | `#00B4D8` (teal) | `#90E0EF` (aquamarine) | Yes |
| **SunsetWarm** | `#1A0F0A` (warm dark brown) | `#FF9E00` (amber) | `#FF6B6B` (coral) | Yes |
| **MonochromePro** | `#1A1A1A` (charcoal) | `#E0E0E0` (cool white) | `#00BCD4` (cyan accent) | Yes |

## New PixelColors Fields

Add 9 stage infrastructure fields to `PixelColors`:

```kotlin
val stageBackground: Color    // Canvas/stage background (was 0xFF060612)
val stageFloor: Color          // Floor plane color (was 0xFF0A0A14)
val stageHorizon: Color        // Horizon line (was 0xFF1A1A30)
val trussColor: Color          // Truss bar (was 0xFF2A2A3E)
val trussBorder: Color         // Truss highlight/edge (was 0xFF3A3A52)
val fixtureHousing: Color      // Fixture body (was 0xFF1A1A2E)
val fixtureHousingBorder: Color // Fixture body edge (was 0xFF2A2A3E)
val scanlineColor: Color       // CRT scanline overlay (was White @ 0.02 alpha)
val gridLineColor: Color       // Canvas grid lines (was White @ 0.05 alpha)
```

Each theme provides contextually appropriate values:
- Dark themes: dark-tinted infrastructure matching the theme's hue
- MatchaLight: lighter grays/whites for stage elements
- HighContrast: pure white lines, high-visibility housing
- NeonCyberpunk: purple-tinted stage, neon grid lines
- OceanDepths: deep blue stage elements
- SunsetWarm: warm brown/amber tints
- MonochromePro: neutral grays

## Wiring: Settings → Persistence → Theme

### 1. SettingsContract.kt

Add to `SettingsUiState`:
```kotlin
val themePreference: PixelColorTheme = PixelColorTheme.MatchaDark,
```

Add to `SettingsEvent`:
```kotlin
data class SetThemePreference(val theme: PixelColorTheme) : SettingsEvent
```

### 2. SettingsViewModelV2.kt

- Collect `settingsRepository.themePreference` flow in `init`, map string to enum, update state
- Handle `SetThemePreference` event: update state + call `settingsRepository.setThemePreference(theme.name)`
- Expose `themePreference: StateFlow<PixelColorTheme>` (separate from UI state, for ChromaDmxApp to collect)

### 3. ChromaDmxApp.kt

- Resolve `SettingsStore` from Koin (already available)
- Collect `settingsRepository.themePreference` as `State<PixelColorTheme>`
- Pass to `ChromaDmxTheme(colorTheme = currentTheme)`

### 4. SettingsScreen.kt

Add an **Appearance** section as the first section in the LazyColumn:

```
┌─────────────────────────────────────────┐
│ Appearance                              │
│                                         │
│ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐   │
│ │██████│ │██████│ │██████│ │██████│   │
│ │██████│ │██████│ │██████│ │██████│   │
│ │ Matcha│ │ Matcha│ │ Hi-Con│ │ Cyber │   │
│ │ Dark  │ │ Light │ │ trast │ │ punk  │   │
│ └──────┘ └──────┘ └──────┘ └──────┘   │
│                                         │
│ ┌──────┐ ┌──────┐ ┌──────┐            │
│ │██████│ │██████│ │██████│            │
│ │██████│ │██████│ │██████│            │
│ │ Ocean │ │Sunset │ │ Mono  │            │
│ │Depths │ │ Warm  │ │  Pro  │            │
│ └──────┘ └──────┘ └──────┘            │
└─────────────────────────────────────────┘
```

Each swatch chip shows a mini rectangle with the theme's background, primary, and secondary colors stacked. Selected theme gets a highlighted border (`PixelDesign.colors.primary`).

## Stage Renderer Updates

### AudienceView.kt
Replace 8 hardcoded colors:
- `Color(0xFF060612)` → `PixelDesign.colors.stageBackground`
- `Color(0xFF0A0A14)` → `PixelDesign.colors.stageFloor`
- `Color(0xFF1A1A30)` → `PixelDesign.colors.stageHorizon`
- `Color(0xFF2A2A3E)` → `PixelDesign.colors.trussColor`
- `Color(0xFF3A3A52)` → `PixelDesign.colors.trussBorder`
- `Color(0xFF3E3E56)` → `PixelDesign.colors.trussBorder` (bracket mount)
- `Color(0xFF1A1A2E)` → `PixelDesign.colors.fixtureHousing`
- `Color(0xFF333344)` → `PixelDesign.colors.fixtureHousing.copy(alpha = 0.8f)` (idle glow)

### VenueCanvas.kt
- Scanline `Color.White.copy(alpha = 0.02f)` → `PixelDesign.colors.scanlineColor`
- Grid `Color.White.copy(alpha = 0.05f)` → `PixelDesign.colors.gridLineColor`
- Truss `Color(0xFF2A2A3E)` → `PixelDesign.colors.trussColor`

### TopDownRenderer.kt
- `HousingColor = Color(0xFF1A1A2E)` → pass `PixelDesign.colors.fixtureHousing` from composable call site
- `HousingBorderColor = Color(0xFF2A2A3E)` → pass `PixelDesign.colors.fixtureHousingBorder`

### StagePreviewScreen.kt
- `Color(0x88000000)` → `PixelDesign.colors.scrim`

## Onboarding Legacy Colors

In `SetupScreen.kt`, replace:
- `NeonCyan` → `PixelDesign.colors.secondary` (or `info`)
- `NeonMagenta` → `PixelDesign.colors.secondary`
- `NeonGreen` → `PixelDesign.colors.primary`
- `NeonYellow` → `PixelDesign.colors.tertiary`

## isDarkTheme Update

Update `PixelDesign.isDarkTheme` to handle new themes:
```kotlin
get() = when (LocalPixelColorTheme.current) {
    PixelColorTheme.MatchaLight -> false
    else -> true  // All others are dark
}
```

Similarly update `ChromaDmxTheme` dark/light color scheme selection.

## Files Modified

| File | Change |
|------|--------|
| `PixelDesignSystem.kt` | Add 4 enum entries, 9 PixelColors fields, 4 palette instances, update `toColors()` |
| `ChromaDmxTheme.kt` | Update `isDark` when-expression for new themes |
| `SettingsContract.kt` | Add `themePreference` to state, `SetThemePreference` event |
| `SettingsViewModelV2.kt` | Collect theme flow, handle event, expose for app root |
| `SettingsScreen.kt` | Add `AppearanceSection` with theme picker grid |
| `ChromaDmxApp.kt` | Collect theme preference, pass to `ChromaDmxTheme` |
| `AudienceView.kt` | Replace 8 hardcoded colors |
| `VenueCanvas.kt` | Replace 3 hardcoded colors |
| `TopDownRenderer.kt` | Replace 2 hardcoded constants |
| `StagePreviewScreen.kt` | Replace 1 hardcoded color |
| `SetupScreen.kt` | Replace 4 legacy neon color imports |
| `Color.kt` | (Optional) Remove unused neon constants after migration |
