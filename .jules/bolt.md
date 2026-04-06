## 2025-03-01 - Avoid Intermediate Color Allocations in Hot Paths
**Learning:** In the DMX engine's rendering hot path (`EffectStack`), even simple operations like `Color * float` or `.clamped()` can create thousands of intermediate `Color` objects per frame, leading to GC pauses and dropped frames.
**Action:** When working in the inner loop (e.g., `evaluate()` methods), bypass operator overloads that allocate new objects if the inputs are already bounded, and use direct `Color` instantiation with pre-multiplied/clamped values.

## 2025-03-01 - Avoid Auto-Boxing with Primitives in Hot Loops
**Learning:** Using generic collections like `ArrayDeque<Int>` for primitive types in hot paths (like `floodFill` in vision code) causes auto-boxing, generating excessive object allocations and GC pressure.
**Action:** Replace primitive-storing generic collections with pre-allocated primitive arrays (e.g., `IntArray`) and manage the size/pointer manually to keep the operations strictly primitive.
