## 2025-03-01 - Avoid Intermediate Color Allocations in Hot Paths
**Learning:** In the DMX engine's rendering hot path (`EffectStack`), even simple operations like `Color * float` or `.clamped()` can create thousands of intermediate `Color` objects per frame, leading to GC pauses and dropped frames.
**Action:** When working in the inner loop (e.g., `evaluate()` methods), bypass operator overloads that allocate new objects if the inputs are already bounded, and use direct `Color` instantiation with pre-multiplied/clamped values.

## 2025-03-01 - Avoid Triple and Color Allocations in ColorUtils
**Learning:** In exhaustive `when` blocks returning a `Triple`, or math functions returning objects like `Color.lerp` in hot paths (like `hsvToRgb` and `samplePalette`), primitive values are boxed to their object equivalents (e.g. `Float` to `java.lang.Float`), creating multiple GC-tracked allocations per call.
**Action:** Use deferred assignment with primitive `val` variables instead of `Triple`, and compute mathematical formulas inline (returning a single new `Color` instead of relying on `Color.lerp` and `.coerceIn()`) for bounded values in math hot loops.
