## 2025-03-01 - Avoid Intermediate Color Allocations in Hot Paths
**Learning:** In the DMX engine's rendering hot path (`EffectStack`), even simple operations like `Color * float` or `.clamped()` can create thousands of intermediate `Color` objects per frame, leading to GC pauses and dropped frames.
**Action:** When working in the inner loop (e.g., `evaluate()` methods), bypass operator overloads that allocate new objects if the inputs are already bounded, and use direct `Color` instantiation with pre-multiplied/clamped values.

## 2026-04-24 - Pre-allocated Primitive Arrays for Flood Fill
**Learning:** Using generic collections like `ArrayDeque<Int>` for primitive types in hot paths causes auto-boxing (Int to java.lang.Integer) and intermediate object allocations, adding significant GC pressure.
**Action:** Replace `ArrayDeque<Int>` with pre-allocated primitive arrays (e.g., `IntArray`) and manage the stack pointer manually to eliminate intermediate object allocations.
