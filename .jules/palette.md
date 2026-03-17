## 2026-03-17 - Add semantic roles to interactive components
**Learning:** Custom interactive components using raw `Modifier.clickable` are not announced as buttons to screen readers without an explicit semantic role.
**Action:** Always set `role = Role.Button` (or the appropriate role) in `Modifier.clickable` for interactive controls to ensure they are accessible.
