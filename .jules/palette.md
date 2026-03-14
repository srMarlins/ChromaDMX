
## $(date +%Y-%m-%d) - Switch Accessibility in Compose
**Learning:** Using `Modifier.clickable(role = Role.Switch)` for custom switch components only announces "Switch" but fails to announce the actual checked/unchecked state to TalkBack/VoiceOver natively.
**Action:** Always use `Modifier.toggleable(value = checked, ... role = Role.Switch)` for binary toggle components to ensure correct state announcement in Compose Multiplatform.
