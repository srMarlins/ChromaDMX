package com.chromadmx.networking

import com.chromadmx.networking.model.DmxNode
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction over fixture/node discovery — both real Art-Net and simulated.
 *
 * Implementors include [com.chromadmx.networking.discovery.NodeDiscovery]
 * for real Art-Net polling and SimulatedDiscovery for testing.
 */
interface FixtureDiscovery {
    fun startScan()
    fun stopScan()
    val discoveredNodes: StateFlow<List<DmxNode>>
    val isScanning: StateFlow<Boolean>
}
