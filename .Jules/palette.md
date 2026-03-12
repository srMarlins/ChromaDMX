## 2024-03-12 - Compose Accessibility for Switch Components
**Learning:** In Compose Multiplatform, using `Modifier.clickable(role = Role.Switch)` for switch components doesn't correctly announce their state to screen readers by itself unless managed properly, but `Modifier.toggleable` inherently handles both the semantic role and the correct on/off state announcement.
**Action:** Use `Modifier.toggleable` instead of `Modifier.clickable` with `Role.Switch` for custom switch components (like `PixelSwitch`) to correctly support screen readers and TalkBack.
