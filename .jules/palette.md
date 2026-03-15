## 2024-05-18 - Improve PixelSwitch Accessibility for Screen Readers
**Learning:** In Compose Multiplatform, using `Modifier.clickable(role = Role.Switch)` for custom switch components does not consistently announce the "on/off" (checked/unchecked) state to screen readers.
**Action:** Always use `Modifier.toggleable` instead of `Modifier.clickable(role = Role.Switch)` for switch or toggle components to ensure their state is properly exposed to accessibility services.
