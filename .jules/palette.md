## 2024-05-24 - Improve PixelSwitch Accessibility
**Learning:** Using `Modifier.clickable(role = Role.Switch)` correctly identifies a custom component as a switch to screen readers but fails to announce its current 'On' or 'Off' state. `Modifier.toggleable` is required for switches to properly manage and announce their state.
**Action:** Ensure all custom toggle-based UI elements (switches, checkboxes) use `Modifier.toggleable` instead of `Modifier.clickable` with a role.
