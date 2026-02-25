package com.chromadmx.ui.components.network

import com.chromadmx.networking.model.DmxNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NodeHealthModelTest {

    // ── NodeHealth derivation ──────────────────────────────────────

    @Test
    fun healthy_when_last_seen_under_5s_ago() {
        val node = makeNode(lastSeenMs = 1000L)
        assertEquals(NodeHealth.HEALTHY, node.toNodeHealth(currentTimeMs = 2000L))
    }

    @Test
    fun healthy_at_exactly_0_elapsed() {
        val node = makeNode(lastSeenMs = 5000L)
        assertEquals(NodeHealth.HEALTHY, node.toNodeHealth(currentTimeMs = 5000L))
    }

    @Test
    fun healthy_at_4999ms_elapsed() {
        val node = makeNode(lastSeenMs = 0L)
        assertEquals(NodeHealth.HEALTHY, node.toNodeHealth(currentTimeMs = 4999L))
    }

    @Test
    fun degraded_at_exactly_5000ms_elapsed() {
        val node = makeNode(lastSeenMs = 0L)
        assertEquals(NodeHealth.DEGRADED, node.toNodeHealth(currentTimeMs = 5000L))
    }

    @Test
    fun degraded_at_14999ms_elapsed() {
        val node = makeNode(lastSeenMs = 0L)
        assertEquals(NodeHealth.DEGRADED, node.toNodeHealth(currentTimeMs = 14999L))
    }

    @Test
    fun lost_at_exactly_15000ms_elapsed() {
        val node = makeNode(lastSeenMs = 0L)
        assertEquals(NodeHealth.LOST, node.toNodeHealth(currentTimeMs = 15000L))
    }

    @Test
    fun lost_at_large_elapsed() {
        val node = makeNode(lastSeenMs = 0L)
        assertEquals(NodeHealth.LOST, node.toNodeHealth(currentTimeMs = 60_000L))
    }

    // ── toNodeStatus mapping ───────────────────────────────────────

    @Test
    fun toNodeStatus_uses_shortName_when_present() {
        val node = makeNode(shortName = "Par LED 1", ipAddress = "10.0.0.5", lastSeenMs = 100L)
        val status = node.toNodeStatus(currentTimeMs = 200L)
        assertEquals("Par LED 1", status.name)
        assertEquals("10.0.0.5", status.ip)
        assertEquals(NodeHealth.HEALTHY, status.health)
    }

    @Test
    fun toNodeStatus_falls_back_to_ip_when_shortName_empty() {
        val node = makeNode(shortName = "", ipAddress = "192.168.1.42", lastSeenMs = 100L)
        val status = node.toNodeStatus(currentTimeMs = 200L)
        assertEquals("192.168.1.42", status.name)
    }

    @Test
    fun toNodeStatus_preserves_universes() {
        val node = makeNode(universes = listOf(0, 1, 2), lastSeenMs = 100L)
        val status = node.toNodeStatus(currentTimeMs = 200L)
        assertEquals(listOf(0, 1, 2), status.universes)
    }

    // ── Compact display logic ──────────────────────────────────────

    @Test
    fun compactOverflowText_null_when_all_fit() {
        assertNull(compactOverflowText(totalNodes = 1))
        assertNull(compactOverflowText(totalNodes = 2))
        assertNull(compactOverflowText(totalNodes = 3))
    }

    @Test
    fun compactOverflowText_plus1_for_4_nodes() {
        assertEquals("+1", compactOverflowText(totalNodes = 4))
    }

    @Test
    fun compactOverflowText_plus7_for_10_nodes() {
        assertEquals("+7", compactOverflowText(totalNodes = 10))
    }

    @Test
    fun compactOverflowText_null_for_zero_nodes() {
        assertNull(compactOverflowText(totalNodes = 0))
    }

    @Test
    fun compactOverflowText_respects_custom_maxVisible() {
        assertNull(compactOverflowText(totalNodes = 5, maxVisible = 5))
        assertEquals("+1", compactOverflowText(totalNodes = 6, maxVisible = 5))
    }

    // ── Helper ─────────────────────────────────────────────────────

    private fun makeNode(
        ipAddress: String = "192.168.1.100",
        shortName: String = "TestNode",
        universes: List<Int> = listOf(0),
        lastSeenMs: Long = 0L,
    ) = DmxNode(
        ipAddress = ipAddress,
        shortName = shortName,
        universes = universes,
        lastSeenMs = lastSeenMs,
    )
}
