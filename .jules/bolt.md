## 2025-03-01 - Avoid Intermediate Color Allocations in Hot Paths
**Learning:** In the DMX engine's rendering hot path (`EffectStack`), even simple operations like `Color * float` or `.clamped()` can create thousands of intermediate `Color` objects per frame, leading to GC pauses and dropped frames.
**Action:** When working in the inner loop (e.g., `evaluate()` methods), bypass operator overloads that allocate new objects if the inputs are already bounded, and use direct `Color` instantiation with pre-multiplied/clamped values.

## 2025-03-01 - Avoid Redundant CoerceIn in Color Interpolation Math
**Learning:** During color interpolation math, methods like `Color.lerp()` include `.coerceIn(0f, 1f)` safety checks. When this is invoked iteratively inside a tight render loop (like sampling an existing palette for a 3D effect across multiple positions where fractional indices are proven to be in the [0, 1] range), the redundant coercion creates significant overhead.
**Action:** Inline color primitive math directly when the input range is already mathematically bounded, rather than relying on generalized safe math helpers like `Color.lerp()`.
