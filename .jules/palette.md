## 2024-05-18 - Compose Switch Accessibility

**Learning:** When building custom Switch components in Compose Multiplatform, using `Modifier.clickable(role = Role.Switch)` is insufficient because it does not announce the switch's `checked` (on/off) state to screen readers like TalkBack or VoiceOver. Furthermore, if the component supports a read-only state (e.g., `onCheckedChange` is null), omitting interactivity entirely will drop the `Role.Switch` semantics, making it appear as a generic text or box.

**Action:** Always use `Modifier.toggleable` instead of `clickable` for switches. If the switch needs to support a read-only state (nullable callback), wrap the `toggleable` modifier in a conditional `.let` block, and explicitly provide fallback semantics (`Modifier.semantics { role = Role.Switch; toggleableState = ... }`) when the callback is null to ensure screen readers always announce its role and state.
