## 2024-05-15 - ARIA equivalent semantics for custom Compose components
**Learning:** Custom Compose components (like `PixelIconButton` built on top of `Box` + `clickable`) don't automatically expose or apply `contentDescription` for screen readers, unlike standard Material components.
**Action:** Always verify that custom icon-only components expose a `contentDescription` parameter and map it to `Modifier.semantics { this.contentDescription = ... }`.
