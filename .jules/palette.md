
## 2024-05-24 - Custom Switch Accessibility in Compose
**Learning:** Using `Modifier.clickable(role = Role.Switch)` on custom components correctly identifies the role to screen readers, but fails to announce its current 'On' or 'Off' state.
**Action:** Always use `Modifier.toggleable(value = checked, onValueChange = ...)` for switch components, which natively sets both the role and the current state semantics for screen readers.
