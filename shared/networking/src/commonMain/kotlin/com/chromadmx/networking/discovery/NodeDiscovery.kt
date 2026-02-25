package com.chromadmx.networking.discovery

import com.chromadmx.networking.model.DmxNode
import com.chromadmx.networking.model.UdpPacket
import com.chromadmx.networking.protocol.ArtNetCodec
import com.chromadmx.networking.protocol.ArtNetConstants
import com.chromadmx.networking.transport.PlatformUdpTransport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Art-Net node discovery service.
 *
 * Periodically broadcasts ArtPoll packets and collects ArtPollReply
 * responses to maintain a live registry of discovered DMX nodes.
 *
 * Usage:
 * ```
 * val discovery = NodeDiscovery(transport)
 * discovery.start()
 * discovery.nodes.collect { nodes -> ... }
 * discovery.stop()
 * ```
 *
 * @param transport    UDP transport for sending/receiving packets
 * @param pollIntervalMs  Interval between ArtPoll broadcasts (default 3000ms)
 * @param nodeTimeoutMs   Time before a node is considered offline (default 10000ms)
 */
class NodeDiscovery(
    private val transport: PlatformUdpTransport,
    private val pollIntervalMs: Long = DEFAULT_POLL_INTERVAL_MS,
    private val nodeTimeoutMs: Long = DmxNode.DEFAULT_TIMEOUT_MS,
    private val maxNodes: Int = DEFAULT_MAX_NODES
) {

    private val _nodes = MutableStateFlow<Map<String, DmxNode>>(emptyMap())

    /** Live registry of discovered nodes, keyed by [DmxNode.nodeKey]. */
    val nodes: StateFlow<Map<String, DmxNode>> = _nodes.asStateFlow()

    /** Convenience: current list of discovered nodes. */
    val nodeList: List<DmxNode> get() = _nodes.value.values.toList()

    private var scope: CoroutineScope? = null
    private var pollJob: Job? = null
    private var listenJob: Job? = null

    /** Whether discovery is currently running. */
    val isRunning: Boolean get() = scope != null

    /**
     * Start the discovery service.
     *
     * Launches two coroutines:
     * 1. Poll loop: broadcasts ArtPoll every [pollIntervalMs] and prunes stale nodes
     * 2. Listen loop: receives ArtPollReply packets and updates the registry
     */
    fun start() {
        if (isRunning) return

        val newScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        scope = newScope

        pollJob = newScope.launch {
            pollLoop()
        }

        listenJob = newScope.launch {
            listenLoop()
        }
    }

    /**
     * Stop the discovery service and clear the node registry.
     */
    fun stop() {
        scope?.cancel()
        scope = null
        pollJob = null
        listenJob = null
        _nodes.value = emptyMap()
    }

    /**
     * Send a single ArtPoll broadcast immediately.
     */
    suspend fun sendPoll() {
        val pollPacket = ArtNetCodec.encodeArtPoll(
            flags = 0x02  // Request ArtPollReply from targeted nodes and diagnostics
        )
        transport.send(pollPacket, ArtNetConstants.BROADCAST_ADDRESS, ArtNetConstants.PORT)
    }

    /**
     * Process a received ArtPollReply packet and update the node registry.
     *
     * This is also exposed for testing — callers can feed in packets
     * without running the full listen loop.
     *
     * @param packet Raw packet bytes
     * @param currentTimeMs Current system time for last-seen tracking
     * @return the updated [DmxNode], or null if the packet was invalid
     */
    fun processReply(packet: ByteArray, currentTimeMs: Long): DmxNode? {
        val reply = ArtNetCodec.decodeArtPollReply(packet) ?: return null

        val universes = buildList {
            for (i in 0 until minOf(reply.numPorts, 4)) {
                val universe = ((reply.netSwitch and 0x7F) shl 8) or
                    ((reply.subSwitch and 0x0F) shl 4) or
                    (reply.swOut[i].toInt() and 0x0F)
                add(universe)
            }
        }

        val node = DmxNode(
            ipAddress = reply.ipString,
            macAddress = reply.macString,
            shortName = reply.shortName,
            longName = reply.longName,
            firmwareVersion = reply.firmwareVersion,
            numPorts = reply.numPorts,
            universes = universes,
            style = reply.style.toInt() and 0xFF,
            lastSeenMs = currentTimeMs
        )

        _nodes.update { currentNodes ->
            val mutableNodes = currentNodes.toMutableMap()

            // If this is a new node and we've reached the capacity limit,
            // evict the node that hasn't been seen for the longest time.
            if (!mutableNodes.containsKey(node.nodeKey) && mutableNodes.size >= maxNodes) {
                val oldestKey = mutableNodes.values.minByOrNull { it.lastSeenMs }?.nodeKey
                if (oldestKey != null) {
                    mutableNodes.remove(oldestKey)
                }
            }

            mutableNodes[node.nodeKey] = node
            mutableNodes
        }

        return node
    }

    /**
     * Remove nodes that have not been seen within [nodeTimeoutMs].
     */
    fun pruneStaleNodes(currentTimeMs: Long) {
        _nodes.update { current ->
            val alive = current.filterValues { it.isAlive(currentTimeMs, nodeTimeoutMs) }
            if (alive.size != current.size) alive else current
        }
    }

    // ------------------------------------------------------------------ //
    //  Internal loops                                                     //
    // ------------------------------------------------------------------ //

    private suspend fun pollLoop() {
        while (scope?.isActive == true) {
            try {
                sendPoll()
                pruneStaleNodes(currentTimeMillis())
            } catch (_: CancellationException) {
                break
            } catch (_: Exception) {
                // Non-fatal: log and continue
            }
            delay(pollIntervalMs)
        }
    }

    private suspend fun listenLoop() {
        val buffer = ByteArray(ArtNetConstants.ART_POLL_REPLY_SIZE + 64)
        while (scope?.isActive == true) {
            try {
                val received: UdpPacket? = transport.receive(buffer, RECEIVE_TIMEOUT_MS)
                if (received != null) {
                    processReply(received.data, currentTimeMillis())
                }
            } catch (_: CancellationException) {
                break
            } catch (_: Exception) {
                // Non-fatal: continue listening
            }
        }
    }

    companion object {
        /** Default poll interval: 3 seconds (Art-Net spec recommends 2.5-3s). */
        const val DEFAULT_POLL_INTERVAL_MS: Long = 3_000L

        /** Receive timeout per iteration. */
        const val RECEIVE_TIMEOUT_MS: Long = 1_000L

        /** Maximum number of discovered nodes to track to prevent memory exhaustion. */
        const val DEFAULT_MAX_NODES: Int = 256
    }
}

/**
 * Platform-agnostic current time in milliseconds.
 *
 * Uses `kotlinx.datetime` epoch milliseconds if available,
 * falls back to a reasonable default.
 */
internal expect fun currentTimeMillis(): Long
