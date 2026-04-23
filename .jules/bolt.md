## 2025-03-01 - Avoid Intermediate Color Allocations in Hot Paths
**Learning:** In the DMX engine's rendering hot path (`EffectStack`), even simple operations like `Color * float` or `.clamped()` can create thousands of intermediate `Color` objects per frame, leading to GC pauses and dropped frames.
**Action:** When working in the inner loop (e.g., `evaluate()` methods), bypass operator overloads that allocate new objects if the inputs are already bounded, and use direct `Color` instantiation with pre-multiplied/clamped values.

## 2025-02-18 - Avoid O(N^2) Map Copies When Creating EffectParams
**Learning:** In `PreGenerationService.kt`, batch scene generation was using `.fold(EffectParams.EMPTY) { acc, (k, v) -> acc.with(k, v) }` to build `EffectParams` from a generic map of primitive parameters. Because `EffectParams.with` creates a completely new map on every single invocation, this caused $O(N)$ redundant allocations and $O(N^2)$ map-copying operations per layer, creating significant garbage collection pressure during rapid scene generation.
**Action:** Replace `fold` loops with direct `EffectParams(map)` constructor calls. Passing the source map directly delegates map handling to the constructor, reducing the operation complexity from $O(N^2)$ to $O(1)$ allocation overhead and dramatically improving hot path performance.
