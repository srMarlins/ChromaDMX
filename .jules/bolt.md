## 2025-05-15 - ArrayDeque boxing in hot loop
**Learning:** In a high-frequency inner loop (`BlobDetector.floodFill`), `ArrayDeque<Int>` forces primitive auto-boxing and frequent heap allocations, leading to high GC pressure.
**Action:** Replace `ArrayDeque<Int>` with a pre-allocated primitive `IntArray` stack to achieve zero allocations in the flood-fill path.
