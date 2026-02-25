package com.chromadmx.simulation.network

import com.chromadmx.networking.model.UdpPacket
import com.chromadmx.networking.protocol.ArtNetCodec
import com.chromadmx.networking.protocol.ArtNetConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A fake Art-Net node for testing.
 *
 * Listens for ArtPoll packets on a [SimulatedNetwork] and responds with
 * ArtPollReply. Receives ArtDmx packets and stores channel data so tests
 * can assert on received DMX values.
 *
 * Runs as a coroutine that processes incoming packets continuously.
 *
 * @param transport      The simulated network transport to listen on
 * @param ipAddress      Simulated IP address (dotted string)
 * @param shortName      Short node name (up to 17 chars)
 * @param longName       Long node name (up to 63 chars)
 * @param universes      List of universe numbers this node handles
 * @param firmwareVersion Firmware version number
 * @param macAddress     6-byte MAC address
 */
class SimulatedDmxNode(
    private val transport: SimulatedNetwork,
    val ipAddress: String = "192.168.1.100",
    val shortName: String = "SimNode",
    val longName: String = "Simulated Art-Net Node",
    val universes: List<Int> = listOf(0),
    val firmwareVersion: Int = 0x0100,
    val macAddress: ByteArray = byteArrayOf(
        0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(),
        0xEF.toByte(), 0x00, 0x01
    )
) {
    /**
     * Received DMX data per universe.
     * Key = universe number, Value = 512-byte channel data.
     */
    private val _receivedDmx = mutableMapOf<Int, ByteArray>()
    private val dmxMutex = Mutex()

    /** Count of ArtPoll packets received. */
    private var _pollCount = 0

    /** Count of ArtDmx packets received. */
    private var _dmxCount = 0

    /** Count of ArtPollReply packets sent. */
    private var _replyCount = 0

    private var scope: CoroutineScope? = null
    private var listenJob: Job? = null

    /** Whether the node is currently running. */
    val isRunning: Boolean get() = scope != null

    /**
     * Start listening for Art-Net packets.
     */
    fun start() {
        if (isRunning) return

        val newScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        scope = newScope

        listenJob = newScope.launch {
            listenLoop()
        }
    }

    /**
     * Stop listening for Art-Net packets.
     */
    fun stop() {
        scope?.cancel()
        scope = null
        listenJob = null
    }

    /**
     * Process a single incoming packet.
     *
     * Exposed for direct testing without running the listen loop.
     * Returns true if the packet was recognized and handled.
     */
    suspend fun processPacket(packet: ByteArray, senderAddress: String): Boolean {
        if (!ArtNetCodec.hasValidHeader(packet)) return false

        return when (ArtNetCodec.readOpCode(packet)) {
            ArtNetConstants.OP_POLL -> {
                handleArtPoll(packet, senderAddress)
                true
            }
            ArtNetConstants.OP_DMX -> {
                handleArtDmx(packet)
                true
            }
            else -> false
        }
    }

    /**
     * Handle an ArtPoll packet by sending an ArtPollReply.
     */
    private suspend fun handleArtPoll(packet: ByteArray, senderAddress: String) {
        val poll = ArtNetCodec.decodeArtPoll(packet) ?: return
        _pollCount++

        val ipBytes = parseIpAddress(ipAddress)

        // Compute net/sub/universe from the first universe in the list
        val firstUniverse = universes.firstOrNull() ?: 0
        val netSwitch = (firstUniverse shr 8) and 0x7F
        val subSwitch = (firstUniverse shr 4) and 0x0F

        // Build swOut from universe list (up to 4 ports)
        val swOut = ByteArray(4)
        for (i in 0 until minOf(universes.size, 4)) {
            swOut[i] = (universes[i] and 0x0F).toByte()
        }

        val replyBytes = ArtNetCodec.encodeArtPollReply(
            ipAddress = ipBytes,
            port = ArtNetConstants.PORT,
            firmwareVersion = firmwareVersion,
            netSwitch = netSwitch,
            subSwitch = subSwitch,
            shortName = shortName,
            longName = longName,
            numPorts = universes.size,
            swIn = ByteArray(4),
            swOut = swOut,
            style = ArtNetConstants.STYLE_NODE,
            macAddress = macAddress,
            bindIp = ipBytes,
            status = 0x00
        )

        transport.send(replyBytes, senderAddress, ArtNetConstants.PORT)
        _replyCount++
    }

    /**
     * Handle an ArtDmx packet by storing the channel data.
     */
    private suspend fun handleArtDmx(packet: ByteArray) {
        val dmx = ArtNetCodec.decodeArtDmx(packet) ?: return

        // Only accept data for universes this node handles
        if (dmx.universe !in universes) return

        _dmxCount++

        dmxMutex.withLock {
            // Store the full 512-channel frame (pad if needed)
            val channels = ByteArray(512)
            dmx.data.copyInto(channels, 0, 0, minOf(dmx.data.size, 512))
            _receivedDmx[dmx.universe] = channels
        }
    }

    /**
     * Main listen loop - receives packets and dispatches them.
     */
    private suspend fun listenLoop() {
        val buffer = ByteArray(ArtNetConstants.ART_DMX_MAX_SIZE + 64)
        while (scope?.isActive == true) {
            try {
                val received: UdpPacket? = transport.receive(buffer, timeoutMs = 500)
                if (received != null) {
                    processPacket(received.data, received.address)
                }
            } catch (_: kotlinx.coroutines.CancellationException) {
                break
            } catch (_: Exception) {
                // Non-fatal: continue listening
            }
        }
    }

    // ------------------------------------------------------------------ //
    //  Test assertion helpers                                              //
    // ------------------------------------------------------------------ //

    /**
     * Get the latest received DMX data for a universe.
     * Returns null if no data has been received for that universe.
     */
    suspend fun getReceivedDmx(universe: Int): ByteArray? = dmxMutex.withLock {
        _receivedDmx[universe]?.copyOf()
    }

    /**
     * Get a specific channel value from the latest received DMX data.
     * Returns null if no data has been received for that universe.
     */
    suspend fun getChannelValue(universe: Int, channel: Int): Int? {
        require(channel in 0 until 512) { "Channel must be 0-511, got $channel" }
        val data = getReceivedDmx(universe) ?: return null
        return data[channel].toInt() and 0xFF
    }

    /**
     * Get RGB color values from 3 consecutive channels.
     */
    suspend fun getColorAt(universe: Int, startChannel: Int): Triple<Int, Int, Int>? {
        require(startChannel in 0..509) { "Start channel must be 0-509, got $startChannel" }
        val data = getReceivedDmx(universe) ?: return null
        return Triple(
            data[startChannel].toInt() and 0xFF,
            data[startChannel + 1].toInt() and 0xFF,
            data[startChannel + 2].toInt() and 0xFF
        )
    }

    /** Number of ArtPoll packets received. */
    val pollCount: Int get() = _pollCount

    /** Number of ArtDmx packets received. */
    val dmxCount: Int get() = _dmxCount

    /** Number of ArtPollReply packets sent. */
    val replyCount: Int get() = _replyCount

    /**
     * Reset all counters and received data.
     */
    suspend fun reset() {
        _pollCount = 0
        _dmxCount = 0
        _replyCount = 0
        dmxMutex.withLock {
            _receivedDmx.clear()
        }
    }

    companion object {
        /**
         * Parse a dotted IP address string into a 4-byte array.
         */
        fun parseIpAddress(ip: String): ByteArray {
            val parts = ip.split(".")
            require(parts.size == 4) { "Invalid IP address: $ip" }
            return ByteArray(4) { parts[it].toInt().toByte() }
        }
    }
}
