## 2024-04-11 - Compose Multiplatform Custom Switch Accessibility
**Learning:** Using `Modifier.clickable(role = Role.Switch)` for custom switches incorrectly identifies the element as a switch but does not announce its current 'On' or 'Off' state to screen readers.
**Action:** Always use `Modifier.toggleable(value = checked, ...)` for custom switch components to properly set the semantics for both the role and the value state.
