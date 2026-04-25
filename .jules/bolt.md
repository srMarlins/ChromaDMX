## 2025-03-01 - Avoid Intermediate Color Allocations in Hot Paths
**Learning:** In the DMX engine's rendering hot path (`EffectStack`), even simple operations like `Color * float` or `.clamped()` can create thousands of intermediate `Color` objects per frame, leading to GC pauses and dropped frames.
**Action:** When working in the inner loop (e.g., `evaluate()` methods), bypass operator overloads that allocate new objects if the inputs are already bounded, and use direct `Color` instantiation with pre-multiplied/clamped values.

## 2025-03-01 - Pre-resolve fixture metadata in hot loops
**Learning:** O(N) map lookups and list scanning inside a high-frequency (40-60Hz) per-fixture rendering loop can cause noticeable CPU overhead.
**Action:** When working with per-fixture rendering loops, always pre-resolve static metadata (like DMX channel offsets, profile capabilities, and array indices) in the constructor or setup phase into O(1) accessible flat arrays to avoid per-frame lookups.
