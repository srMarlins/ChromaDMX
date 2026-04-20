## 2025-03-01 - Avoid Intermediate Color Allocations in Hot Paths
**Learning:** In the DMX engine's rendering hot path (`EffectStack`), even simple operations like `Color * float` or `.clamped()` can create thousands of intermediate `Color` objects per frame, leading to GC pauses and dropped frames.
**Action:** When working in the inner loop (e.g., `evaluate()` methods), bypass operator overloads that allocate new objects if the inputs are already bounded, and use direct `Color` instantiation with pre-multiplied/clamped values.
## 2025-03-01 - Hoist Invariant Calculations in Rendering Loops
**Learning:** In Compose UI hot paths like `TopDownEditor.kt`, computations that depend solely on outer scope properties (like `size.width / 2f`) can trigger thousands of redundant arithmetic operations if placed inside `.map {}` closures over large collections.
**Action:** Always hoist loop-invariant calculations outside of collection mapping closures in performance-critical drawing loops to minimize repeated arithmetic and property access overhead.
