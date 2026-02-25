package com.chromadmx.simulation.di

import com.chromadmx.simulation.camera.SimulatedCamera
import com.chromadmx.simulation.fixtures.RigPreset
import com.chromadmx.simulation.fixtures.SimulatedFixtureRig
import com.chromadmx.simulation.network.SimulatedDmxNode
import com.chromadmx.simulation.network.SimulatedNetwork
import com.chromadmx.simulation.network.SimulatedNetworkBus
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Koin module that provides simulated implementations for hardware-free testing.
 *
 * Swaps real hardware dependencies with simulated ones:
 * - [SimulatedNetwork] replaces PlatformUdpTransport
 * - [SimulatedDmxNode] provides fake Art-Net nodes
 * - [SimulatedFixtureRig] provides preset fixture layouts
 * - [SimulatedCamera] provides synthetic frames
 *
 * Usage:
 * ```
 * // In test setup or debug builds
 * startKoin {
 *     modules(simulationModule)
 * }
 * ```
 *
 * Or combine with production modules, overriding real transports:
 * ```
 * startKoin {
 *     modules(networkingModule, simulationModule)
 *     // simulationModule's allowOverride replaces real transport
 * }
 * ```
 */
val simulationModule: Module = module {

    // ---- Network Bus ----
    // Shared bus connecting all simulated transports
    single { SimulatedNetworkBus() }

    // ---- Controller Transport ----
    // The transport used by the controller (NodeDiscovery, DmxOutputService)
    // Named "controller" to distinguish from node transports
    single(named("controller")) {
        SimulatedNetwork().also { it.connectToBus(get()) }
    }

    // ---- Simulated DMX Node ----
    // A default simulated node on universe 0
    single {
        val nodeTransport = SimulatedNetwork().also { it.connectToBus(get()) }
        SimulatedDmxNode(
            transport = nodeTransport,
            ipAddress = "192.168.1.100",
            shortName = "SimNode",
            longName = "Simulated Art-Net Node",
            universes = listOf(0)
        )
    }

    // ---- Fixture Rig ----
    // Default preset is SMALL_DJ; can be overridden
    single { SimulatedFixtureRig(RigPreset.SMALL_DJ) }

    // ---- Camera ----
    single {
        SimulatedCamera(
            width = 640,
            height = 480,
            noiseLevel = 0.02f,
            blobRadius = 15f,
            ambientLevel = 10
        )
    }
}

/**
 * Create a simulation module with a specific rig preset.
 *
 * @param preset The rig preset to use
 * @param nodeCount Number of simulated DMX nodes to create
 * @param universes Universes the node(s) should handle
 * @return Koin module configured for the specified scenario
 */
fun simulationModule(
    preset: RigPreset = RigPreset.SMALL_DJ,
    nodeCount: Int = 1,
    universes: List<Int> = listOf(0),
    packetLossRate: Float = 0f,
    latencyMs: Long = 0L,
    cameraWidth: Int = 640,
    cameraHeight: Int = 480
): Module = module {

    // Network bus
    single { SimulatedNetworkBus() }

    // Controller transport with configurable fault injection
    single(named("controller")) {
        SimulatedNetwork(
            packetLossRate = packetLossRate,
            latencyMs = latencyMs
        ).also { it.connectToBus(get()) }
    }

    // Simulated DMX nodes
    for (i in 0 until nodeCount) {
        val qualifier = if (nodeCount == 1) null else named("node-$i")
        single(qualifier) {
            val nodeTransport = SimulatedNetwork().also { it.connectToBus(get()) }
            SimulatedDmxNode(
                transport = nodeTransport,
                ipAddress = "192.168.1.${100 + i}",
                shortName = "SimNode${i + 1}",
                longName = "Simulated Art-Net Node ${i + 1}",
                universes = universes
            )
        }
    }

    // Fixture rig
    single { SimulatedFixtureRig(preset) }

    // Camera
    single {
        SimulatedCamera(
            width = cameraWidth,
            height = cameraHeight,
            noiseLevel = 0.02f,
            blobRadius = 15f,
            ambientLevel = 10
        )
    }
}
