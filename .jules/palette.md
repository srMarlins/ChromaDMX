## 2026-03-25 - Modifier.toggleable over clickable
**Learning:** In Compose Multiplatform, using Modifier.toggleable instead of Modifier.clickable(role = Role.Switch) is crucial for screen readers (like TalkBack) to correctly announce the component's on/off state to users.
**Action:** Use Modifier.toggleable for all custom switch, checkbox, and toggle implementations.
