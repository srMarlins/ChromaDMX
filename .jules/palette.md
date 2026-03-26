
## 2024-03-26 - PixelSwitch Accessibility Announcement
**Learning:** Using `Modifier.clickable(role = Role.Switch)` allows TalkBack to identify the element as a switch, but it does NOT announce its current "On" or "Off" state because `clickable` doesn't hold semantic value state.
**Action:** Always use `Modifier.toggleable(value = checked, ...)` instead of `clickable` for custom switch components. It properly sets the semantics for both the role AND the current value state, ensuring screen readers announce changes correctly.
