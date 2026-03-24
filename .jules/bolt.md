## 2025-03-01 - Avoid Intermediate Color Allocations in Hot Paths
**Learning:** In the DMX engine's rendering hot path (`EffectStack`), even simple operations like `Color * float` or `.clamped()` can create thousands of intermediate `Color` objects per frame, leading to GC pauses and dropped frames.
**Action:** When working in the inner loop (e.g., `evaluate()` methods), bypass operator overloads that allocate new objects if the inputs are already bounded, and use direct `Color` instantiation with pre-multiplied/clamped values.

## 2025-03-01 - Avoid Triple/Pair Allocations in Exhaustive When Blocks
**Learning:** Returning `Triple` or `Pair` from an exhaustive `when` block in hot paths (like `ColorUtils.hsvToRgb`) forces object allocations and primitive auto-boxing on every execution, adding up to thousands of allocations per frame and causing GC pressure.
**Action:** Use deferred `val` assignments for primitive variables (e.g., `val r1: Float; when { ... -> r1 = val }`) instead of returning and destructuring utility objects in performance-critical code.
