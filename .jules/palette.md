## 2024-04-13 - Compose Accessibility for Switch Components
**Learning:** Using `Modifier.clickable(role = Role.Switch)` correctly identifies the element as a switch but fails to announce its current 'On' or 'Off' state. `Modifier.toggleable` correctly communicates both the role and its current state.
**Action:** Always use `toggleable` instead of `clickable` for custom switch or checkbox components in Compose Multiplatform to ensure screen readers announce their state.
