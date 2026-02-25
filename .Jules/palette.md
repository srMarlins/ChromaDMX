## 2024-05-22 - Custom Clickable Components
**Learning:** Using `clickable(indication = null)` completely removes focus feedback. In `PixelButton`, this meant keyboard users had no way to know where focus was.
**Action:** Always add manual focus handling (border color change, scale, etc.) when disabling standard indication. Use `interactionSource.collectIsFocusedAsState()`.
