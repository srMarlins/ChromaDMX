## 2024-04-23 - PixelSwitch screen reader semantics
**Learning:** Using `Modifier.clickable(role = Role.Switch)` does not announce state. Custom switches need `Modifier.toggleable` to announce the 'On' or 'Off' value.
**Action:** Used `Modifier.toggleable` inside a `let` block conditional on `onCheckedChange != null` instead of `clickable`.
