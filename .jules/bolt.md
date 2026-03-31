## 2025-03-01 - Avoid Intermediate Color Allocations in Hot Paths
**Learning:** In the DMX engine's rendering hot path (`EffectStack`), even simple operations like `Color * float` or `.clamped()` can create thousands of intermediate `Color` objects per frame, leading to GC pauses and dropped frames.
**Action:** When working in the inner loop (e.g., `evaluate()` methods), bypass operator overloads that allocate new objects if the inputs are already bounded, and use direct `Color` instantiation with pre-multiplied/clamped values.

## 2025-03-31 - Avoid Auto-Boxing and Object Allocation in when Blocks
**Learning:** Using `Triple` or `Pair` in exhaustive `when` expressions within hot paths (like `ColorUtils.hsvToRgb`) causes both intermediate object allocation (the `Triple` itself) and primitive auto-boxing (e.g., `Float` to `java.lang.Float`), creating significant GC pressure.
**Action:** Refactor these structures to use deferred assignment of primitive `val` variables (e.g., `val r1: Float; when { ... -> { r1 = c } }`) to keep operations on the stack and entirely allocation-free.
