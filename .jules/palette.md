## 2024-05-28 - PixelSwitch Accessibility Update
**Learning:** Raw `Modifier.clickable(role = Role.Switch)` with boolean state doesn't fully expose the toggle semantics (like current state being checked vs unchecked) properly to screen readers for switch-like components.
**Action:** Use `Modifier.toggleable` instead of `Modifier.clickable(role = Role.Switch)` for custom switch components (like `PixelSwitch`) so that TalkBack correctly announces "On" or "Off" states to screen readers.
