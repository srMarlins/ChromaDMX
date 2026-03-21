## 2024-05-14 - Improve Switch component accessibility with Modifier.toggleable
**Learning:** In Compose Multiplatform, using `Modifier.clickable` with `role = Role.Switch` for switch components does not correctly announce their on/off state to screen readers.
**Action:** Always use `Modifier.toggleable` instead of `Modifier.clickable(role = Role.Switch)` for switch components to correctly announce their on/off state to screen readers.
