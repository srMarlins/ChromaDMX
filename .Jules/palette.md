## 2025-03-11 - Use toggleable for Switch accessibility
**Learning:** In Compose Multiplatform, using `Modifier.clickable(role = Role.Switch)` for switch components does not correctly announce their on/off state to screen readers.
**Action:** Always use `Modifier.toggleable` instead of `Modifier.clickable` for switch components like `PixelSwitch` to ensure proper accessibility and screen reader support.
