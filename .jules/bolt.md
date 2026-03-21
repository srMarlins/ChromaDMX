## 2026-03-21 - Avoid Chained Collection Operations in Hot Paths
**Learning:** Chaining `.filter { ... }.map { ... }` on collections creates intermediate allocations which can cause GC pressure in performance-critical hot paths like `BlobDetector.kt`.
**Action:** Use `.mapNotNull { if (condition) result else null }` to filter and map in a single pass without intermediate list allocations.
