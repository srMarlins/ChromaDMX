package com.chromadmx.simulation.network

/**
 * Predefined network behavior profiles for simulation testing.
 */
enum class NetworkProfile {
    /** All nodes respond reliably, low latency. */
    Stable,
    /** Intermittent packet loss, variable latency. */
    Flaky,
    /** One or more nodes go offline after a period. */
    PartialFailure,
    /** High latency, degraded responses. */
    Overloaded
}
