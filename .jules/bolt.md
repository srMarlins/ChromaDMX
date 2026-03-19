## 2025-03-01 - Avoid Intermediate Color Allocations in Hot Paths
**Learning:** In the DMX engine's rendering hot path (`EffectStack`), even simple operations like `Color * float` or `.clamped()` can create thousands of intermediate `Color` objects per frame, leading to GC pauses and dropped frames.
**Action:** When working in the inner loop (e.g., `evaluate()` methods), bypass operator overloads that allocate new objects if the inputs are already bounded, and use direct `Color` instantiation with pre-multiplied/clamped values.

## 2025-03-10 - Avoid Chained Collection Operations in Hot Paths
**Learning:** In the `BlobDetector` computer vision hot path, chaining `.filter { ... }.map { ... }` on collections causes unnecessary intermediate object allocations, adding GC pressure during per-frame processing.
**Action:** Always combine filtering and mapping into a single `.mapNotNull { if (condition) result else null }` to traverse the collection exactly once and avoid the intermediate collection allocation.