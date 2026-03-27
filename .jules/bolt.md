## 2025-03-01 - Avoid Intermediate Color Allocations in Hot Paths
**Learning:** In the DMX engine's rendering hot path (`EffectStack`), even simple operations like `Color * float` or `.clamped()` can create thousands of intermediate `Color` objects per frame, leading to GC pauses and dropped frames.
**Action:** When working in the inner loop (e.g., `evaluate()` methods), bypass operator overloads that allocate new objects if the inputs are already bounded, and use direct `Color` instantiation with pre-multiplied/clamped values.
## 2026-03-27 - Avoid Triple object allocations and redundant coerceIn in Kotlin loops
**Learning:** In the DMX engine's rendering hot path, allocating `Triple` objects in exhaustive `when` blocks and invoking safe-math wrappers like `lerp()` with already-bounded variables causes severe GC pauses by creating thousands of short-lived objects.
**Action:** Use deferred assignment of primitive `val` variables inside `when` blocks and inline math instead of helper methods to bypass redundant checks.
