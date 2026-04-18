## $(date +%Y-%m-%d) - Improve PixelSwitch Accessibility
**Learning:** The custom `PixelSwitch` used `Modifier.clickable(role = Role.Switch)`, which identifies the role but fails to announce its current 'On' or 'Off' state to screen readers.
**Action:** Replaced `clickable` with `Modifier.toggleable` (conditionally applied using `.let` to handle the nullable callback), which properly sets semantics for both the role and the value state.
