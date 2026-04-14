## 2025-03-01 - Avoid Intermediate Color Allocations in Hot Paths
**Learning:** In the DMX engine's rendering hot path (`EffectStack`), even simple operations like `Color * float` or `.clamped()` can create thousands of intermediate `Color` objects per frame, leading to GC pauses and dropped frames.
**Action:** When working in the inner loop (e.g., `evaluate()` methods), bypass operator overloads that allocate new objects if the inputs are already bounded, and use direct `Color` instantiation with pre-multiplied/clamped values.

## 2025-03-02 - Pre-allocate Primitive Array Stacks to Avoid Auto-boxing
**Learning:** In hot path algorithms like `BlobDetector.floodFill`, using generic collections like `ArrayDeque<Int>` causes primitive auto-boxing and significant object allocations per component.
**Action:** Replace `ArrayDeque<Int>` with a pre-allocated primitive array (`IntArray`) passed directly from the caller to manage stack states, eliminating allocations entirely.
