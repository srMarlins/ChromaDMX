package com.chromadmx.core.model

import kotlinx.serialization.Serializable

/**
 * Represents a discovered Art-Net or sACN node on the network.
 *
 * Built from ArtPollReply packets during node discovery.
 *
 * @property ipAddress   Node IP address (dotted string, e.g. "192.168.1.100")
 * @property macAddress  Node MAC address (colon hex, e.g. "aa:bb:cc:dd:ee:ff")
 * @property shortName   Short node name (up to 17 characters)
 * @property longName    Long node name (up to 63 characters)
 * @property firmwareVersion Firmware version reported by the node
 * @property numPorts    Number of DMX ports on the node
 * @property universes   List of universe numbers this node handles
 * @property style       Node style code (0 = Node, 1 = Controller)
 * @property lastSeenMs  System time (ms) when this node was last seen
 * @property firstSeenMs System time (ms) when this node was first discovered
 * @property latencyMs   Round-trip time (ms) from last ArtPoll/Reply exchange
 */
@Serializable
data class DmxNode(
    val ipAddress: String,
    val macAddress: String = "",
    val shortName: String = "",
    val longName: String = "",
    val firmwareVersion: Int = 0,
    val numPorts: Int = 0,
    val universes: List<Int> = emptyList(),
    val style: Int = 0,
    val lastSeenMs: Long = 0L,
    val firstSeenMs: Long = 0L,
    val latencyMs: Long = 0L
) {
    /**
     * Unique key for this node in the device registry.
     * MAC address is preferred; falls back to IP if MAC is empty.
     */
    val nodeKey: String
        get() = macAddress.ifEmpty { ipAddress }

    /**
     * Whether this node has been seen within the given timeout.
     */
    fun isAlive(currentTimeMs: Long, timeoutMs: Long = DEFAULT_TIMEOUT_MS): Boolean {
        return (currentTimeMs - lastSeenMs) < timeoutMs
    }

    companion object {
        /** Default timeout for considering a node "alive" (10 seconds). */
        const val DEFAULT_TIMEOUT_MS: Long = 10_000L

        /** Latency threshold (ms) for "degraded" health status. */
        const val LATENCY_THRESHOLD_MS: Long = 150L

        /** Time since last seen (ms) before marking health as "degraded". */
        const val DEGRADED_TIMEOUT_MS: Long = 5_000L

        /** Time since last seen (ms) before marking health as "lost". */
        const val LOST_TIMEOUT_MS: Long = 8_000L
    }
}
