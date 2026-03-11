## 2025-02-27 - PixelSwitch Accessibility
**Learning:** Using `Modifier.clickable(role = Role.Switch)` on switch components correctly assigns the role but fails to announce the actual on/off state to screen readers in Compose Multiplatform.
**Action:** Replaced `Modifier.clickable` with `Modifier.toggleable` on switch components to ensure their toggled state is properly exposed to the accessibility tree.
