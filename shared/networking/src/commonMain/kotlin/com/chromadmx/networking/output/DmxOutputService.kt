package com.chromadmx.networking.output

import com.chromadmx.networking.protocol.ArtNetCodec
import com.chromadmx.networking.protocol.ArtNetConstants
import com.chromadmx.networking.protocol.SacnCodec
import com.chromadmx.networking.protocol.SacnConstants
import com.chromadmx.networking.transport.UdpTransport
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.AtomicRef
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * DMX output protocol.
 */
enum class DmxProtocol {
    ART_NET,
    SACN
}

/**
 * High-frequency DMX output service.
 *
 * Runs a 40Hz (25ms) broadcast loop that reads the latest frame data
 * from an atomic reference and sends one ArtDmx or sACN packet per
 * universe per frame via [UdpTransport].
 *
 * Usage:
 * ```
 * val service = DmxOutputService(transport)
 * service.start()
 * service.updateFrame(mapOf(0 to channelData0, 1 to channelData1))
 * // ... later
 * service.stop()
 * ```
 *
 * The service supports multi-universe output: the frame map keys are
 * universe numbers and values are 512-byte channel data arrays.
 *
 * @param transport       UDP transport for sending packets
 * @param targetAddress   Destination IP for Art-Net (default broadcast)
 * @param protocol        Which DMX-over-IP protocol to use
 * @param frameRateHz     Output frame rate (default 40Hz)
 * @param sourceName      sACN source name (used only for sACN protocol)
 * @param sacnCid         sACN Component ID, 16-byte UUID (used only for sACN)
 * @param sacnPriority    sACN priority 0-200 (used only for sACN, default 100)
 */
class DmxOutputService(
    private val transport: UdpTransport,
    private val targetAddress: String = ArtNetConstants.BROADCAST_ADDRESS,
    private val protocol: DmxProtocol = DmxProtocol.ART_NET,
    private val frameRateHz: Int = DEFAULT_FRAME_RATE_HZ,
    private val sourceName: String = "ChromaDMX",
    private val sacnCid: ByteArray = ByteArray(SacnConstants.CID_SIZE),
    private val sacnPriority: Int = SacnConstants.DEFAULT_PRIORITY
) {
    /**
     * Atomic reference to the latest frame data.
     * Map<universeNumber, 512-byte channel data>.
     */
    private val frameRef: AtomicRef<Map<Int, ByteArray>> = atomic(emptyMap())

    /** Rolling Art-Net sequence counter (1..255, 0 disables reordering). */
    private var artNetSequence: Int = 1

    /** Rolling sACN sequence counter (0..255). */
    private var sacnSequence: Int = 0

    private var scope: CoroutineScope? = null
    private var outputJob: Job? = null

    /** Whether the output loop is running. */
    val isRunning: Boolean get() = scope != null

    /** Interval between frames in milliseconds. */
    val frameIntervalMs: Long get() = (1000L / frameRateHz)

    /** Number of frames sent since [start]. */
    var frameCount: Long = 0L
        private set

    /**
     * Update the frame data for one or more universes.
     *
     * This is lock-free: the new map is atomically published and
     * picked up by the output loop on the next frame.
     *
     * @param universeData Map of universe number to 512-byte channel data
     */
    fun updateFrame(universeData: Map<Int, ByteArray>) {
        frameRef.value = universeData
    }

    /**
     * Update a single universe's channel data.
     *
     * @param universe Universe number
     * @param data     512-byte channel data
     */
    fun updateUniverse(universe: Int, data: ByteArray) {
        val current = frameRef.value.toMutableMap()
        current[universe] = data
        frameRef.value = current
    }

    /**
     * Start the 40Hz output loop.
     */
    fun start() {
        if (isRunning) return

        frameCount = 0L
        artNetSequence = 1
        sacnSequence = 0

        val newScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        scope = newScope

        outputJob = newScope.launch {
            outputLoop()
        }
    }

    /**
     * Stop the output loop.
     */
    fun stop() {
        scope?.cancel()
        scope = null
        outputJob = null
    }

    // ------------------------------------------------------------------ //
    //  Output loop                                                        //
    // ------------------------------------------------------------------ //

    private suspend fun outputLoop() {
        val interval = frameIntervalMs

        while (scope?.isActive == true) {
            val startTime = monotonicTimeMs()

            try {
                sendFrame()
                frameCount++
            } catch (_: CancellationException) {
                break
            } catch (_: Exception) {
                // Non-fatal: skip this frame
            }

            // Maintain consistent frame rate
            val elapsed = monotonicTimeMs() - startTime
            val remaining = interval - elapsed
            if (remaining > 0) {
                delay(remaining)
            }
        }
    }

    /**
     * Send all universe data for one frame.
     * Called by the output loop and also exposed for testing.
     */
    internal suspend fun sendFrame() {
        val frame = frameRef.value
        if (frame.isEmpty()) return

        for ((universe, data) in frame) {
            when (protocol) {
                DmxProtocol.ART_NET -> sendArtDmx(universe, data)
                DmxProtocol.SACN -> sendSacn(universe, data)
            }
        }
    }

    private suspend fun sendArtDmx(universe: Int, data: ByteArray) {
        val packet = ArtNetCodec.encodeArtDmx(
            sequence = artNetSequence.toByte(),
            physical = 0,
            universe = universe,
            data = data
        )
        transport.send(packet, targetAddress, ArtNetConstants.PORT)
        artNetSequence = if (artNetSequence >= 255) 1 else artNetSequence + 1
    }

    private suspend fun sendSacn(universe: Int, data: ByteArray) {
        val multicastAddr = SacnConstants.multicastAddress(
            if (universe == 0) 1 else universe  // sACN universes start at 1
        )
        val packet = SacnCodec.encode(
            cid = sacnCid,
            sourceName = sourceName,
            priority = sacnPriority,
            sequence = sacnSequence,
            universe = if (universe == 0) 1 else universe,
            dmxData = data
        )
        transport.send(packet, multicastAddr, SacnConstants.PORT)
        sacnSequence = (sacnSequence + 1) and 0xFF
    }

    companion object {
        /** Default output frame rate: 40Hz (25ms per frame). */
        const val DEFAULT_FRAME_RATE_HZ: Int = 40

        /** Minimum frame rate. */
        const val MIN_FRAME_RATE_HZ: Int = 1

        /** Maximum frame rate (Art-Net spec limit is ~44Hz). */
        const val MAX_FRAME_RATE_HZ: Int = 44
    }
}

/**
 * Platform-agnostic monotonic clock in milliseconds for frame timing.
 */
internal expect fun monotonicTimeMs(): Long
