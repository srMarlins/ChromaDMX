package com.chromadmx.simulation.network

import com.chromadmx.networking.model.UdpPacket
import com.chromadmx.networking.transport.UdpTransport
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random

/**
 * Loopback UDP transport for testing.
 *
 * Routes packets locally without touching the real network.
 * Configurable packet loss rate and latency for fault injection testing.
 *
 * All packets sent via [send] are deposited into the internal channel
 * and can be retrieved via [receive], simulating a local network loop.
 *
 * Multiple [SimulatedNetwork] instances can be connected together via
 * a [SimulatedNetworkBus] to simulate multi-node scenarios.
 *
 * @param packetLossRate Probability of dropping a packet (0.0 = no loss, 1.0 = drop all)
 * @param latencyMs      Simulated latency in milliseconds added to receive
 * @param random         Random source for packet loss decisions (injectable for tests)
 */
class SimulatedNetwork(
    val packetLossRate: Float = 0f,
    val latencyMs: Long = 0L,
    private val random: Random = Random.Default
) : UdpTransport {
    /**
     * Internal buffer for packets routed to this transport.
     * Channel capacity is generous to avoid blocking senders.
     */
    private val inboundPackets = Channel<UdpPacket>(capacity = 256)

    /** All packets that were successfully sent (not dropped), for test assertions. */
    private val _sentPackets = mutableListOf<UdpPacket>()
    private val sentMutex = Mutex()

    /** Network bus this transport is connected to, if any. */
    private var bus: SimulatedNetworkBus? = null

    /** Whether this transport has been closed. */
    private var closed = false

    /**
     * Send a UDP datagram.
     *
     * Applies packet loss simulation. If the packet survives, it is
     * routed to the bus (if connected) or to this transport's own
     * inbound queue (loopback mode).
     */
    override suspend fun send(data: ByteArray, address: String, port: Int) {
        if (closed) return

        // Simulate packet loss
        if (packetLossRate > 0f && random.nextFloat() < packetLossRate) {
            return
        }

        val packet = UdpPacket(
            data = data.copyOf(),
            address = address,
            port = port
        )

        sentMutex.withLock {
            _sentPackets.add(packet)
        }

        // Route through bus if connected, otherwise loopback
        val networkBus = bus
        if (networkBus != null) {
            networkBus.route(packet, sender = this)
        } else {
            inboundPackets.trySend(packet)
        }
    }

    /**
     * Receive a UDP datagram.
     *
     * Blocks (suspending) until a packet arrives or the timeout expires.
     * Latency simulation is applied via an additional delay.
     *
     * @param buffer     Pre-allocated buffer (size used as max packet size)
     * @param timeoutMs  Maximum wait time in milliseconds
     * @return received packet, or null on timeout
     */
    override suspend fun receive(buffer: ByteArray, timeoutMs: Long): UdpPacket? {
        if (closed) return null

        val effectiveTimeout = timeoutMs + latencyMs

        return withTimeoutOrNull(effectiveTimeout) {
            if (latencyMs > 0) {
                kotlinx.coroutines.delay(latencyMs)
            }
            val packet = inboundPackets.receiveCatching().getOrNull()
            if (packet != null && packet.data.size <= buffer.size) {
                packet.data.copyInto(buffer)
                packet
            } else {
                packet
            }
        }
    }

    /**
     * Deliver a packet to this transport's inbound queue.
     * Used by [SimulatedNetworkBus] to route packets between transports.
     */
    internal fun deliver(packet: UdpPacket) {
        if (!closed) {
            inboundPackets.trySend(packet)
        }
    }

    /**
     * Close the transport and release resources.
     */
    override fun close() {
        closed = true
        inboundPackets.close()
    }

    /**
     * Connect this transport to a network bus for multi-endpoint simulation.
     */
    fun connectToBus(networkBus: SimulatedNetworkBus) {
        bus = networkBus
        networkBus.register(this)
    }

    /**
     * Get all sent packets (for test assertions). Thread-safe snapshot.
     */
    suspend fun sentPackets(): List<UdpPacket> = sentMutex.withLock {
        _sentPackets.toList()
    }

    /**
     * Get the count of sent packets without copying.
     */
    suspend fun sentPacketCount(): Int = sentMutex.withLock {
        _sentPackets.size
    }

    /**
     * Clear the sent packet history.
     */
    suspend fun clearSentPackets() = sentMutex.withLock {
        _sentPackets.clear()
    }
}

/**
 * A simulated network bus connecting multiple [SimulatedNetwork] transports.
 *
 * When a transport sends a packet, the bus delivers it to all other
 * registered transports (simulating broadcast). This enables multi-node
 * testing scenarios (e.g., controller discovers multiple simulated nodes).
 */
class SimulatedNetworkBus {

    private val transports = mutableListOf<SimulatedNetwork>()

    /**
     * Register a transport on this bus.
     * Should be called during test setup before concurrent operations begin.
     */
    fun register(transport: SimulatedNetwork) {
        if (transport !in transports) {
            transports.add(transport)
        }
    }

    /**
     * Route a packet from [sender] to all other transports on the bus.
     */
    internal fun route(packet: UdpPacket, sender: SimulatedNetwork) {
        for (transport in transports) {
            if (transport !== sender) {
                transport.deliver(packet)
            }
        }
    }

    /**
     * Get the number of connected transports.
     */
    fun transportCount(): Int = transports.size
}
