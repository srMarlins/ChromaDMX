## 2024-05-24 - Accessibility of Switch Components
**Learning:** In Compose Multiplatform, using `Modifier.toggleable` instead of `Modifier.clickable(role = Role.Switch)` correctly announces the on/off state of switch components (like `PixelSwitch`) to screen readers.
**Action:** Always use `Modifier.toggleable` for custom switch components to ensure proper accessibility and state announcement.
