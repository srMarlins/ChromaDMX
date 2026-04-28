## 2025-03-01 - Avoid Intermediate Color Allocations in Hot Paths
**Learning:** In the DMX engine's rendering hot path (`EffectStack`), even simple operations like `Color * float` or `.clamped()` can create thousands of intermediate `Color` objects per frame, leading to GC pauses and dropped frames.
**Action:** When working in the inner loop (e.g., `evaluate()` methods), bypass operator overloads that allocate new objects if the inputs are already bounded, and use direct `Color` instantiation with pre-multiplied/clamped values.
## 2024-05-24 - Avoid Fold for Immutable Map Wrappers
**Learning:** In performance-critical hot paths, avoiding O(N) object allocations and O(N^2) map-copying overhead when building immutable wrapper objects using `.entries.fold` is important.
**Action:** Use direct constructor calls for O(1) allocation, taking advantage of Kotlin's Map interface covariance.
