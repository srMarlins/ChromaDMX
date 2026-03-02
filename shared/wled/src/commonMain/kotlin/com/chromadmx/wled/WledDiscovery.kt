package com.chromadmx.wled

import com.chromadmx.core.model.DmxNode
import com.chromadmx.networking.FixtureDiscovery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * [FixtureDiscovery] implementation for WLED devices.
 *
 * Wraps a [WledMdnsBrowser] for mDNS discovery and a [WledApiClient] for enriching
 * discovered devices with full state data (LED count, firmware, segments, etc).
 *
 * Discovered WLED devices are also exposed as [DmxNode] instances via [discoveredNodes]
 * for compatibility with the rest of the ChromaDMX discovery pipeline.
 */
class WledDiscovery(
    private val browser: WledMdnsBrowser,
    private val apiClient: WledApiClient,
    private val scope: CoroutineScope,
) : FixtureDiscovery {

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredNodes = MutableStateFlow<List<DmxNode>>(emptyList())
    override val discoveredNodes: StateFlow<List<DmxNode>> = _discoveredNodes.asStateFlow()

    private val _wledDevices = MutableStateFlow<List<WledDevice>>(emptyList())

    /** WLED-specific device list for consumers that need full WLED data. */
    val wledDevices: StateFlow<List<WledDevice>> = _wledDevices.asStateFlow()

    private var collectionJob: Job? = null

    override fun startScan() {
        if (_isScanning.value) return
        _isScanning.value = true
        browser.startBrowse()

        collectionJob = scope.launch {
            browser.discoveredDevices.collect { devices ->
                val enriched = devices.map { device -> enrichDevice(device) }
                _wledDevices.value = enriched
                _discoveredNodes.value = enriched.map { it.toDmxNode() }
            }
        }
    }

    override fun stopScan() {
        browser.stopBrowse()
        collectionJob?.cancel()
        collectionJob = null
        _isScanning.value = false
    }

    /**
     * Attempt to enrich a discovered device with full state from the WLED JSON API.
     * Falls back to the original device data if the API call fails.
     */
    private suspend fun enrichDevice(device: WledDevice): WledDevice {
        val fullState = apiClient.getFullState(device.ipAddress) ?: return device
        return device.copy(
            name = fullState.info.name.ifEmpty { device.name },
            macAddress = fullState.info.mac.ifEmpty { device.macAddress },
            totalLeds = fullState.info.leds.count,
            segments = fullState.state.seg,
            firmwareVersion = fullState.info.ver.ifEmpty { device.firmwareVersion },
            isOnline = true,
        )
    }
}

/**
 * Convert a [WledDevice] to a [DmxNode] for use with the ChromaDMX discovery pipeline.
 *
 * Maps WLED concepts to DMX node concepts:
 * - Each WLED segment is treated as a separate "universe" (by segment ID)
 * - numPorts = number of segments
 * - style = 0 (Node)
 */
fun WledDevice.toDmxNode(): DmxNode = DmxNode(
    ipAddress = ipAddress,
    macAddress = macAddress,
    shortName = name.take(17),
    longName = "WLED: $name",
    firmwareVersion = firmwareVersion.hashCode(),
    numPorts = segments.size.coerceAtLeast(1),
    universes = if (segments.isEmpty()) listOf(0) else segments.map { it.id },
    style = 0,
    lastSeenMs = lastSeenMs,
)
