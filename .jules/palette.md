## 2026-03-01 - Switch Component Accessibility in Compose Multiplatform
**Learning:** In Compose Multiplatform, using `Modifier.clickable(role = Role.Switch)` on custom switch components (like `PixelSwitch`) does not correctly announce their on/off state to screen readers (TalkBack/VoiceOver).
**Action:** Always use `Modifier.toggleable` instead of `Modifier.clickable` for switch components to ensure their toggled state is properly exposed to accessibility services.
