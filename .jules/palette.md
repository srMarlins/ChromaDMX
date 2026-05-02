## 2024-06-25 - Improve PixelSwitch Accessibility
**Learning:** Found that custom switch components using `clickable(role = Role.Switch)` do not announce their on/off state to assistive technologies like screen readers in Compose Multiplatform.
**Action:** Replaced `clickable` with `toggleable(value = checked)` and applied it conditionally via `.let { mod -> if (onCheckedChange != null) ... }` to ensure proper accessibility state broadcasting and interactivity mapping. Also added keyboard option `autoCorrectEnabled = false` for passwords.
