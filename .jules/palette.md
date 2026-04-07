## 2025-03-09 - PixelSwitch Accessibility
**Learning:** Using `Modifier.clickable(role = Role.Switch)` for custom switches identifies the component as a switch to screen readers but fails to announce its current "On" or "Off" state.
**Action:** Always use `Modifier.toggleable(value = checked, ...)` for custom switch/checkbox components in Compose Multiplatform to properly bind the semantic value state to the accessibility tree.
