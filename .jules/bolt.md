## 2025-03-01 - Avoid Intermediate Color Allocations in Hot Paths
**Learning:** In the DMX engine's rendering hot path (`EffectStack`), even simple operations like `Color * float` or `.clamped()` can create thousands of intermediate `Color` objects per frame, leading to GC pauses and dropped frames.
**Action:** When working in the inner loop (e.g., `evaluate()` methods), bypass operator overloads that allocate new objects if the inputs are already bounded, and use direct `Color` instantiation with pre-multiplied/clamped values.
## 2025-03-02 - DMX Bridge Resolution O(N) lookup overhead elimination
**Learning:** `DmxBridge` evaluated metadata repeatedly per frame in `convert()` / `convertOutputs()`, fetching profiles from maps and properties within a hot path. High-frequency rendering causes this redundant check overhead to accumulate.
**Action:** Pre-resolve fixture metadata (profile, channel starts, universe info) into flat indexed arrays during `DmxBridge` initialization. This enables simple array O(1) lookups during the main high-frequency loop and eliminates hash map accesses.
