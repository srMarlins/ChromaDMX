## 2024-05-18 - Added ARIA/semantics labels to custom PixelIconButton
**Learning:** Custom interactive components like `PixelIconButton` built with `Box` and `clickable` do not inherit accessibility traits automatically; they must explicitly expose and apply `contentDescription` via the `semantics` modifier.
**Action:** Always add a `contentDescription` parameter to custom icon-only UI components and apply it within `Modifier.semantics` before the `clickable` modifier.
