## 2024-04-08 - Use toggleable instead of clickable for custom switches
**Learning:** Using `Modifier.clickable(role = Role.Switch)` identifies a custom switch component as a switch, but it doesn't announce its current "On" or "Off" state to screen readers.
**Action:** Use `Modifier.toggleable(value = checked, ...)` instead of `clickable` to ensure the component's state is properly announced.
