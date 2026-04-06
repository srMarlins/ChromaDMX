## 2024-05-24 - Fix Switch Accessibility Announcing
**Learning:** Using `Modifier.clickable(role = Role.Switch)` properly sets the semantics role, but screen readers will not announce the current checked/unchecked state of the switch.
**Action:** Use `Modifier.toggleable(value = checked, ...)` instead, which correctly sets both the role and announces the state changes to screen readers.
