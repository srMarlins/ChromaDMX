
## 2024-04-20 - Custom Compose Switch Accessibility
**Learning:** In Compose Multiplatform, using `clickable(role = Role.Switch)` on a custom switch correctly identifies it as a switch, but screen readers will not announce its current "On" or "Off" state.
**Action:** Always use `Modifier.toggleable(value = checked, ...)` for custom switches, checking for nullable `onCheckedChange` callbacks via `.let` to maintain proper interaction behavior, as it inherently reports both the switch role and the current value.
