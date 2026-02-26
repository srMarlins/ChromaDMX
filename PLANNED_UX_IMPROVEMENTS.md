# Planned UX Improvements & Issues

## 1. Design System & Theming
- **Standardize Pixel Theme**: Consolidate `PixelThemeData` to include more semantic colors (Success, Warning, Info) and standardized spacing tokens.
- **Typography Update**: Ensure `PixelFontFamily` is used consistently for headers, while a legible monospace or sans-serif font is used for data/body text.
- **Component Library**:
    - [ ] Create `PixelScaffold`: A standard layout container with a pixelated top bar and bottom navigation.
    - [ ] Create `PixelSwitch`: A toggle switch with pixel-art "on/off" states.
    - [ ] Create `PixelTextField`: An input field with pixel borders and clear focus states.
    - [ ] Refactor `PixelSlider`: Improve touch targets, add accessibility semantics, and smooth out the thumb dragging.
    - [ ] Refactor `PixelButton`: Add disabled states, loading indicators, and haptic feedback.
    - [ ] Create `PixelDialog`: A modal dialog with a pixelated window frame.

## 2. Layout & Navigation
- **Navigation Structure**: Simplify the hierarchy. Ensure the "Back" button is always clear and accessible.
- **Perform Screen Overhaul**:
    - Organize controls into logical groups (e.g., "Transport", "Effect", "Override").
    - Use `PixelCard` to visually group related elements.
    - Improve the "Stage Preview" visibility.
- **Settings Overlay**: Make the settings easily accessible without obscuring the entire context.

## 3. Feedback & Interaction
- **Haptics**: Integrate haptic feedback for all primary interactions (button taps, slider moves, successful connections).
- **Animations**:
    - Add "press" animations to all interactive elements (scale down or offset).
    - Add transition animations between screens (slide/pixelate).
- **Network Status**: Make the connection status (hearts) more prominent and interactive (tap for details).

## 4. Accessibility & Polish
- **Touch Targets**: Ensure all interactive elements have a minimum size of 48dp.
- **Semantics**: Add `semantics` modifiers to custom components (Slider, Toggle) for screen reader support.
- **Contrast**: Verify color contrast ratios for text on neon backgrounds.

## 5. "MtgPirate" Inspiration
- Adopt the "Clean but Retro" aesthetic:
    - High contrast borders (White on Black).
    - Explicit grid alignments.
    - "Card-based" layouts for distinct functional areas.
