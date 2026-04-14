## 2024-04-14 - Semantic Switch Accessibility
**Learning:** Found custom Switch component `PixelSwitch` using `Modifier.clickable(role = Role.Switch)` instead of `Modifier.toggleable`. While `clickable` allows identifying it as a switch, it lacks the semantic tracking of `value` (on/off) which is critical for screen reader users to know its state.
**Action:** Replaced `clickable` with `toggleable(value = checked)` on the switch component when a callback is present, so the screen reader natively announces "Switch, Off" or "Switch, On".
