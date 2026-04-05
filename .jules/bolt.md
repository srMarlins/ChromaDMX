## 2025-03-01 - Avoid Intermediate Color Allocations in Hot Paths
**Learning:** In the DMX engine's rendering hot path (`EffectStack`), even simple operations like `Color * float` or `.clamped()` can create thousands of intermediate `Color` objects per frame, leading to GC pauses and dropped frames.
**Action:** When working in the inner loop (e.g., `evaluate()` methods), bypass operator overloads that allocate new objects if the inputs are already bounded, and use direct `Color` instantiation with pre-multiplied/clamped values.
## 2024-05-18 - Avoid Triple allocations in hot path when blocks
**Learning:** Using `val (a, b) = when { ... -> Triple(x,y,z) }` in Kotlin performance-critical hot paths (like DMX engine inner loops or color conversions) causes primitive auto-boxing and intermediate object creations (`Triple`), leading to GC pressure.
**Action:** Use deferred assignment of primitive `val` variables (e.g., `val r1: Float; when { ... -> { r1 = c } }`) to prevent GC pauses and frame drops.
