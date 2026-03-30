## 2024-05-24 - Custom Switch Accessibility
**Learning:** Using `Modifier.clickable(role = Role.Switch)` on custom switch components identifies the element as a switch to screen readers but fails to announce its current 'On' or 'Off' state.
**Action:** Always use `Modifier.toggleable(value = checked, ...)` for custom switches, which properly sets the semantics for both the role and the value state, providing accurate feedback to assistive technologies.
