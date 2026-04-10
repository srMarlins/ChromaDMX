## 2025-03-01 - Avoid Intermediate Color Allocations in Hot Paths
**Learning:** In the DMX engine's rendering hot path (`EffectStack`), even simple operations like `Color * float` or `.clamped()` can create thousands of intermediate `Color` objects per frame, leading to GC pauses and dropped frames.
**Action:** When working in the inner loop (e.g., `evaluate()` methods), bypass operator overloads that allocate new objects if the inputs are already bounded, and use direct `Color` instantiation with pre-multiplied/clamped values.

## 2024-05-24 - BlobDetector flood-fill array allocations
**Learning:** Using generic collections like `ArrayDeque<Int>` for primitives in hot paths causes auto-boxing and GC pressure. Chained `.filter().map()` causes intermediate allocations.
**Action:** Use pre-allocated primitive arrays like `IntArray` and manage the index manually. Use `.mapNotNull()` to combine filtering and mapping.
