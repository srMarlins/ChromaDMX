## 2025-03-01 - Avoid Intermediate Color Allocations in Hot Paths
**Learning:** In the DMX engine's rendering hot path (`EffectStack`), even simple operations like `Color * float` or `.clamped()` can create thousands of intermediate `Color` objects per frame, leading to GC pauses and dropped frames.
**Action:** When working in the inner loop (e.g., `evaluate()` methods), bypass operator overloads that allocate new objects if the inputs are already bounded, and use direct `Color` instantiation with pre-multiplied/clamped values.
## 2025-04-18 - Avoid Primitive Auto-boxing in Hot Loops
**Learning:** Using generic collections like `ArrayDeque<Int>` in performance-critical hot paths (like `BlobDetector.floodFill`) causes significant auto-boxing (Int -> java.lang.Integer) and object allocation overhead. Chained `.filter().map()` also creates intermediate collections.
**Action:** Pre-allocate primitive arrays (e.g., `IntArray`) to act as stacks/queues in algorithms, and use `.mapNotNull()` to combine filtering and mapping to eliminate intermediate allocations and GC pressure.
