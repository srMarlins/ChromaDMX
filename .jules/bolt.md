## 2025-03-01 - Avoid Intermediate Color Allocations in Hot Paths
**Learning:** In the DMX engine's rendering hot path (`EffectStack`), even simple operations like `Color * float` or `.clamped()` can create thousands of intermediate `Color` objects per frame, leading to GC pauses and dropped frames.
**Action:** When working in the inner loop (e.g., `evaluate()` methods), bypass operator overloads that allocate new objects if the inputs are already bounded, and use direct `Color` instantiation with pre-multiplied/clamped values.
## 2025-04-09 - Reduce allocations and auto-boxing in BlobDetector
**Learning:** In performance-critical vision hot paths, `ArrayDeque<Int>` causes primitive auto-boxing and object allocations. Chaining `.filter` and `.map` creates intermediate collections.
**Action:** Use pre-allocated primitive arrays (`IntArray`) instead of generic collections for primitive types to avoid GC pressure. Use `.mapNotNull` to filter and map in a single pass without intermediate collections.
