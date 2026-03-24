
## 2024-05-18 - Improve Switch component accessibility
**Learning:** For Switch components like `PixelSwitch` in Compose Multiplatform, using `Modifier.toggleable` instead of `Modifier.clickable(role = Role.Switch)` is essential to ensure screen readers (like TalkBack) correctly announce the on/off state of the component.
**Action:** Always prefer `Modifier.toggleable` when building boolean toggle controls to provide accurate accessibility state feedback out-of-the-box.
