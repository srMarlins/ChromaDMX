# Palette's Journal

## UX & Accessibility Learnings
## 2025-02-24 - Accessibility for Switch Components
**Learning:** In Compose Multiplatform, using `Modifier.clickable(role = Role.Switch)` identifies the element as a switch but does not announce its current 'On' or 'Off' state.
**Action:** Use `Modifier.toggleable(value = checked, onValueChange = ...)` instead to properly set both the role and the value state for screen readers.
