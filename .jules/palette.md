
## 2024-05-19 - Use toggleable instead of clickable for custom Switch components
**Learning:** `Modifier.clickable(role = Role.Switch)` tells the screen reader it is a switch but does NOT announce the current "On" or "Off" state of the checked property, making it inaccessible.
**Action:** Use `Modifier.toggleable(value = checked, ...)` instead when building custom switch or checkbox components in Compose. This properly associates the semantics for both the role and the boolean value state.
