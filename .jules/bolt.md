## 2025-03-01 - Avoid Intermediate Color Allocations in Hot Paths
**Learning:** In the DMX engine's rendering hot path (`EffectStack`), even simple operations like `Color * float` or `.clamped()` can create thousands of intermediate `Color` objects per frame, leading to GC pauses and dropped frames.
**Action:** When working in the inner loop (e.g., `evaluate()` methods), bypass operator overloads that allocate new objects if the inputs are already bounded, and use direct `Color` instantiation with pre-multiplied/clamped values.

## 2025-03-01 - Avoid Chained Collection Operations in Hot Paths
**Learning:** In performance-critical hot paths like `BlobDetector.kt`, chaining collection operations such as `.filter { ... }.map { ... }` generates unnecessary intermediate collections and allocations, causing GC pressure and CPU cycles.
**Action:** Replace chained `.filter` and `.map` operations with `.mapNotNull { if (condition) result else null }` to perform both filtering and mapping in a single traversal with fewer allocations.
