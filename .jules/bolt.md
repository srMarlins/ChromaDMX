## 2025-03-01 - Avoid Intermediate Color Allocations in Hot Paths
**Learning:** In the DMX engine's rendering hot path (`EffectStack`), even simple operations like `Color * float` or `.clamped()` can create thousands of intermediate `Color` objects per frame, leading to GC pauses and dropped frames.
**Action:** When working in the inner loop (e.g., `evaluate()` methods), bypass operator overloads that allocate new objects if the inputs are already bounded, and use direct `Color` instantiation with pre-multiplied/clamped values.

## 2025-02-28 - Removed Triple allocation in ColorUtils
**Learning:** Using `Triple` or `Pair` with primitives in Kotlin hot loops (like color conversion) causes hidden object allocations and primitive auto-boxing (e.g. `Float` to `java.lang.Float`), creating GC pressure.
**Action:** Use deferred initialization of primitive local variables instead of `Triple` in `when` expressions inside performance-critical paths.
