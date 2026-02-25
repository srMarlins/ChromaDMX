## 2025-02-14 - Memory DoS in NodeDiscovery
**Vulnerability:** NodeDiscovery stored unlimited DMX nodes based on spoofable nodeKey (IP/MAC).
**Learning:** UDP-based discovery protocols are vulnerable to spoofing, leading to unbounded memory usage if not capped.
**Prevention:** Always enforce a hard limit (maxNodes) on network-populated collections and use an eviction policy (e.g., oldest seen) to handle overflow.
