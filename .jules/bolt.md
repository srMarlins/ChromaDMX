## 2025-03-01 - Avoid Intermediate Color Allocations in Hot Paths
**Learning:** In the DMX engine's rendering hot path (`EffectStack`), even simple operations like `Color * float` or `.clamped()` can create thousands of intermediate `Color` objects per frame, leading to GC pauses and dropped frames.
**Action:** When working in the inner loop (e.g., `evaluate()` methods), bypass operator overloads that allocate new objects if the inputs are already bounded, and use direct `Color` instantiation with pre-multiplied/clamped values.

## 2025-03-01 - Avoid Collections and Auto-Boxing in Vision Hot Paths
**Learning:** Using `ArrayDeque<Int>` in recursive/flood-fill vision algorithms (like `BlobDetector.floodFill`) causes significant primitive auto-boxing allocations (Int to Integer) on each pixel. Additionally, chaining `.filter {}.map {}` causes double traversal and an extra list allocation.
**Action:** Pre-allocate primitive arrays (e.g., `IntArray` sized to `w*h`) and track a `stackSize` index manually for DFS algorithms. Use `.mapNotNull {}` instead of `.filter.map` to perform operations in a single traversal with fewer allocations.
