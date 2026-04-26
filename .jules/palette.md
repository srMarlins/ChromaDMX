## 2024-05-15 - [Initial Rules]
**Learning:** In Compose Multiplatform, custom interactive containers or icon-only buttons must expose a `contentDescription` parameter and apply it via `Modifier.semantics { this.contentDescription = ... }` before the `clickable` modifier to ensure they are properly announced by screen readers.
**Action:** Always add a `contentDescription: String? = null` parameter to custom icon button implementations and apply it via semantics.
