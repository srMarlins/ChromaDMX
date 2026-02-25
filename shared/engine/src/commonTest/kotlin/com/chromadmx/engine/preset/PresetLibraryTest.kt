package com.chromadmx.engine.preset

import com.chromadmx.core.model.*
import com.chromadmx.core.persistence.FileStorage
import com.chromadmx.engine.effect.EffectRegistry
import com.chromadmx.engine.effect.EffectStack
import kotlin.test.*

class FakeFileStorage : FileStorage {
    private val files = mutableMapOf<String, String>()
    override fun saveFile(path: String, content: String) { files[path] = content }
    override fun readFile(path: String): String? = files[path]
    override fun deleteFile(path: String): Boolean = files.remove(path) != null
    override fun listFiles(directory: String): List<String> =
        files.keys.filter { it.startsWith(directory) }.map { it.substringAfterLast("/") }
    override fun exists(path: String): Boolean = files.containsKey(path)
    override fun mkdirs(directory: String) {}
}

class PresetLibraryTest {
    private lateinit var storage: FakeFileStorage
    private lateinit var registry: EffectRegistry
    private lateinit var stack: EffectStack
    private lateinit var library: PresetLibrary

    @BeforeTest
    fun setup() {
        storage = FakeFileStorage()
        registry = EffectRegistry()
        stack = EffectStack()
        library = PresetLibrary(storage, registry, stack)
    }

    @Test
    fun testSaveAndGetPreset() {
        val preset = ScenePreset(
            id = "test1",
            name = "Test Preset",
            genre = Genre.TECHNO,
            layers = emptyList(),
            masterDimmer = 1.0f,
            createdAt = 123456789L,
            thumbnailColors = emptyList()
        )
        library.savePreset(preset)
        val loaded = library.getPreset("test1")
        assertNotNull(loaded)
        assertEquals("Test Preset", loaded.name)
        assertEquals(Genre.TECHNO, loaded.genre)
    }

    @Test
    fun testDeletePreset() {
        val preset = ScenePreset(
            id = "test1",
            name = "Test Preset",
            genre = Genre.TECHNO,
            layers = emptyList(),
            masterDimmer = 1.0f,
            createdAt = 123456789L,
            thumbnailColors = emptyList(),
            isBuiltIn = false
        )
        library.savePreset(preset)
        assertTrue(library.deletePreset("test1"))
        assertNull(library.getPreset("test1"))
    }

    @Test
    fun testProtectBuiltIn() {
        val preset = ScenePreset(
            id = "builtin1",
            name = "Built-in",
            genre = Genre.TECHNO,
            layers = emptyList(),
            masterDimmer = 1.0f,
            createdAt = 123456789L,
            thumbnailColors = emptyList(),
            isBuiltIn = true
        )
        library.savePreset(preset)
        assertFalse(library.deletePreset("builtin1"))
        assertNotNull(library.getPreset("builtin1"))
    }

    @Test
    fun testListAndFilter() {
        library.savePreset(createPreset("p1", Genre.TECHNO))
        library.savePreset(createPreset("p2", Genre.HOUSE))
        library.savePreset(createPreset("p3", Genre.TECHNO))

        assertEquals(3, library.listPresets().size)
        assertEquals(2, library.listPresets(Genre.TECHNO).size)
        assertEquals(1, library.listPresets(Genre.HOUSE).size)
    }

    @Test
    fun testFavorites() {
        val ids = listOf("p1", "p3")
        library.setFavorites(ids)
        assertEquals(ids, library.getFavorites())
    }

    private fun createPreset(id: String, genre: Genre) = ScenePreset(
        id = id,
        name = "Preset $id",
        genre = genre,
        layers = emptyList(),
        masterDimmer = 1.0f,
        createdAt = 0L,
        thumbnailColors = emptyList()
    )
}
