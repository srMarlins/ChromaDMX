## 2024-05-24 - Accessible Switch States
**Learning:** Using `Modifier.clickable` on a custom switch with `Role.Switch` identifies it as a switch, but fails to announce its current 'On' or 'Off' state to screen readers.
**Action:** Always use `Modifier.toggleable(value = checked, ...)` for custom switch/checkbox components to ensure both the role and the boolean value state are semantically exposed.
