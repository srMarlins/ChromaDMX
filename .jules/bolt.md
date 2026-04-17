## 2025-03-01 - Avoid Intermediate Color Allocations in Hot Paths
**Learning:** In the DMX engine's rendering hot path (`EffectStack`), even simple operations like `Color * float` or `.clamped()` can create thousands of intermediate `Color` objects per frame, leading to GC pauses and dropped frames.
**Action:** When working in the inner loop (e.g., `evaluate()` methods), bypass operator overloads that allocate new objects if the inputs are already bounded, and use direct `Color` instantiation with pre-multiplied/clamped values.

## 2024-04-17 - Avoid Intermediate Collections and Autoboxing in Vision Hot Paths
**Learning:** The `BlobDetector.floodFill` processing image frames frequently accesses generic collections like `ArrayDeque<Int>`, causing primitive auto-boxing and intermediate object creations. Combining `.filter {}.map {}` into `.mapNotNull {}` reduces GC pressure and CPU cycles.
**Action:** In performance-critical vision logic, replace `ArrayDeque` with pre-allocated primitive arrays (like `IntArray`) as stacks, and combine sequential collection operators to minimize allocations.
