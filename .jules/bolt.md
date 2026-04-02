## 2025-03-01 - Avoid Intermediate Color Allocations in Hot Paths
**Learning:** In the DMX engine's rendering hot path (`EffectStack`), even simple operations like `Color * float` or `.clamped()` can create thousands of intermediate `Color` objects per frame, leading to GC pauses and dropped frames.
**Action:** When working in the inner loop (e.g., `evaluate()` methods), bypass operator overloads that allocate new objects if the inputs are already bounded, and use direct `Color` instantiation with pre-multiplied/clamped values.

## 2025-03-01 - Avoid Primitive Auto-Boxing in Vision Hot Paths
**Learning:** In performance-critical hot paths like `BlobDetector.floodFill`, using generic collections like `ArrayDeque<Int>` causes primitive auto-boxing (`Int` to `java.lang.Integer`) and frequent intermediate object allocations, which creates GC pressure.
**Action:** Prefer pre-allocated primitive arrays (e.g., `IntArray`) to act as stacks or queues in inner loops to prevent boxing and minimize GC overhead.
