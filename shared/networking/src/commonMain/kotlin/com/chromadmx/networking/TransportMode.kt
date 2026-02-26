package com.chromadmx.networking

/**
 * Operating mode for the transport/discovery routers.
 */
enum class TransportMode {
    /** Use real hardware (Art-Net/sACN over UDP). */
    Real,
    /** Use simulated in-memory transport. */
    Simulated,
    /** Route to both real and simulated simultaneously. */
    Mixed
}
