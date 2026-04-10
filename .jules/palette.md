## 2024-05-15 - Improve PixelSwitch Accessibility
**Learning:** `Modifier.clickable(role = Role.Switch)` properly identifies a component as a switch, but it fails to communicate the current 'On'/'Off' state to screen readers. `Modifier.toggleable` properly provides semantics for both the role and the value state.
**Action:** Use `Modifier.toggleable(value = checked, ...)` instead of `Modifier.clickable` for custom switch components.
