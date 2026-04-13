## 2025-03-01 - Avoid Intermediate Color Allocations in Hot Paths
**Learning:** In the DMX engine's rendering hot path (`EffectStack`), even simple operations like `Color * float` or `.clamped()` can create thousands of intermediate `Color` objects per frame, leading to GC pauses and dropped frames.
**Action:** When working in the inner loop (e.g., `evaluate()` methods), bypass operator overloads that allocate new objects if the inputs are already bounded, and use direct `Color` instantiation with pre-multiplied/clamped values.

## 2025-03-02 - Eliminate Allocation and Boxing in Flood-Fill
**Learning:** Using `ArrayDeque<Int>` in `BlobDetector.floodFill` for connected-component labeling causes massive auto-boxing (`Int` -> `java.lang.Integer`) and internal allocations, hurting performance in the vision processing hot path.
**Action:** Replaced `ArrayDeque` with a pre-allocated `IntArray` stack at the class level and tracked `stackSize` manually, cutting allocations down drastically. Used `.mapNotNull` instead of `.filter.map` to avoid intermediate collections.
