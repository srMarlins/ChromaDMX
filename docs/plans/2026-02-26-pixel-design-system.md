# ChromaDMX Pixel Design System

**Date:** 2026-02-26
**Status:** Approved
**Scope:** Design system component library only — screen architecture/UX rethink comes in a follow-up phase

## Overview

A complete rebuild of ChromaDMX's visual design system, inspired by MtgPirate's proven pixel-art approach. The core insight: achieve the pixel aesthetic through **shape** (chamfered corners), **decoration** (glowing borders, particles, scanlines), and **typography styling** (monospace + UPPERCASE + letter spacing) — not through bitmap pixel fonts or low-resolution rendering.

**Design principles:**
- "Look retro, behave modern" — pixel aesthetic is visual, interaction model is contemporary
- Readability is non-negotiable — system monospace, never bitmap fonts
- Theme-swappable — dark, light, high contrast, and extensible
- Reduced motion respected — all animations gate on system accessibility setting
- Every color through tokens — no raw hex values in components or screens

## Section 1: Foundation — Shape, Border & Theming

### PixelShape

Custom `Shape` implementation with chamfered (diagonal-cut) corners creating octagonal outlines. Two standard sizes:

- `PixelShape(6.dp)` — small components (badges, text fields, toggles, chips)
- `PixelShape(9.dp)` — large components (buttons, cards, dialogs, bottom sheets)

Every interactive and container element uses this shape. No rounded corners anywhere. All strokes use `StrokeCap.Square` + `StrokeJoin.Miter` for sharp pixel-perfect edges.

### Modifier.pixelBorder()

Custom draw modifier with three layers:

1. **Outer glow** — wide semi-transparent stroke (glow color at 15-25% alpha, optionally animated)
2. **Main border** — visible pixel border at 60% opacity
3. **Inner shadow** — subtle depth at 15% black

Variants:
- `pixelBorder()` — standard static border
- `pixelBorderGlowing()` — animated glow pulse (1.5s cycle, 0.15→0.5 alpha)
- `pixelBorderActive()` — brighter glow for focus/selection states

### Color Architecture (theme-swappable)

`PixelColors` is an `@Immutable` data class that any theme implements. Full token set:

```
PixelColors:
  primary, onPrimary
  primaryDark, primaryLight
  secondary, onSecondary
  tertiary, onTertiary
  error, onError
  info, success, warning
  background, onBackground
  surface, onSurface, onSurfaceDim
  surfaceVariant, onSurfaceVariant
  outline, outlineVariant
  scrim (overlay backgrounds)
  glow (ambient animation color, usually = primary)
```

**Built-in themes:**

| Theme | Background | Surface | Primary | Text |
|-------|-----------|---------|---------|------|
| MatchaDark (default) | `#1B261D` | `#263228` | `#9CCC65` | `#DCEDC8` |
| MatchaLight | Mint/cream | White/pale green | Darker greens | Dark green/black |
| HighContrast | Pure black | Pure black | Bright accent | Pure white |

**Theme switching** via `CompositionLocalProvider`:
```kotlin
ChromaDmxTheme(colorTheme: PixelColorTheme = PixelColorTheme.MatchaDark) {
    // PixelDesign.colors resolves to the active theme's PixelColors
}
```

Adding a new theme = define a new `PixelColors` instance. No code changes anywhere else.

**Light mode specifics:**
- Borders darken instead of glow
- Glow effects reduce opacity
- Scanline overlay uses dark lines at 2% alpha

**High contrast specifics:**
- Pure black backgrounds, pure white text
- Borders at full 100% opacity, no glow effects
- All decorative animations disabled
- Larger minimum touch targets

**System integration:**
- `isSystemInDarkTheme()` for system preference
- Respect Android accessibility high contrast setting
- User manual theme choice stored in preferences (overrides system)
- `PixelDesign.isDarkTheme: Boolean` exposed for conditional rendering

### Spacing Tokens

```
PixelSpacing:
  extraSmall: 4.dp
  small: 8.dp
  medium: 16.dp
  large: 24.dp
  extraLarge: 32.dp
  screenPadding: 16.dp
  componentPadding: 12.dp
  sectionGap: 20.dp
  pixelSize: 4.dp (base unit for borders)
```

## Section 2: Typography & Decorators

### Font System

Single font family: `FontFamily.Monospace` (system monospace). No pixel bitmap fonts. The retro feel comes entirely from styling.

| Style | Size | Weight | Letter Spacing | Transform | Usage |
|-------|------|--------|----------------|-----------|-------|
| `displayLarge` | 32sp | Bold | 2.0sp | UPPERCASE | App branding, splash |
| `displayMedium` | 28sp | Bold | 1.5sp | UPPERCASE | — |
| `headlineLarge` | 24sp | Bold | 1.0sp | UPPERCASE | Screen titles |
| `headlineMedium` | 20sp | Bold | 1.0sp | UPPERCASE | Section headers |
| `headlineSmall` | 18sp | Bold | 0.5sp | UPPERCASE | Sub-headers |
| `titleLarge` | 16sp | Bold | 0.5sp | UPPERCASE | Card titles |
| `titleMedium` | 14sp | Bold | 0.5sp | — | Labels |
| `bodyLarge` | 16sp | Normal | 0.25sp | — | Primary body text |
| `bodyMedium` | 14sp | Normal | 0.25sp | — | Secondary body text |
| `bodySmall` | 12sp | Normal | 0.25sp | — | Tertiary text |
| `labelLarge` | 14sp | Bold | 1.0sp | UPPERCASE | Button labels |
| `labelMedium` | 12sp | Bold | 0.5sp | UPPERCASE | Badges, tags |
| `labelSmall` | 11sp | Normal | 0.4sp | UPPERCASE | Captions, metadata |

Key rules:
- All headers and labels UPPERCASE (enforced in text style)
- Wide letter spacing on Bold text creates "pixel font" illusion
- Body text: normal case, tighter spacing for readability
- All sizes in `sp` — respects user font scaling
- Line height: 1.5x for pixel-styled text, 1.4x for body

### Unicode Decorators

| Constant | Character | Usage |
|----------|-----------|-------|
| `SECTION_PREFIX` | `"▸ "` | Screen titles: `▸ STAGE VIEW` |
| `SUB_PREFIX` | `"└─ "` | Descriptions under titles |
| `LABEL_PREFIX` | `"✦ "` | Input field labels: `✦ IP ADDRESS` |
| `BULLET` | `"▪ "` | List items |
| `CURSOR` | `"█"` | Blinking cursor after headers |

Applied via helper composables (`PixelSectionTitle`, `PixelLabel`), never manually by callers.

### Text Contrast Guarantees
- `onBackground` on `background`: minimum 7:1 (WCAG AAA)
- `onSurface` on `surface`: minimum 4.5:1 (WCAG AA)
- `onSurfaceDim` on `surface`: minimum 3:1 (large text only)

## Section 3: Animation & Effects

### Animation Presets

Centralized `ChromaAnimations` object. No ad-hoc spring/tween values in components.

**Spring presets (interactive):**

| Preset | Damping | Stiffness | Usage |
|--------|---------|-----------|-------|
| `buttonPress` | LowBouncy (0.75) | Medium (1500) | Button/toggle press |
| `panelSlide` | NoBouncy (1.0) | MediumLow (400) | Overlays, panels |
| `mascotBounce` | MediumBouncy (0.5) | Low (200) | Mascot, playful elements |
| `beatPulse` | NoBouncy (1.0) | High (10000) | Beat-reactive snaps |
| `cardExpand` | LowBouncy (0.75) | MediumLow (400) | Card expand/collapse |
| `dragReturn` | MediumBouncy (0.5) | Medium (1500) | Drag release snap-back |

**Tween presets (continuous/looping):**

| Preset | Duration | Easing | Usage |
|--------|----------|--------|-------|
| `glowPulse` | 1500ms | FastOutSlowIn, reversing | Border glow breathing |
| `sparkleOrbit` | 3000ms | Linear, infinite | Sparkle particle orbits |
| `shimmerSweep` | 2000ms | Linear, infinite | Divider shimmer gradient |
| `cursorBlink` | 500ms | Linear, reversing | Blinking cursor alpha |
| `scanlineDrift` | 8000ms | Linear, infinite | Scanline vertical scroll |
| `starTwinkle` | 1000ms | FastOutSlowIn, reversing | Star brightness pulse |
| `starRotate` | 4000ms | Linear, infinite | Star rotation |

### Reduced Motion

When system reduced motion is enabled:
- Springs become instant (VeryHigh stiffness, no bounce)
- Continuous animations stop (glow at static 50%, no sparkles, no scanlines)
- Only functional state transitions remain

`PixelDesign.reduceMotion: Boolean` — all animation composables check this.

### Ambient Effects

**CRT Scanlines** — Full-screen overlay:
- Horizontal lines every 4px, primary color at 3% alpha
- Slow vertical drift (8s cycle)
- Light themes: dark lines at 2% alpha

**Sparkle Particles** — Decorative floating particles:
- 4-8 particles orbiting in circles (3s cycle)
- Size oscillates sinusoidally with glow halo
- Color: `PixelDesign.colors.tertiary`
- Used on: loading states, active cards, celebration moments

**Pulsing Glow** — via `pixelBorderGlowing()`:
- Border glow alpha oscillates 0.15 → 0.5 (1.5s cycle)
- Color: `PixelDesign.colors.glow`
- Used on: enabled buttons, selected cards, active inputs, mascot

**Blinking Cursor** — `BlinkingCursor` composable:
- `█` character, alpha 0→1 (500ms cycle)
- Placed after screen titles: `▸ STAGE VIEW█`

**Enchanted Divider** — `PixelEnchantedDivider`:
- Gradient shimmer sweep left-to-right (2s cycle)
- Colors: primary → secondary → tertiary → primary

**Fantasy Stars** — `PixelStar` composable:
- 4-pointed star, rotates 360° (4s), brightness pulses (1s)
- Used in headers, empty states, loading screens

**Loading Spinner** — `PixelLoadingSpinner`:
- Sweep gradient circle + 4 orbiting sparkle dots
- Sizes: Small (24dp), Medium (40dp), Large (64dp)

## Section 4: Component Library

25 components total. Every component uses chamfered shapes, pixel borders, reads from `PixelDesign.colors`.

### Interactive Components

**PixelButton**
- Two-layer 3D: shadow (black 30%) offset + face that depresses on press
- Chamfered (9dp), glowing border when enabled
- Press: face drops 4dp via `buttonPress` spring, glow intensifies
- Disabled: surface color, no glow, 50% alpha text
- Variants: Primary, Secondary, Surface, Danger
- Height: 48dp min, UPPERCASE labelLarge text
- Provides `LocalContentColor`

**PixelIconButton**
- 36dp square, chamfered (6dp)
- Scale spring on press (0.9→1.0)
- Optional glow border

**PixelToggle**
- Track: 48x26dp chamfered, Thumb: 20dp
- On: primary + glow, Off: surfaceVariant
- `buttonPress` spring, `Role.Switch` accessible

**PixelSlider**
- Chamfered track (full width, 8dp), blocky square thumb (20dp)
- Active = primary, inactive = surfaceVariant
- Optional value label above thumb, `dragReturn` spring

**PixelTextField**
- `BasicTextField` wrapper (full visual control)
- Chamfered (6dp), pixel border, focus glow
- Label: `✦ LABEL` in labelMedium, placeholder at 40% alpha

**PixelDropdown**
- Chamfered trigger + arrow, dropdown: chamfered card with glow
- Selected item: primary at 15% alpha, scrollable if > 5 items

### Container Components

**PixelCard**
- Chamfered (9dp), pixel border, hard shadow (4dp offset, black 20%)
- Optional `glowing`, surface background, 12dp padding
- Optional `onClick` with press animation

**PixelScaffold**
- Proper `SubcomposeLayout` — content constrained to remaining space after top/bottom bar
- CRT scanline overlay at this level
- Provides padding values to body

**PixelSurface**
- Background fill + optional pixel grid (primary at 3%)
- Provides `LocalContentColor`

**PixelDialog**
- Scrim (60%), centered chamfered card with glow
- Title + content + actions, `cardExpand` spring enter/exit

**PixelBottomSheet**
- `panelSlide` spring, chamfered top corners
- Pixel border top edge, drag handle, scrim behind

### Feedback Components

**PixelBadge**
- Chamfered (4dp), variants: Primary, Secondary, Tertiary, Error, Info
- labelSmall UPPERCASE

**PixelProgressBar**
- Chamfered track, fill gradient primary → secondary with shimmer
- Indeterminate: marching gradient, optional percentage overlay

**PixelLoadingSpinner**
- Sweep gradient + orbiting sparkles, 3 size variants

**PixelDivider**
- Standard: 1dp at 30% alpha
- Enchanted: shimmer gradient sweep
- Dashed: marching animation for processing states

**PixelToast / PixelSnackbar**
- Chamfered card, slides up, auto-dismiss 3s
- Icon + message + optional action

### Data Display Components

**PixelSectionTitle**
- `▸ TITLE` + optional badge + blinking cursor
- Sub-line: `└─ description` at 70% alpha

**PixelListItem**
- Row: optional icon, title, subtitle, trailing element
- Tappable with press feedback, divider between items

**PixelChip**
- Chamfered (6dp), selected: primary + glow, unselected: outline
- For filters, genre selection, tags

**PixelTable**
- Header: primary at 10% alpha, bold columns
- Data: alternating surface/surfaceVariant, chamfered container

### Decorative Components

- **BlinkingCursor** — animated █
- **PixelStar** — rotating, twinkling 4-pointed star
- **PixelSparkles** — orbiting particle system
- **ScanlineOverlay** — full-screen CRT effect
- **PixelEnchantedDivider** — shimmer gradient line

## Section 5: Testing & Validation

### Component Showcase Screen

`PixelShowcaseScreen` (debug builds only) renders every component in every state:
- All button variants × states (Enabled, Disabled, Pressed)
- All form inputs (empty, filled, focused, error)
- Cards (static, glowing, tappable)
- All badge variants
- Full typography scale
- Animation gallery
- Theme switcher (Dark / Light / High Contrast) at top

### Validation Checklist (per component)

1. Renders correctly on emulator at default density
2. Text readable at arm's length
3. Touch targets ≥ 48dp
4. Animations feel right (not sluggish, not jarring)
5. Works in all three themes
6. Respects reduced motion setting
7. No visual glitches on state transitions

### Emulator Testing

- `adb` install and launch after each component batch
- Screenshot comparisons between theme variants
- Test with font scaling at 1.0x, 1.3x, and 2.0x
