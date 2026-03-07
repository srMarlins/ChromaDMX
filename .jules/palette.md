## 2024-05-18 - Missing Role in Custom Pixel Components
**Learning:** Missing `role = Role.Button` on custom `Modifier.clickable` components prevents screen readers (like TalkBack) from correctly announcing them as interactive elements, leading to poor accessibility for visually impaired users.
**Action:** Always explicitly set `role = Role.Button` (or other appropriate roles like `Role.DropdownList` or `Role.Switch`) on `Modifier.clickable` blocks when building custom interactive pixel-styled components.
