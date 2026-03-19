
## 2026-03-02 - PixelSwitch Accessibility
**Learning:** `Modifier.clickable(role = Role.Switch)` with an `onClick` parameter on a custom toggle switch component does not correctly convey the "toggle" semantics to screen readers on Compose Multiplatform, resulting in suboptimal accessibility.
**Action:** Used `Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = ...)` instead, which specifically provides the current toggle state to semantics trees and appropriately handles the state mutation.
