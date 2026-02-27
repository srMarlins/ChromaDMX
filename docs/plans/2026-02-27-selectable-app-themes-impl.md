# Selectable App Themes — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add 7 selectable app themes to the settings screen with full stage infrastructure theming.

**Architecture:** Expand `PixelColorTheme` enum with 4 new entries, add 9 stage infrastructure fields to `PixelColors`, wire the existing persistence layer (`SettingsStore.themePreference`) through `SettingsViewModelV2` to `ChromaDmxApp`, add a theme picker section to the settings UI, and replace all hardcoded stage colors with theme-aware references.

**Tech Stack:** Kotlin Multiplatform, Jetpack Compose, SQLDelight (persistence), Koin (DI)

---

### Task 1: Add stage infrastructure color fields to PixelColors

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/theme/PixelDesignSystem.kt`

**Step 1: Add 9 new fields to `PixelColors` data class**

In `PixelDesignSystem.kt`, add these fields to the `PixelColors` data class after the `glow` field (line 68):

```kotlin
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
```

**Step 2: Add stage colors to MatchaLightColors**

In the `MatchaLightColors` instance (around line 77), add before the closing paren:

```kotlin
    glow = Color(0xFF558B2F),
    // Stage — lighter infrastructure for bright theme
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
```

**Step 3: Add stage colors to HighContrastColors**

In the `HighContrastColors` instance (around line 105), add before the closing paren:

```kotlin
    glow = Color(0xFFFFFF00),
    // Stage — high-visibility infrastructure
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
```

**Step 4: Compile check**

Run: `./gradlew :shared:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/chromadmx/ui/theme/PixelDesignSystem.kt
git commit -m "feat: add stage infrastructure color fields to PixelColors"
```

---

### Task 2: Add 4 new theme enum entries and palette instances

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/theme/PixelDesignSystem.kt`

**Step 1: Add enum entries**

In the `PixelColorTheme` enum (line 18), add 4 new entries:

```kotlin
enum class PixelColorTheme {
    MatchaDark,
    MatchaLight,
    HighContrast,
    NeonCyberpunk,
    OceanDepths,
    SunsetWarm,
    MonochromePro,
}
```

**Step 2: Add NeonCyberpunkColors palette**

After `HighContrastColors`, add:

```kotlin
/** Neon Cyberpunk — electric blues, hot pinks, deep purple-black. */
val NeonCyberpunkColors = PixelColors(
    primary = Color(0xFF00D4FF),             // Electric blue
    onPrimary = Color(0xFF0D0221),
    secondary = Color(0xFFFF2E97),           // Hot pink
    onSecondary = Color(0xFF0D0221),
    tertiary = Color(0xFFBF00FF),            // Neon violet
    onTertiary = Color(0xFF0D0221),
    background = Color(0xFF0D0221),          // Deep purple-black
    onBackground = Color(0xFFE0D4FF),
    surface = Color(0xFF1A0A3E),             // Dark purple
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
```

**Step 3: Add OceanDepthsColors palette**

```kotlin
/** Ocean Depths — deep navy, teal, aquamarine. Calm underwater. */
val OceanDepthsColors = PixelColors(
    primary = Color(0xFF00B4D8),             // Teal
    onPrimary = Color(0xFF03045E),
    secondary = Color(0xFF90E0EF),           // Aquamarine
    onSecondary = Color(0xFF03045E),
    tertiary = Color(0xFF48CAE4),            // Sky blue
    onTertiary = Color(0xFF03045E),
    background = Color(0xFF03045E),          // Deep navy
    onBackground = Color(0xFFCAF0F8),
    surface = Color(0xFF0A1128),             // Dark blue
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
```

**Step 4: Add SunsetWarmColors palette**

```kotlin
/** Sunset Warm — amber, coral, warm dark browns. Golden hour. */
val SunsetWarmColors = PixelColors(
    primary = Color(0xFFFF9E00),             // Amber
    onPrimary = Color(0xFF1A0F0A),
    secondary = Color(0xFFFF6B6B),           // Coral
    onSecondary = Color(0xFF1A0F0A),
    tertiary = Color(0xFFFFD54F),            // Warm yellow
    onTertiary = Color(0xFF1A0F0A),
    background = Color(0xFF1A0F0A),          // Warm dark brown
    onBackground = Color(0xFFFFE0C0),
    surface = Color(0xFF2A1A10),             // Dark timber
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
```

**Step 5: Add MonochromeProColors palette**

```kotlin
/** Monochrome Pro — grayscale with single cyan accent. Minimal, professional. */
val MonochromeProColors = PixelColors(
    primary = Color(0xFFE0E0E0),             // Cool white
    onPrimary = Color(0xFF1A1A1A),
    secondary = Color(0xFF00BCD4),           // Cyan accent
    onSecondary = Color(0xFF1A1A1A),
    tertiary = Color(0xFF80DEEA),            // Light cyan
    onTertiary = Color(0xFF1A1A1A),
    background = Color(0xFF1A1A1A),          // Charcoal
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF252525),             // Dark gray
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
```

**Step 6: Update toColors() resolver**

Replace the `toColors()` function:

```kotlin
fun PixelColorTheme.toColors(): PixelColors = when (this) {
    PixelColorTheme.MatchaDark -> MatchaDarkColors
    PixelColorTheme.MatchaLight -> MatchaLightColors
    PixelColorTheme.HighContrast -> HighContrastColors
    PixelColorTheme.NeonCyberpunk -> NeonCyberpunkColors
    PixelColorTheme.OceanDepths -> OceanDepthsColors
    PixelColorTheme.SunsetWarm -> SunsetWarmColors
    PixelColorTheme.MonochromePro -> MonochromeProColors
}
```

**Step 7: Update isDarkTheme in PixelDesign**

In `PixelDesign.isDarkTheme`, replace the when-expression:

```kotlin
    val isDarkTheme: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalPixelColorTheme.current != PixelColorTheme.MatchaLight
```

**Step 8: Compile check**

Run: `./gradlew :shared:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 9: Commit**

```bash
git add shared/src/commonMain/kotlin/com/chromadmx/ui/theme/PixelDesignSystem.kt
git commit -m "feat: add 4 new theme palettes (Cyberpunk, Ocean, Sunset, Monochrome)"
```

---

### Task 3: Update ChromaDmxTheme for new themes

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/theme/ChromaDmxTheme.kt`

**Step 1: Simplify isDark check**

In `ChromaDmxTheme.kt` (line 45-49), replace the when-expression:

```kotlin
    val isDark = colorTheme != PixelColorTheme.MatchaLight
```

**Step 2: Compile check**

Run: `./gradlew :shared:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/chromadmx/ui/theme/ChromaDmxTheme.kt
git commit -m "refactor: simplify isDark check for new themes"
```

---

### Task 4: Add theme preference to settings contract and ViewModel

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/state/SettingsContract.kt`
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/viewmodel/SettingsViewModelV2.kt`

**Step 1: Add import and field to SettingsUiState**

In `SettingsContract.kt`, add import:

```kotlin
import com.chromadmx.ui.theme.PixelColorTheme
```

Add `themePreference` field to `SettingsUiState` (after `dataTransferStatus`):

```kotlin
    // Theme
    val themePreference: PixelColorTheme = PixelColorTheme.MatchaDark,
```

**Step 2: Add SetThemePreference event**

Add to `SettingsEvent`:

```kotlin
    data class SetThemePreference(val theme: PixelColorTheme) : SettingsEvent
```

**Step 3: Wire SettingsViewModelV2**

In `SettingsViewModelV2.kt`, add import:

```kotlin
import com.chromadmx.ui.theme.PixelColorTheme
```

In the `init` block, add a collector for theme preference (after the `isSimulation` collector):

```kotlin
        // Derive theme from the persisted preference.
        scope.launch {
            settingsRepository.themePreference.collect { name ->
                val theme = PixelColorTheme.entries.firstOrNull { it.name == name }
                    ?: PixelColorTheme.MatchaDark
                _state.update { it.copy(themePreference = theme) }
            }
        }
```

In `onEvent()`, add the new event handler (after `DismissDataTransferStatus`):

```kotlin
            is SettingsEvent.SetThemePreference ->
                setThemePreference(event.theme)
```

Add the handler method:

```kotlin
    private fun setThemePreference(theme: PixelColorTheme) {
        _state.update { it.copy(themePreference = theme) }
        scope.launch {
            settingsRepository.setThemePreference(theme.name)
        }
    }
```

**Step 4: Compile check**

Run: `./gradlew :shared:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/chromadmx/ui/state/SettingsContract.kt \
        shared/src/commonMain/kotlin/com/chromadmx/ui/viewmodel/SettingsViewModelV2.kt
git commit -m "feat: wire theme preference through settings contract and ViewModel"
```

---

### Task 5: Wire theme into ChromaDmxApp root

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/ChromaDmxApp.kt`

**Step 1: Collect theme from SettingsStore and pass to ChromaDmxTheme**

In `ChromaDmxApp.kt`, add imports:

```kotlin
import com.chromadmx.core.persistence.SettingsStore
import com.chromadmx.ui.theme.PixelColorTheme
```

Inside `ChromaDmxApp()`, before the `ChromaDmxTheme {` call, resolve the settings store and collect the theme:

```kotlin
@Composable
fun ChromaDmxApp() {
    val settingsStore = resolveOrNull<SettingsStore>()
    val themeName by settingsStore?.themePreference?.collectAsState(initial = PixelColorTheme.MatchaDark.name)
        ?: remember { mutableStateOf(PixelColorTheme.MatchaDark.name) }
    val currentTheme = remember(themeName) {
        PixelColorTheme.entries.firstOrNull { it.name == themeName }
            ?: PixelColorTheme.MatchaDark
    }

    ChromaDmxTheme(colorTheme = currentTheme) {
```

Add the missing import for `mutableStateOf`:

```kotlin
import androidx.compose.runtime.mutableStateOf
```

**Step 2: Compile check**

Run: `./gradlew :shared:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/chromadmx/ui/ChromaDmxApp.kt
git commit -m "feat: wire persisted theme preference into ChromaDmxTheme root"
```

---

### Task 6: Add theme picker UI to Settings screen

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/settings/SettingsScreen.kt`

**Step 1: Add theme display name helper**

At the bottom of `SettingsScreen.kt`, add a helper function:

```kotlin
/** User-friendly display name for each theme. */
private fun PixelColorTheme.displayName(): String = when (this) {
    PixelColorTheme.MatchaDark -> "Matcha Dark"
    PixelColorTheme.MatchaLight -> "Matcha Light"
    PixelColorTheme.HighContrast -> "High Contrast"
    PixelColorTheme.NeonCyberpunk -> "Cyberpunk"
    PixelColorTheme.OceanDepths -> "Ocean"
    PixelColorTheme.SunsetWarm -> "Sunset"
    PixelColorTheme.MonochromePro -> "Mono Pro"
}
```

**Step 2: Add AppearanceSection composable**

Add imports at the top:

```kotlin
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.chromadmx.ui.theme.PixelColorTheme
```

Add the section composable:

```kotlin
@Composable
private fun AppearanceSection(
    selectedTheme: PixelColorTheme,
    onEvent: (SettingsEvent) -> Unit,
) {
    PixelCard(
        title = { PixelSectionTitle(title = "Appearance") },
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PixelDesign.spacing.small),
            verticalArrangement = Arrangement.spacedBy(PixelDesign.spacing.small),
        ) {
            PixelColorTheme.entries.forEach { theme ->
                ThemeChip(
                    theme = theme,
                    isSelected = theme == selectedTheme,
                    onClick = { onEvent(SettingsEvent.SetThemePreference(theme)) },
                )
            }
        }
    }
}

@Composable
private fun ThemeChip(
    theme: PixelColorTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = theme.toColors()
    val shape = RoundedCornerShape(8.dp)
    val borderColor = if (isSelected) PixelDesign.colors.primary else PixelDesign.colors.outlineVariant
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Column(
        modifier = Modifier
            .width(80.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Color swatch: background + primary + secondary stacked
        androidx.compose.material3.Surface(
            modifier = Modifier.size(72.dp, 48.dp),
            shape = shape,
            border = BorderStroke(borderWidth, borderColor),
            color = colors.background,
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(colors.background),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(colors.primary),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(colors.secondary),
                )
            }
        }
        Text(
            text = theme.displayName(),
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) PixelDesign.colors.primary else PixelDesign.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
```

Add `toColors` import:

```kotlin
import com.chromadmx.ui.theme.toColors
```

**Step 3: Wire AppearanceSection into the LazyColumn**

In the `SettingsScreen` composable, add the Appearance section as the **first item** in the LazyColumn (before Network):

```kotlin
            // Appearance section (theme picker)
            item { AppearanceSection(state.themePreference, viewModel::onEvent) }
            // Network section
            item { NetworkSection(state, viewModel::onEvent) }
```

**Step 4: Add missing imports for `fillMaxSize` and `background` in Row/Box**

These should already be imported. Verify `fillMaxSize`, `background`, `Box`, `Row` are imported.

**Step 5: Compile check**

Run: `./gradlew :shared:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/com/chromadmx/ui/screen/settings/SettingsScreen.kt
git commit -m "feat: add theme picker UI to settings screen"
```

---

### Task 7: Replace hardcoded colors in AudienceView

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/components/AudienceView.kt`

**Step 1: Remove file-level color constants**

Delete lines 22-47 (the 9 `private val` constants: `StageBackground`, `TrussColor`, `TrussHighlight`, `BracketColor`, `FloorColor`, `HorizonColor`, `HousingColor`, `HousingBorderColor`, `IdleGlowColor`).

**Step 2: Add theme import**

Ensure this import exists:
```kotlin
import com.chromadmx.ui.theme.PixelDesign
```

**Step 3: Read colors from theme inside the composable**

In the `@Composable` `AudienceView` function, at the top of the composable body (before the Canvas call), read the theme colors:

```kotlin
    val stageBackground = PixelDesign.colors.stageBackground
    val stageFloor = PixelDesign.colors.stageFloor
    val stageHorizon = PixelDesign.colors.stageHorizon
    val trussColor = PixelDesign.colors.trussColor
    val trussBorder = PixelDesign.colors.trussBorder
    val bracketColor = PixelDesign.colors.trussBorder
    val housingColor = PixelDesign.colors.fixtureHousing
    val housingBorderColor = PixelDesign.colors.fixtureHousingBorder
    val idleGlowColor = PixelDesign.colors.fixtureHousing.copy(alpha = 0.8f)
```

Then replace all references to the old constants (`StageBackground` → `stageBackground`, etc.) throughout the file. The variable names are lowercase versions of the originals, so the references in the `DrawScope` code need updating.

Also replace the hardcoded gradient colors (lines ~95 and ~110 in the background gradient):
- `Color(0xFF08081A)` → `stageBackground.copy(alpha = 0.95f)` (ambient sky tint)
- `Color(0xFF080810)` → `stageFloor` (deeper at bottom)

**Step 4: Compile check**

Run: `./gradlew :shared:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/chromadmx/ui/components/AudienceView.kt
git commit -m "refactor: replace hardcoded colors in AudienceView with theme references"
```

---

### Task 8: Replace hardcoded colors in VenueCanvas

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/components/VenueCanvas.kt`

**Step 1: Remove file-level color constants**

Delete lines 38-45 (the 3 `private val` constants: `ScanlineColor`, `GridLineColor`, `TrussColor`).

**Step 2: Read colors from theme in composable**

Inside the `VenueCanvas` composable, near the existing `canvasBg`/`selectionColor` reads (around line 87), add:

```kotlin
    val scanlineColor = PixelDesign.colors.scanlineColor
    val gridLineColor = PixelDesign.colors.gridLineColor
    val trussColor = PixelDesign.colors.trussColor
    val housingColor = PixelDesign.colors.fixtureHousing
    val housingBorderColor = PixelDesign.colors.fixtureHousingBorder
```

**Step 3: Replace references**

Throughout the Canvas lambda, replace:
- `ScanlineColor` → `scanlineColor`
- `GridLineColor` → `gridLineColor`
- `TrussColor` → `trussColor`

**Step 4: Compile check**

Run: `./gradlew :shared:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/chromadmx/ui/components/VenueCanvas.kt
git commit -m "refactor: replace hardcoded colors in VenueCanvas with theme references"
```

---

### Task 9: Replace hardcoded colors in TopDownRenderer

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/renderer/TopDownRenderer.kt`
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/components/VenueCanvas.kt` (call sites)

The TopDownRenderer functions are `DrawScope` extensions (not `@Composable`), so colors must be passed as parameters.

**Step 1: Add housingColor and housingBorderColor params to all draw functions**

For each of the 5 draw functions (`drawParFixture`, `drawStrobeFixture`, `drawWashFixture`, `drawBarFixture`, `drawBeamConeFixture`), add two parameters:

```kotlin
    fun DrawScope.drawParFixture(
        cx: Float,
        cy: Float,
        color: Color,
        isSelected: Boolean,
        selectionColor: Color,
        scale: Float = 1f,
        housingColor: Color = HousingColor,
        housingBorderColor: Color = HousingBorderColor,
    )
```

Keep the object-level `HousingColor` and `HousingBorderColor` as defaults so existing call sites don't break during transition.

Inside each function body, the references to `HousingColor` and `HousingBorderColor` already match the parameter names — just make sure the parameter names shadow the object vals.

Wait — the parameter name `housingColor` is lowercase but the object val is `HousingColor` (uppercase). So inside each function, replace:
- `HousingColor` → `housingColor`
- `HousingBorderColor` → `housingBorderColor`

**Step 2: Update VenueCanvas call sites**

In `VenueCanvas.kt`, pass the theme colors through at each call site (around lines 272-281):

```kotlin
FixtureType.STROBE -> drawStrobeFixture(cx, cy, composeColor, isSelected, selectionColor, fixtureScale, housingColor, housingBorderColor)
FixtureType.WASH -> drawWashFixture(cx, cy, composeColor, isSelected, selectionColor, fixtureScale, housingColor, housingBorderColor)
else -> drawParFixture(cx, cy, composeColor, isSelected, selectionColor, fixtureScale, housingColor, housingBorderColor)
```

For `drawBarFixture`:
```kotlin
drawBarFixture(cx, cy, composeColor, pixelCount, isSelected, selectionColor, fixtureScale, housingColor, housingBorderColor)
```

For `drawBeamConeFixture`:
```kotlin
drawBeamConeFixture(cx, cy, composeColor, isSelected, reusablePath, selectionColor, fixtureScale, housingColor, housingBorderColor)
```

Note: `housingColor` and `housingBorderColor` were already captured from `PixelDesign.colors` in Task 8.

**Step 3: Check for other call sites**

Search for any other files that call `TopDownRenderer.draw*` and update them similarly. Likely candidates: `TopDownEditor.kt` or `FixtureEditOverlay.kt`.

**Step 4: Compile check**

Run: `./gradlew :shared:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/chromadmx/ui/renderer/TopDownRenderer.kt \
        shared/src/commonMain/kotlin/com/chromadmx/ui/components/VenueCanvas.kt
git commit -m "refactor: parameterize TopDownRenderer housing colors for theming"
```

---

### Task 10: Replace hardcoded color in StagePreviewScreen

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/stage/StagePreviewScreen.kt`

**Step 1: Replace Color(0x88000000)**

Around line 241, replace:

```kotlin
.background(Color(0x88000000))
```

with:

```kotlin
.background(PixelDesign.colors.scrim)
```

Ensure `PixelDesign` is imported.

**Step 2: Compile check**

Run: `./gradlew :shared:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/chromadmx/ui/screen/stage/StagePreviewScreen.kt
git commit -m "refactor: replace hardcoded scrim in StagePreviewScreen with theme color"
```

---

### Task 11: Replace legacy neon colors in SetupScreen

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/setup/SetupScreen.kt`

**Step 1: Remove neon color imports**

Delete these import lines:
```kotlin
import com.chromadmx.ui.theme.NeonCyan
import com.chromadmx.ui.theme.NeonGreen
import com.chromadmx.ui.theme.NeonMagenta
import com.chromadmx.ui.theme.NeonYellow
```

Ensure `PixelDesign` is imported:
```kotlin
import com.chromadmx.ui.theme.PixelDesign
```

**Step 2: Replace all neon color usages**

Apply these replacements throughout the file:
- `NeonCyan` → `PixelDesign.colors.info`
- `NeonGreen` → `PixelDesign.colors.success`
- `NeonMagenta` → `PixelDesign.colors.secondary`
- `NeonYellow` → `PixelDesign.colors.warning`

For inline hex colors used in non-composable contexts (like the V-formation preview pixel colors around lines 680-682, 827-829):
- `Color(0xFF00FBFF)` → `PixelDesign.colors.info`
- `Color(0xFFFF00FF)` → `PixelDesign.colors.secondary`
- `Color(0xFF00FF00)` → `PixelDesign.colors.success`

**Important:** These replacements only work inside `@Composable` functions where `PixelDesign.colors` is accessible. If any neon color usage is inside a non-composable function or a `remember {}` lambda, capture the color in a composable-scope `val` first and pass it in.

**Step 3: Compile check**

Run: `./gradlew :shared:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/chromadmx/ui/screen/setup/SetupScreen.kt
git commit -m "refactor: replace legacy neon colors in SetupScreen with theme colors"
```

---

### Task 12: Final compile + build verification

**Files:** None (verification only)

**Step 1: Full compile**

Run: `./gradlew :shared:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 2: Android build**

Run: `./gradlew :android:app:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 3: Run shared module tests**

Run: `./gradlew :shared:core:testAndroidHostTest :shared:engine:testAndroidHostTest :shared:simulation:testAndroidHostTest`
Expected: All tests pass (these modules don't touch UI but verify no transitive breakage)

**Step 4: Commit any fixes**

If any compilation or test issues were found, fix and commit them.
