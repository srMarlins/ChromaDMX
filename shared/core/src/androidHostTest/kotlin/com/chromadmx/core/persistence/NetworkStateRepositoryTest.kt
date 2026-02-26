package com.chromadmx.core.persistence

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.chromadmx.core.db.ChromaDmxDatabase
import com.chromadmx.core.model.DmxNode
import com.chromadmx.core.model.KnownNode
import com.chromadmx.core.model.toKnownNode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NetworkStateRepositoryTest {

    private lateinit var db: ChromaDmxDatabase
    private lateinit var repo: NetworkStateRepository

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChromaDmxDatabase.Schema.create(driver)
        db = ChromaDmxDatabase(driver)
        repo = NetworkStateRepository(db)
    }

    private fun makeDmxNode(
        ip: String = "192.168.1.100",
        mac: String = "AA:BB:CC:DD:EE:01",
        shortName: String = "Node 1"
    ) = DmxNode(
        ipAddress = ip,
        macAddress = mac,
        shortName = shortName,
        lastSeenMs = 1000L
    )

    private fun makeKnownNode(
        key: String = "AA:BB:CC:DD:EE:01",
        ip: String = "192.168.1.100",
        shortName: String = "Node 1",
        lastSeen: Long = 1000L
    ) = KnownNode(
        nodeKey = key,
        ipAddress = ip,
        shortName = shortName,
        longName = "",
        lastSeenMs = lastSeen
    )

    @Test
    fun saveAndRetrieveKnownNodes() = runTest {
        val nodes = listOf(
            makeKnownNode(key = "node1", ip = "1.1.1.1", lastSeen = 5000L),
            makeKnownNode(key = "node2", ip = "2.2.2.2", lastSeen = 3000L)
        )
        repo.saveKnownNodes(nodes)

        val retrieved = repo.knownNodes().first()
        assertEquals(2, retrieved.size)
        // Ordered by last seen DESC
        assertEquals("node1", retrieved[0].nodeKey)
        assertEquals("node2", retrieved[1].nodeKey)
    }

    @Test
    fun detectNewNodes() = runTest {
        // Start with empty DB
        val currentNodes = listOf(
            makeDmxNode(ip = "1.1.1.1", mac = "MAC1"),
            makeDmxNode(ip = "2.2.2.2", mac = "MAC2")
        )

        val diff = repo.detectTopologyChanges(currentNodes)
        assertEquals(setOf("MAC1", "MAC2"), diff.newNodes.map { it.nodeKey }.toSet())
        assertEquals(0, diff.lostNodes.size)
    }

    @Test
    fun detectLostNodes() = runTest {
        // Save some known nodes
        repo.saveKnownNodes(listOf(
            makeKnownNode(key = "MAC1", ip = "1.1.1.1"),
            makeKnownNode(key = "MAC2", ip = "2.2.2.2")
        ))

        // Current network only has MAC1
        val currentNodes = listOf(
            makeDmxNode(ip = "1.1.1.1", mac = "MAC1")
        )

        val diff = repo.detectTopologyChanges(currentNodes)
        assertEquals(0, diff.newNodes.size)
        assertEquals(1, diff.lostNodes.size)
        assertEquals("MAC2", diff.lostNodes[0].nodeKey)
    }

    @Test
    fun detectMixedChanges() = runTest {
        // Known: MAC1, MAC2
        repo.saveKnownNodes(listOf(
            makeKnownNode(key = "MAC1", ip = "1.1.1.1"),
            makeKnownNode(key = "MAC2", ip = "2.2.2.2")
        ))

        // Current: MAC2, MAC3
        val currentNodes = listOf(
            makeDmxNode(ip = "2.2.2.2", mac = "MAC2"),
            makeDmxNode(ip = "3.3.3.3", mac = "MAC3")
        )

        val diff = repo.detectTopologyChanges(currentNodes)
        assertEquals(1, diff.newNodes.size)
        assertEquals("MAC3", diff.newNodes[0].nodeKey)
        assertEquals(1, diff.lostNodes.size)
        assertEquals("MAC1", diff.lostNodes[0].nodeKey)
    }

    @Test
    fun detectNoChanges() = runTest {
        repo.saveKnownNodes(listOf(
            makeKnownNode(key = "MAC1", ip = "1.1.1.1")
        ))

        val currentNodes = listOf(
            makeDmxNode(ip = "1.1.1.1", mac = "MAC1")
        )

        val diff = repo.detectTopologyChanges(currentNodes)
        assertTrue(diff.newNodes.isEmpty())
        assertTrue(diff.lostNodes.isEmpty())
    }

    @Test
    fun toKnownNodeExtension() {
        val dmxNode = makeDmxNode(ip = "1.2.3.4", mac = "MAC_ADDR", shortName = "Test Node")
        val knownNode = dmxNode.toKnownNode()

        assertEquals(dmxNode.nodeKey, knownNode.nodeKey)
        assertEquals(dmxNode.ipAddress, knownNode.ipAddress)
        assertEquals(dmxNode.shortName, knownNode.shortName)
        assertEquals(dmxNode.lastSeenMs, knownNode.lastSeenMs)
    }
}
