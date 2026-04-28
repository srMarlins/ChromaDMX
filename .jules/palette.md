## 2025-03-01 - Proper Accessible State for Switches
**Learning:** When building a custom switch component in Compose Multiplatform, using `clickable(role = Role.Switch)` is insufficient for proper accessibility because it only tells the screen reader the component is a switch, but not its current state.
**Action:** Always use the `toggleable(value = checked, ...)` modifier instead. This correctly announces both the switch role AND its current on/off (boolean) state to assistive technologies.
