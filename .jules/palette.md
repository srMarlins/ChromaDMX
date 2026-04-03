## 2024-04-03 - Accessible Switch Components
**Learning:** In Compose Multiplatform, using `Modifier.clickable(role = Role.Switch)` identifies the element as a switch but does not announce its current 'On' or 'Off' state to screen readers.
**Action:** Must use `Modifier.toggleable(value = checked, ...)` instead of `clickable` to properly set semantics for both the role and the value state.
