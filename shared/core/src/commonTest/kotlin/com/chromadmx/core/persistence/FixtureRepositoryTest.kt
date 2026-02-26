package com.chromadmx.core.persistence

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.chromadmx.core.db.ChromaDmxDatabase
import com.chromadmx.core.model.Fixture
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FixtureRepositoryTest {

    private lateinit var db: ChromaDmxDatabase
    private lateinit var repo: FixtureRepository

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChromaDmxDatabase.Schema.create(driver)
        db = ChromaDmxDatabase(driver)
        repo = FixtureRepository(db)
    }

    private fun makeFixture(
        id: String = "f1",
        name: String = "Par 1",
        channelStart: Int = 1,
        channelCount: Int = 3,
        universeId: Int = 0,
        profileId: String = "generic-rgb-par",
        position: Vec3 = Vec3.ZERO,
        groupId: String? = null,
    ) = Fixture3D(
        fixture = Fixture(
            fixtureId = id,
            name = name,
            channelStart = channelStart,
            channelCount = channelCount,
            universeId = universeId,
            profileId = profileId,
        ),
        position = position,
        groupId = groupId,
    )

    // ---- Fixture CRUD ----

    @Test
    fun insertAndQueryFixture() = runTest {
        val fixture = makeFixture(id = "f1", name = "Par 1", position = Vec3(1f, 2f, 3f))
        repo.saveFixture(fixture)

        val all = repo.allFixtures().first()
        assertEquals(1, all.size)
        assertEquals("f1", all[0].fixture.fixtureId)
        assertEquals("Par 1", all[0].fixture.name)
        assertEquals(Vec3(1f, 2f, 3f), all[0].position)
    }

    @Test
    fun insertMultipleFixtures() = runTest {
        val f1 = makeFixture(id = "f1", name = "Alpha")
        val f2 = makeFixture(id = "f2", name = "Bravo")
        val f3 = makeFixture(id = "f3", name = "Charlie")
        repo.saveAll(listOf(f1, f2, f3))

        val all = repo.allFixtures().first()
        assertEquals(3, all.size)
        // Ordered by name
        assertEquals("Alpha", all[0].fixture.name)
        assertEquals("Bravo", all[1].fixture.name)
        assertEquals("Charlie", all[2].fixture.name)
    }

    @Test
    fun updatePosition() = runTest {
        repo.saveFixture(makeFixture(id = "f1", position = Vec3(0f, 0f, 0f)))
        repo.updatePosition("f1", Vec3(5f, 10f, 3f))

        val all = repo.allFixtures().first()
        assertEquals(1, all.size)
        assertEquals(Vec3(5f, 10f, 3f), all[0].position)
    }

    @Test
    fun updateGroup() = runTest {
        repo.saveFixture(makeFixture(id = "f1"))
        assertNull(repo.allFixtures().first()[0].groupId)

        repo.updateGroup("f1", "grp-truss")
        assertEquals("grp-truss", repo.allFixtures().first()[0].groupId)

        // Unassign
        repo.updateGroup("f1", null)
        assertNull(repo.allFixtures().first()[0].groupId)
    }

    @Test
    fun deleteFixture() = runTest {
        repo.saveAll(listOf(
            makeFixture(id = "f1", name = "A"),
            makeFixture(id = "f2", name = "B"),
        ))
        assertEquals(2, repo.allFixtures().first().size)

        repo.deleteFixture("f1")
        val remaining = repo.allFixtures().first()
        assertEquals(1, remaining.size)
        assertEquals("f2", remaining[0].fixture.fixtureId)
    }

    @Test
    fun deleteAllFixtures() = runTest {
        repo.saveAll(listOf(
            makeFixture(id = "f1", name = "A"),
            makeFixture(id = "f2", name = "B"),
        ))
        repo.deleteAll()
        assertTrue(repo.allFixtures().first().isEmpty())
    }

    @Test
    fun insertOrReplaceUpdatesExisting() = runTest {
        repo.saveFixture(makeFixture(id = "f1", name = "Original", position = Vec3(1f, 1f, 1f)))
        repo.saveFixture(makeFixture(id = "f1", name = "Updated", position = Vec3(9f, 9f, 9f)))

        val all = repo.allFixtures().first()
        assertEquals(1, all.size)
        assertEquals("Updated", all[0].fixture.name)
        assertEquals(Vec3(9f, 9f, 9f), all[0].position)
    }

    @Test
    fun fixturePreservesAllFields() = runTest {
        val fixture = makeFixture(
            id = "test-id",
            name = "Moving Head 1",
            channelStart = 100,
            channelCount = 16,
            universeId = 2,
            profileId = "moving-head-spot",
            position = Vec3(3.5f, 7.0f, 4.5f),
            groupId = "grp-truss-left",
        )
        repo.saveFixture(fixture)

        val loaded = repo.allFixtures().first()[0]
        assertEquals("test-id", loaded.fixture.fixtureId)
        assertEquals("Moving Head 1", loaded.fixture.name)
        assertEquals(100, loaded.fixture.channelStart)
        assertEquals(16, loaded.fixture.channelCount)
        assertEquals(2, loaded.fixture.universeId)
        assertEquals("moving-head-spot", loaded.fixture.profileId)
        assertEquals(Vec3(3.5f, 7.0f, 4.5f), loaded.position)
        assertEquals("grp-truss-left", loaded.groupId)
    }

    // ---- Group CRUD ----

    @Test
    fun insertAndQueryGroup() = runTest {
        val group = FixtureGroup(groupId = "g1", name = "Truss Left", color = 0xFF00FBFF)
        repo.saveGroup(group)

        val groups = repo.allGroups().first()
        assertEquals(1, groups.size)
        assertEquals("g1", groups[0].groupId)
        assertEquals("Truss Left", groups[0].name)
        assertEquals(0xFF00FBFF, groups[0].color)
    }

    @Test
    fun deleteGroup() = runTest {
        repo.saveGroup(FixtureGroup("g1", "Group A", 0))
        repo.saveGroup(FixtureGroup("g2", "Group B", 0))
        assertEquals(2, repo.allGroups().first().size)

        repo.deleteGroup("g1")
        val remaining = repo.allGroups().first()
        assertEquals(1, remaining.size)
        assertEquals("g2", remaining[0].groupId)
    }

    @Test
    fun groupsOrderedByName() = runTest {
        repo.saveGroup(FixtureGroup("g3", "Zebra", 0))
        repo.saveGroup(FixtureGroup("g1", "Alpha", 0))
        repo.saveGroup(FixtureGroup("g2", "Mid", 0))

        val names = repo.allGroups().first().map { it.name }
        assertEquals(listOf("Alpha", "Mid", "Zebra"), names)
    }

    @Test
    fun insertOrReplaceGroupUpdatesExisting() = runTest {
        repo.saveGroup(FixtureGroup("g1", "Original", 0))
        repo.saveGroup(FixtureGroup("g1", "Renamed", 42))

        val groups = repo.allGroups().first()
        assertEquals(1, groups.size)
        assertEquals("Renamed", groups[0].name)
        assertEquals(42L, groups[0].color)
    }

    // ---- Flow reactivity ----

    @Test
    fun flowEmitsOnFixtureInsert() = runTest {
        // First emission: empty
        assertEquals(0, repo.allFixtures().first().size)

        // Insert and verify
        repo.saveFixture(makeFixture(id = "f1"))
        assertEquals(1, repo.allFixtures().first().size)

        repo.saveFixture(makeFixture(id = "f2", name = "Zeta"))
        assertEquals(2, repo.allFixtures().first().size)
    }

    @Test
    fun flowEmitsOnGroupInsert() = runTest {
        assertEquals(0, repo.allGroups().first().size)
        repo.saveGroup(FixtureGroup("g1", "Test", 0))
        assertEquals(1, repo.allGroups().first().size)
    }
}
