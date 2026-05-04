## 2026-05-04 - PixelSwitch Accessibility
**Learning:** In Compose Multiplatform, using `clickable(role = Role.Switch)` on a custom switch component only announces the role but fails to communicate the actual on/off boolean state to screen readers.
**Action:** Replace `clickable` with `toggleable(value = checked, ...)` which correctly announces both the role and state. For components where the callback might be null, conditionally apply `toggleable` using a `.let` block so it correctly remains non-interactive when no callback is provided.
