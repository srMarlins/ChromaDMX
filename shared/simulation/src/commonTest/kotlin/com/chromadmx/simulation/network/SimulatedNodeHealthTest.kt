package com.chromadmx.simulation.network

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SimulatedNodeHealthTest {

    private val testNodeIps = listOf("192.168.1.100", "192.168.1.101", "192.168.1.102")

    // ------------------------------------------------------------------ //
    //  Stable profile                                                     //
    // ------------------------------------------------------------------ //

    @Test
    fun stableProfile_emitsNoEvents() = runTest {
        val health = SimulatedNodeHealth(
            profile = NetworkProfile.Stable,
            nodeIps = testNodeIps,
            coroutineContext = UnconfinedTestDispatcher(testScheduler),
        )

        val events = mutableListOf<NodeHealthEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            health.events.collect { events.add(it) }
        }

        health.start()
        advanceTimeBy(5000)
        health.stop()
        job.cancel()

        assertEquals(0, events.size)
    }

    // ------------------------------------------------------------------ //
    //  Flaky profile                                                      //
    // ------------------------------------------------------------------ //

    @Test
    fun flakyProfile_emitsLatencyOrLossEvents() = runTest {
        val health = SimulatedNodeHealth(
            profile = NetworkProfile.Flaky,
            nodeIps = testNodeIps,
            random = Random(42),
            coroutineContext = UnconfinedTestDispatcher(testScheduler),
        )

        val events = mutableListOf<NodeHealthEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            health.events.collect { events.add(it) }
        }

        health.start()
        // Flaky emits events every 2-5 seconds; advance enough for several
        advanceTimeBy(15000)
        health.stop()
        job.cancel()

        assertTrue(events.isNotEmpty(), "Flaky profile should emit at least one event")

        // All events should be LatencySpike or PacketLoss
        for (event in events) {
            assertTrue(
                event is NodeHealthEvent.LatencySpike || event is NodeHealthEvent.PacketLoss,
                "Unexpected event type: $event"
            )
        }
    }

    // ------------------------------------------------------------------ //
    //  Partial failure profile                                            //
    // ------------------------------------------------------------------ //

    @Test
    fun partialFailureProfile_dropsANode() = runTest {
        val health = SimulatedNodeHealth(
            profile = NetworkProfile.PartialFailure,
            nodeIps = testNodeIps,
            random = Random(123),
            coroutineContext = UnconfinedTestDispatcher(testScheduler),
        )

        val events = mutableListOf<NodeHealthEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            health.events.collect { events.add(it) }
        }

        health.start()
        // PartialFailure drops after 5-15 seconds, may recover after 10-30s more
        advanceTimeBy(50_000)
        health.stop()
        job.cancel()

        // Should have at least a NodeDropped event
        val dropEvents = events.filterIsInstance<NodeHealthEvent.NodeDropped>()
        assertTrue(dropEvents.isNotEmpty(), "PartialFailure should drop at least one node")

        // The dropped node IP should be from the configured list
        assertTrue(dropEvents[0].nodeIp in testNodeIps)
    }

    // ------------------------------------------------------------------ //
    //  Overloaded profile                                                 //
    // ------------------------------------------------------------------ //

    @Test
    fun overloadedProfile_emitsLatencyForAllNodes() = runTest {
        val health = SimulatedNodeHealth(
            profile = NetworkProfile.Overloaded,
            nodeIps = testNodeIps,
            random = Random(99),
            coroutineContext = UnconfinedTestDispatcher(testScheduler),
        )

        val events = mutableListOf<NodeHealthEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            health.events.collect { events.add(it) }
        }

        health.start()
        // Overloaded emits every 1-3 seconds for all nodes
        advanceTimeBy(10_000)
        health.stop()
        job.cancel()

        assertTrue(events.isNotEmpty(), "Overloaded profile should emit events")

        // All events should be LatencySpike
        val latencyEvents = events.filterIsInstance<NodeHealthEvent.LatencySpike>()
        assertTrue(latencyEvents.isNotEmpty())

        // Should have events for multiple node IPs
        val affectedIps = latencyEvents.map { it.nodeIp }.toSet()
        assertTrue(affectedIps.size > 1, "Overloaded should affect multiple nodes, got: $affectedIps")
    }

    // ------------------------------------------------------------------ //
    //  Start / Stop lifecycle                                             //
    // ------------------------------------------------------------------ //

    @Test
    fun stop_preventsNewEvents() = runTest {
        val health = SimulatedNodeHealth(
            profile = NetworkProfile.Flaky,
            nodeIps = testNodeIps,
            random = Random(42),
            coroutineContext = UnconfinedTestDispatcher(testScheduler),
        )

        health.start()
        health.stop()

        val events = mutableListOf<NodeHealthEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            health.events.collect { events.add(it) }
        }

        advanceTimeBy(10_000)
        job.cancel()

        assertEquals(0, events.size, "No events should be emitted after stop()")
    }

    @Test
    fun start_afterStop_restartsEventGeneration() = runTest {
        val health = SimulatedNodeHealth(
            profile = NetworkProfile.Overloaded,
            nodeIps = testNodeIps,
            random = Random(42),
            coroutineContext = UnconfinedTestDispatcher(testScheduler),
        )

        val events = mutableListOf<NodeHealthEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            health.events.collect { events.add(it) }
        }

        health.start()
        advanceTimeBy(5000)
        health.stop()

        val countAfterFirstStop = events.size

        health.start()
        advanceTimeBy(5000)
        health.stop()
        job.cancel()

        assertTrue(
            events.size > countAfterFirstStop,
            "Should emit new events after restart"
        )
    }
}
