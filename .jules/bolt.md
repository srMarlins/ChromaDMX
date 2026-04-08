## 2025-03-01 - Avoid Intermediate Color Allocations in Hot Paths
**Learning:** In the DMX engine's rendering hot path (`EffectStack`), even simple operations like `Color * float` or `.clamped()` can create thousands of intermediate `Color` objects per frame, leading to GC pauses and dropped frames.
**Action:** When working in the inner loop (e.g., `evaluate()` methods), bypass operator overloads that allocate new objects if the inputs are already bounded, and use direct `Color` instantiation with pre-multiplied/clamped values.

## 2025-03-01 - Avoid Collections of Primitives in Hot Paths
**Learning:** In Kotlin, using generic collections like `ArrayDeque<Int>` for primitives causes auto-boxing to objects (e.g., `Int` to `java.lang.Integer`). In high-frequency code like the per-frame `BlobDetector.floodFill`, this creates immense GC pressure. Additionally, chaining `.filter {}.map {}` creates intermediate collections.
**Action:** Replace primitive collections with pre-allocated primitive arrays (e.g., `IntArray`) in hot paths. Use `.mapNotNull {}` instead of chained filter/map to avoid intermediate collections.
