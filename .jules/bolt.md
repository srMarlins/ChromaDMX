## 2025-03-01 - Avoid Intermediate Color Allocations in Hot Paths
**Learning:** In the DMX engine's rendering hot path (`EffectStack`), even simple operations like `Color * float` or `.clamped()` can create thousands of intermediate `Color` objects per frame, leading to GC pauses and dropped frames.
**Action:** When working in the inner loop (e.g., `evaluate()` methods), bypass operator overloads that allocate new objects if the inputs are already bounded, and use direct `Color` instantiation with pre-multiplied/clamped values.

## 2025-04-21 - Avoid auto-boxing and redundant list allocations in `BlobDetector`
**Learning:** In the `BlobDetector` hot path within the vision module, using generic collections like `ArrayDeque<Int>` for the flood-fill stack caused primitive auto-boxing (Int to Integer) and unnecessary intermediate object creations. Furthermore, chained `filter` and `map` operations produced redundant intermediate list allocations.
**Action:** Replaced `ArrayDeque<Int>` with a pre-allocated primitive array (`IntArray`) to avoid auto-boxing and combined `filter` and `map` into a single `mapNotNull` pass to eliminate intermediate collection allocations in performance-critical paths.
