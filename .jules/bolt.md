## 2025-03-01 - Avoid Intermediate Color Allocations in Hot Paths
**Learning:** In the DMX engine's rendering hot path (`EffectStack`), even simple operations like `Color * float` or `.clamped()` can create thousands of intermediate `Color` objects per frame, leading to GC pauses and dropped frames.
**Action:** When working in the inner loop (e.g., `evaluate()` methods), bypass operator overloads that allocate new objects if the inputs are already bounded, and use direct `Color` instantiation with pre-multiplied/clamped values.

## 2025-03-01 - Avoid Redundant Object Access in Iteration Paths
**Learning:** In hot rendering loops (like `DmxBridge` converting colors to universes at 60Hz), continually accessing nested properties (e.g., `fixture.fixture.profileId`) or repeatedly performing map lookups (`profiles[profileId]`) for immutable metadata incurs severe cumulative CPU and GC overhead across hundreds of objects per frame.
**Action:** When working in $O(N)$ execution paths that run frequently, pre-resolve and cache metadata into primitive arrays (e.g., `IntArray`, `FloatArray`) or flat object arrays inside the object constructor. Accessing pre-calculated index-aligned arrays eliminates redundant evaluation inside the hot path entirely.
