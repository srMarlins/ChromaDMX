## 2024-05-24 - Use toggleable instead of clickable(role = Role.Switch) for switches
**Learning:** In Compose Multiplatform, using `Modifier.clickable(role = Role.Switch)` on switch components (like `PixelSwitch`) does not correctly announce their on/off state to screen readers.
**Action:** Use `Modifier.toggleable` instead, which natively handles state announcements for switches, checkboxes, etc.
