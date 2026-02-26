package com.chromadmx.simulation.network

import com.chromadmx.networking.FixtureDiscovery
import com.chromadmx.networking.model.DmxNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Simulated fixture discovery that emits fake nodes with staggered timing
 * to mimic real network discovery behavior.
 *
 * @param nodes           Pre-configured list of nodes to "discover"
 * @param baseDelayMs     Initial delay before first node appears
 * @param perNodeDelayMs  Incremental delay between each node discovery
 * @param coroutineContext Context for launching coroutines (injectable for tests)
 */
class SimulatedDiscovery(
    private val nodes: List<DmxNode> = defaultNodes(),
    private val baseDelayMs: Long = 150L,
    private val perNodeDelayMs: Long = 80L,
    private val coroutineContext: CoroutineContext = Dispatchers.Default,
) : FixtureDiscovery {

    private val _discoveredNodes = MutableStateFlow<List<DmxNode>>(emptyList())
    override val discoveredNodes: StateFlow<List<DmxNode>> = _discoveredNodes.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var scope: CoroutineScope? = null
    private var scanCounter = 0L

    override fun startScan() {
        stopScan()
        _discoveredNodes.value = emptyList()
        _isScanning.value = true
        scope = CoroutineScope(SupervisorJob() + coroutineContext)
        scope!!.launch {
            delay(baseDelayMs)
            val discovered = mutableListOf<DmxNode>()
            for ((index, node) in nodes.withIndex()) {
                delay(perNodeDelayMs * (index + 1))
                scanCounter++
                // Use incrementing counter for timestamps since kotlinx-datetime
                // is not available — the values are relative, not absolute.
                discovered.add(node.copy(
                    lastSeenMs = scanCounter * 1000L,
                    firstSeenMs = scanCounter * 1000L,
                    latencyMs = 5L + (index * 3L)
                ))
                _discoveredNodes.value = discovered.toList()
            }
            _isScanning.value = false
        }
    }

    override fun stopScan() {
        scope?.cancel()
        scope = null
        _isScanning.value = false
    }

    companion object {
        fun defaultNodes(): List<DmxNode> = listOf(
            DmxNode(
                ipAddress = "192.168.1.100",
                macAddress = "de:ad:be:ef:00:01",
                shortName = "SimNode-1",
                longName = "Simulated Art-Net Node 1",
                numPorts = 1,
                universes = listOf(0),
            ),
            DmxNode(
                ipAddress = "192.168.1.101",
                macAddress = "de:ad:be:ef:00:02",
                shortName = "SimNode-2",
                longName = "Simulated Art-Net Node 2",
                numPorts = 1,
                universes = listOf(1),
            ),
            DmxNode(
                ipAddress = "192.168.1.102",
                macAddress = "de:ad:be:ef:00:03",
                shortName = "SimNode-3",
                longName = "Simulated Art-Net Node 3",
                numPorts = 2,
                universes = listOf(2, 3),
            ),
        )
    }
}
