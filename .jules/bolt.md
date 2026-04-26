## 2025-03-01 - Avoid Intermediate Color Allocations in Hot Paths
**Learning:** In the DMX engine's rendering hot path (`EffectStack`), even simple operations like `Color * float` or `.clamped()` can create thousands of intermediate `Color` objects per frame, leading to GC pauses and dropped frames.
**Action:** When working in the inner loop (e.g., `evaluate()` methods), bypass operator overloads that allocate new objects if the inputs are already bounded, and use direct `Color` instantiation with pre-multiplied/clamped values.

## 2024-06-25 - Optimize DMX Conversion Loop by Pre-resolving Indexed Arrays
**Learning:** To optimize high-frequency DMX conversion paths (40Hz-60Hz) in `DmxBridge`, pre-resolving fixture metadata—such as profiles, universe IDs, and channel starts—into indexed arrays in the constructor avoids redundant O(N) map lookups and nested property access within the per-fixture per-frame loops.
**Action:** When working on hot paths running at high refresh rates, use parallel, pre-calculated arrays for quick, allocation-free data access over complex object lookups per loop iteration.
