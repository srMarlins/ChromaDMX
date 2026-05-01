
## 2026-05-01 - Custom Switch Accessibility
**Learning:** In Compose Multiplatform, using `clickable(role = Role.Switch)` on a custom switch only announces its role, but not its checked state. `toggleable` must be used to announce both role and state.
**Action:** Use `toggleable` for custom switch components.
