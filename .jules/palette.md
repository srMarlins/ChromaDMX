
## 2025-03-08 - Switch Accessibility Semantics
**Learning:** In Compose Multiplatform, using `Modifier.clickable(role = Role.Switch)` tells the screen reader it is a switch but fails to announce its current "On" or "Off" value.
**Action:** Always use `Modifier.toggleable(value = checked, ...)` for custom switch components to properly set the semantics for both the role and its current state.
