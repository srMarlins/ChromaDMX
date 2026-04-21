## 2025-04-21 - Switch Accessibility via Toggleable
**Learning:** Using `Modifier.clickable(role = Role.Switch)` properly identifies a component as a switch to screen readers but fails to announce its current 'On'/'Off' state. `Modifier.toggleable(value = checked)` correctly manages both the role and the value state for a11y.
**Action:** Always prefer `toggleable` over `clickable` for custom switch/checkbox UI components to ensure accurate state announcements.
