## 2025-03-01 - Avoid Intermediate Color Allocations in Hot Paths
**Learning:** In the DMX engine's rendering hot path (`EffectStack`), even simple operations like `Color * float` or `.clamped()` can create thousands of intermediate `Color` objects per frame, leading to GC pauses and dropped frames.
**Action:** When working in the inner loop (e.g., `evaluate()` methods), bypass operator overloads that allocate new objects if the inputs are already bounded, and use direct `Color` instantiation with pre-multiplied/clamped values.

## 2025-03-01 - Optimize BlobDetector by reducing GC pressure
**Learning:** Found that using generic collections (`ArrayDeque<Int>`) for primitive types in high-frequency hot paths (like vision processing `floodFill`) causes significant auto-boxing overhead (Int -> java.lang.Integer) and heavy GC pressure.
**Action:** Replaced `ArrayDeque<Int>` with a pre-allocated single `IntArray` shared across the stack for `floodFill` in `BlobDetector`. Also replaced `filter` + `map` chain with `mapNotNull` to eliminate intermediate lists. Next time, always check for generic primitive collections in performance-critical loops.
