## 2024-05-18 - Compose Multiplatform Switch Accessibility
**Learning:** Custom switch components using `Modifier.clickable(role = Role.Switch)` do not announce their "On"/"Off" value to screen readers.
**Action:** Always use `Modifier.toggleable(value = checked, role = Role.Switch)` for custom switches so that both the semantic role and the current state value are correctly announced.
