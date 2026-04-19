## 2025-03-01 - Avoid Intermediate Color Allocations in Hot Paths
**Learning:** In the DMX engine's rendering hot path (`EffectStack`), even simple operations like `Color * float` or `.clamped()` can create thousands of intermediate `Color` objects per frame, leading to GC pauses and dropped frames.
**Action:** When working in the inner loop (e.g., `evaluate()` methods), bypass operator overloads that allocate new objects if the inputs are already bounded, and use direct `Color` instantiation with pre-multiplied/clamped values.
## 2025-04-19 - EffectParams Allocation Optimization in Engine Controller Hot Path
**Learning:** In the `RealEngineController.kt` and `PreGenerationService.kt` hot paths, iterating through Map entries and using `fold` to build immutable `EffectParams` via sequential `.with()` calls incurs $O(N)$ redundant allocations and map copies.
**Action:** Replace `entries.fold(EffectParams.EMPTY) { acc, (k, v) -> acc.with(k, v) }` with a direct `$O(1)$ allocation by using the `EffectParams` constructor wrapper around the source Map directly, passing the map reference without duplicating entries.
