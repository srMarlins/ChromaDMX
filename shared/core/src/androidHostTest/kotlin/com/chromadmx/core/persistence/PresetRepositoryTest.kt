package com.chromadmx.core.persistence

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.chromadmx.core.db.ChromaDmxDatabase
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Genre
import com.chromadmx.core.model.ScenePreset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PresetRepositoryTest {

    private lateinit var db: ChromaDmxDatabase
    private lateinit var repo: PresetRepository

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChromaDmxDatabase.Schema.create(driver)
        db = ChromaDmxDatabase(driver)
        repo = PresetRepository(db)
    }

    private fun createPreset(
        id: String = "p1",
        name: String = "Test Preset",
        genre: Genre? = Genre.TECHNO,
        isBuiltIn: Boolean = false,
        sortOrder: Int = 0,
        createdAt: Long = 1000L
    ) = ScenePreset(
        id = id,
        name = name,
        genre = genre,
        layers = emptyList(),
        masterDimmer = 1.0f,
        isBuiltIn = isBuiltIn,
        sortOrder = sortOrder,
        createdAt = createdAt,
        thumbnailColors = listOf(Color(1f, 0f, 0f))
    )

    @Test
    fun insertAndQueryPreset() = runTest {
        val preset = createPreset(id = "p1", name = "Techno 1", sortOrder = 5)
        repo.save(preset)

        val all = repo.allPresets().first()
        assertEquals(1, all.size)
        assertEquals("p1", all[0].id)
        assertEquals("Techno 1", all[0].name)
        assertEquals(Genre.TECHNO, all[0].genre)
        assertEquals(5, all[0].sortOrder)
    }

    @Test
    fun queryBuiltinPresets() = runTest {
        repo.save(createPreset(id = "user1", isBuiltIn = false))
        repo.save(createPreset(id = "built1", isBuiltIn = true))

        val builtins = repo.builtinPresets().first()
        assertEquals(1, builtins.size)
        assertEquals("built1", builtins[0].id)
        assertTrue(builtins[0].isBuiltIn)
    }

    @Test
    fun updatePreset() = runTest {
        repo.save(createPreset(id = "p1", name = "Old Name"))
        repo.save(createPreset(id = "p1", name = "New Name"))

        val all = repo.allPresets().first()
        assertEquals(1, all.size)
        assertEquals("New Name", all[0].name)
    }

    @Test
    fun deletePreset() = runTest {
        repo.save(createPreset(id = "p1"))
        repo.save(createPreset(id = "p2"))

        repo.delete("p1")

        val all = repo.allPresets().first()
        assertEquals(1, all.size)
        assertEquals("p2", all[0].id)
    }

    @Test
    fun allPresetsOrderedBySortOrderAndCreatedAt() = runTest {
        repo.save(createPreset(id = "p1", sortOrder = 10, createdAt = 100L))
        repo.save(createPreset(id = "p2", sortOrder = 5, createdAt = 300L))
        repo.save(createPreset(id = "p3", sortOrder = 10, createdAt = 200L))

        val all = repo.allPresets().first()
        assertEquals("p2", all[0].id) // lowest sort_order
        assertEquals("p3", all[1].id) // same sort_order, newer createdAt
        assertEquals("p1", all[2].id) // same sort_order, older createdAt
    }

    @Test
    fun flowEmitsOnSave() = runTest {
        val allFlow = repo.allPresets()
        assertEquals(0, allFlow.first().size)

        repo.save(createPreset(id = "p1"))
        assertEquals(1, allFlow.first().size)
    }
}
