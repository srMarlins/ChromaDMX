## 2024-05-18 - Accessibility Improvement for PixelSwitch
**Learning:** `Modifier.clickable` with `role = Role.Switch` does not announce the on/off state to screen readers correctly in Compose Multiplatform.
**Action:** Use `Modifier.toggleable` instead for switch components like `PixelSwitch` to correctly announce their state to screen readers.
