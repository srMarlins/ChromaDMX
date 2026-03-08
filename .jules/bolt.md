## 2025-03-01 - Avoid Intermediate Color Allocations in Hot Paths
**Learning:** In the DMX engine's rendering hot path (`EffectStack`), even simple operations like `Color * float` or `.clamped()` can create thousands of intermediate `Color` objects per frame, leading to GC pauses and dropped frames.
**Action:** When working in the inner loop (e.g., `evaluate()` methods), bypass operator overloads that allocate new objects if the inputs are already bounded, and use direct `Color` instantiation with pre-multiplied/clamped values.

## 2024-05-20 - [Avoid Intermediate Object Allocations in Render Loop]
**Learning:** Returning intermediate objects like `Triple` in frequently called math functions (like `ColorUtils.hsvToRgb`) forces garbage collection in the 60fps render path, causing frame drops and GC pauses.
**Action:** Use local primitive variables and early assignments to avoid allocating temporary wrapper objects like `Triple` during hot-path calculations.
