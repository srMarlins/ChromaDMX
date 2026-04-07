## 2025-03-01 - Avoid Intermediate Color Allocations in Hot Paths
**Learning:** In the DMX engine's rendering hot path (`EffectStack`), even simple operations like `Color * float` or `.clamped()` can create thousands of intermediate `Color` objects per frame, leading to GC pauses and dropped frames.
**Action:** When working in the inner loop (e.g., `evaluate()` methods), bypass operator overloads that allocate new objects if the inputs are already bounded, and use direct `Color` instantiation with pre-multiplied/clamped values.

## 2025-03-01 - Avoid Auto-Boxing and Chained Collections in Hot Paths
**Learning:** In performance-critical hot paths like `BlobDetector.floodFill`, using generic collections like `ArrayDeque<Int>` for primitive types causes auto-boxing (`Int` to `java.lang.Integer`) and frequent object allocations. Similarly, chaining `.filter { ... }.map { ... }` creates intermediate collections.
**Action:** Use pre-allocated primitive arrays (e.g., `IntArray`) to avoid auto-boxing and minimize GC pressure. Use `.mapNotNull { if (condition) result else null }` to combine filtering and mapping into a single traversal without intermediate allocations.
