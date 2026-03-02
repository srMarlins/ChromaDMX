package com.chromadmx.wled

import com.chromadmx.networking.ConnectionState
import com.chromadmx.networking.DmxTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Maps a DMX universe to a WLED device and its segment-to-channel assignments.
 *
 * @param deviceIp IP address of the WLED device handling this universe.
 * @param segmentMappings Each pair maps a WLED segment ID to the DMX channel range
 *   (within the 512-channel universe) that drives it. The channel range contains
 *   consecutive RGB triplets.
 */
data class WledUniverseMapping(
    val deviceIp: String,
    val segmentMappings: List<Pair<Int, IntRange>>,
)

/**
 * Registry of WLED devices that have been adopted into the lighting rig
 * and their universe/segment assignments.
 */
interface WledDeviceRegistry {
    val adoptedDevices: StateFlow<List<WledDevice>>
    fun getUniverseMapping(universe: Int): WledUniverseMapping?
    fun adoptDevice(device: WledDevice, universe: Int)
    fun removeDevice(ip: String)
}

/**
 * [DmxTransport] implementation that bridges DMX universe frames to WLED devices
 * via the WLED JSON HTTP API.
 *
 * For each incoming DMX frame the transport:
 * 1. Looks up the [WledUniverseMapping] from the [WledDeviceRegistry].
 * 2. Extracts RGB values from the channel data for each mapped segment.
 * 3. Sends `setSegmentColor` calls through the [WledApiClient].
 *
 * This is a "thin bridge" -- it does not buffer or rate-limit.  The engine's
 * 40 Hz output loop already provides pacing.
 */
class WledTransport(
    private val apiClient: WledApiClient,
    private val registry: WledDeviceRegistry,
    private val scope: CoroutineScope,
) : DmxTransport {

    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var _isRunning = false
    override val isRunning: Boolean get() = _isRunning

    override fun start() {
        _isRunning = true
        val hasDevices = registry.adoptedDevices.value.isNotEmpty()
        _connectionState.value = if (hasDevices) ConnectionState.Connected else ConnectionState.Disconnected
    }

    override fun stop() {
        _isRunning = false
        _connectionState.value = ConnectionState.Disconnected
    }

    override fun sendFrame(universe: Int, channels: ByteArray) {
        val mapping = registry.getUniverseMapping(universe) ?: return
        val ip = mapping.deviceIp

        for ((segmentId, channelRange) in mapping.segmentMappings) {
            val startIndex = channelRange.first
            // Need at least 3 bytes (R, G, B) starting at startIndex
            if (startIndex + 2 >= channels.size) continue

            val r = channels[startIndex].toInt() and 0xFF
            val g = channels[startIndex + 1].toInt() and 0xFF
            val b = channels[startIndex + 2].toInt() and 0xFF

            scope.launch {
                apiClient.setSegmentColor(ip, segmentId, r, g, b)
            }
        }
    }

    override fun updateFrame(universeData: Map<Int, ByteArray>) {
        for ((universe, channels) in universeData) {
            sendFrame(universe, channels)
        }
    }
}
