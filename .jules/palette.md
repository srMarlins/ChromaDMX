
## 2025-01-23 - PixelSwitch Accessibility State Announcement
**Learning:** Using `Modifier.clickable(role = Role.Switch)` on custom switch components identifies the element as a switch but fails to announce its current 'On' or 'Off' state to screen readers.
**Action:** Always use `Modifier.toggleable(value = checked, ...)` for custom switches in Compose Multiplatform to properly set semantics for both the role and the value state.
