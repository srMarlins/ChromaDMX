package com.chromadmx.service

import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Fixture
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Genre
import com.chromadmx.core.model.ScenePreset
import com.chromadmx.core.model.Vec3
import com.chromadmx.core.persistence.FileStorage
import com.chromadmx.core.persistence.FixtureStore
import com.chromadmx.core.persistence.SettingsStore
import com.chromadmx.engine.effect.EffectRegistry
import com.chromadmx.engine.effect.EffectStack
import com.chromadmx.engine.preset.PresetLibrary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

// -- Fakes --

private class FakeFileStorage : FileStorage {
    private val files = mutableMapOf<String, String>()
    override fun saveFile(path: String, content: String) { files[path] = content }
    override fun readFile(path: String): String? = files[path]
    override fun deleteFile(path: String): Boolean = files.remove(path) != null
    override fun listFiles(directory: String): List<String> =
        files.keys.filter { it.startsWith(directory) }.map { it.substringAfterLast("/") }
    override fun exists(path: String): Boolean = files.containsKey(path)
    override fun mkdirs(directory: String) {}
}

private class FakeFixtureStore : FixtureStore {
    private val _fixtures = MutableStateFlow<List<Fixture3D>>(emptyList())
    var deleteAllCount = 0; private set

    override fun allFixtures(): Flow<List<Fixture3D>> = _fixtures
    override fun saveFixture(fixture: Fixture3D) {
        _fixtures.value = _fixtures.value + fixture
    }
    override fun saveAll(fixtures: List<Fixture3D>) {
        _fixtures.value = _fixtures.value + fixtures
    }
    override fun deleteFixture(fixtureId: String) {
        _fixtures.value = _fixtures.value.filter { it.fixture.fixtureId != fixtureId }
    }
    override fun deleteAll() {
        deleteAllCount++
        _fixtures.value = emptyList()
    }

    fun setFixtures(fixtures: List<Fixture3D>) {
        _fixtures.value = fixtures
    }
}

private class FakeSettingsStore : SettingsStore {
    private val _masterDimmer = MutableStateFlow(1.0f)
    private val _themePreference = MutableStateFlow("MatchaDark")
    private val _isSimulation = MutableStateFlow(false)
    private val _transportMode = MutableStateFlow("Real")
    private val _activePresetId = MutableStateFlow<String?>(null)
    private val _setupCompleted = MutableStateFlow(false)

    override val masterDimmer: Flow<Float> = _masterDimmer
    override val themePreference: Flow<String> = _themePreference
    override val isSimulation: Flow<Boolean> = _isSimulation
    override val transportMode: Flow<String> = _transportMode
    override val activePresetId: Flow<String?> = _activePresetId
    override val setupCompleted: Flow<Boolean> = _setupCompleted

    override suspend fun setMasterDimmer(value: Float) { _masterDimmer.value = value }
    override suspend fun setThemePreference(value: String) { _themePreference.value = value }
    override suspend fun setIsSimulation(value: Boolean) { _isSimulation.value = value }
    override suspend fun setTransportMode(value: String) { _transportMode.value = value }
    override suspend fun setActivePresetId(value: String?) { _activePresetId.value = value }
    override suspend fun setSetupCompleted(value: Boolean) { _setupCompleted.value = value }
}

class DataExportServiceTest {

    private val testJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun createService(
        fixtureStore: FakeFixtureStore = FakeFixtureStore(),
        settingsStore: FakeSettingsStore = FakeSettingsStore(),
    ): Triple<DataExportService, FakeFixtureStore, FakeSettingsStore> {
        val storage = FakeFileStorage()
        val registry = EffectRegistry()
        val stack = EffectStack()
        val presetLibrary = PresetLibrary(storage, registry, stack)
        val service = DataExportService(fixtureStore, presetLibrary, settingsStore)
        return Triple(service, fixtureStore, settingsStore)
    }

    private fun testFixture(id: String = "fix-1", name: String = "Par 1"): Fixture3D {
        return Fixture3D(
            fixture = Fixture(
                fixtureId = id,
                name = name,
                channelStart = 1,
                channelCount = 3,
                universeId = 0,
                profileId = "generic-rgb-par",
            ),
            position = Vec3(1f, 2f, 3f),
        )
    }

    private fun testPreset(id: String = "preset-1", name: String = "Test Preset"): ScenePreset {
        return ScenePreset(
            id = id,
            name = name,
            genre = Genre.TECHNO,
            layers = emptyList(),
            masterDimmer = 0.8f,
            createdAt = 1234567890L,
            thumbnailColors = listOf(Color.RED, Color.BLUE),
        )
    }

    // -- Export tests --

    @Test
    fun exportProducesValidJson() = runTest {
        val (service, fixtureStore, _) = createService()
        fixtureStore.setFixtures(listOf(testFixture()))

        val json = service.export()
        assertTrue(json.isNotBlank())
        // Should be parseable as ExportBundle
        val bundle = testJson.decodeFromString<ExportBundle>(json)
        assertEquals(1, bundle.version)
        assertEquals(1, bundle.fixtures.size)
        assertEquals("fix-1", bundle.fixtures[0].fixture.fixtureId)
    }

    @Test
    fun exportIncludesSettings() = runTest {
        val settingsStore = FakeSettingsStore()
        settingsStore.setMasterDimmer(0.75f)
        settingsStore.setThemePreference("AzukiDark")
        settingsStore.setIsSimulation(true)

        val (service, _, _) = createService(settingsStore = settingsStore)
        val json = service.export()
        val bundle = testJson.decodeFromString<ExportBundle>(json)

        assertEquals("0.75", bundle.settings["masterDimmer"])
        assertEquals("AzukiDark", bundle.settings["themePreference"])
        assertEquals("true", bundle.settings["isSimulation"])
    }

    @Test
    fun exportIncludesPresets() = runTest {
        val (service, _, _) = createService()
        // PresetLibrary auto-installs built-in presets, so we should see at least those
        val json = service.export()
        val bundle = testJson.decodeFromString<ExportBundle>(json)
        assertTrue(bundle.presets.isNotEmpty(), "Should have at least built-in presets")
    }

    @Test
    fun exportWithEmptyDataProducesValidBundle() = runTest {
        val (service, _, _) = createService()
        val json = service.export()
        val bundle = testJson.decodeFromString<ExportBundle>(json)
        assertEquals(1, bundle.version)
        assertTrue(bundle.fixtures.isEmpty())
    }

    // -- Import tests --

    @Test
    fun importValidJsonSucceeds() = runTest {
        val (service, fixtureStore, settingsStore) = createService()
        val bundle = ExportBundle(
            fixtures = listOf(testFixture()),
            presets = listOf(testPreset()),
            settings = mapOf(
                "masterDimmer" to "0.5",
                "themePreference" to "AzukiDark",
            ),
        )
        val jsonStr = testJson.encodeToString(ExportBundle.serializer(), bundle)

        val result = service.import(jsonStr)
        assertIs<ImportResult.Success>(result)

        // Verify fixtures were imported
        val fixtures = fixtureStore.allFixtures().first()
        assertEquals(1, fixtures.size)
        assertEquals("fix-1", fixtures[0].fixture.fixtureId)

        // Verify settings were applied
        assertEquals(0.5f, settingsStore.masterDimmer.first())
        assertEquals("AzukiDark", settingsStore.themePreference.first())
    }

    @Test
    fun importMalformedJsonReturnsError() = runTest {
        val (service, _, _) = createService()
        val result = service.import("this is not json {{{")
        assertIs<ImportResult.Error>(result)
        assertTrue(result.message.contains("Invalid JSON format"))
    }

    @Test
    fun importEmptyJsonReturnsError() = runTest {
        val (service, _, _) = createService()
        val result = service.import("")
        assertIs<ImportResult.Error>(result)
    }

    @Test
    fun importFutureVersionReturnsError() = runTest {
        val (service, _, _) = createService()
        val bundle = ExportBundle(version = 999)
        val jsonStr = testJson.encodeToString(ExportBundle.serializer(), bundle)

        val result = service.import(jsonStr)
        assertIs<ImportResult.Error>(result)
        assertTrue(result.message.contains("Unsupported export version"))
    }

    @Test
    fun importClearsExistingFixturesBeforeApplying() = runTest {
        val (service, fixtureStore, _) = createService()
        fixtureStore.setFixtures(listOf(
            testFixture("old-1", "Old Par"),
            testFixture("old-2", "Old Wash"),
        ))

        val bundle = ExportBundle(
            fixtures = listOf(testFixture("new-1", "New Par")),
        )
        val jsonStr = testJson.encodeToString(ExportBundle.serializer(), bundle)

        val result = service.import(jsonStr)
        assertIs<ImportResult.Success>(result)

        val fixtures = fixtureStore.allFixtures().first()
        assertEquals(1, fixtures.size)
        assertEquals("new-1", fixtures[0].fixture.fixtureId)
    }

    // -- Round-trip tests --

    @Test
    fun exportThenImportRoundTrip() = runTest {
        // Set up source data
        val sourceFixtureStore = FakeFixtureStore()
        val sourceSettingsStore = FakeSettingsStore()
        sourceFixtureStore.setFixtures(listOf(
            testFixture("f1", "Par Left"),
            testFixture("f2", "Par Right"),
        ))
        sourceSettingsStore.setMasterDimmer(0.6f)
        sourceSettingsStore.setThemePreference("AzukiDark")
        sourceSettingsStore.setIsSimulation(true)
        sourceSettingsStore.setActivePresetId("preset-abc")

        val (exportService, _, _) = createService(
            fixtureStore = sourceFixtureStore,
            settingsStore = sourceSettingsStore,
        )
        val exported = exportService.export()

        // Import into fresh stores
        val targetFixtureStore = FakeFixtureStore()
        val targetSettingsStore = FakeSettingsStore()
        val (importService, _, _) = createService(
            fixtureStore = targetFixtureStore,
            settingsStore = targetSettingsStore,
        )

        val result = importService.import(exported)
        assertIs<ImportResult.Success>(result)

        // Verify fixtures
        val fixtures = targetFixtureStore.allFixtures().first()
        assertEquals(2, fixtures.size)
        val fixtureIds = fixtures.map { it.fixture.fixtureId }.toSet()
        assertTrue("f1" in fixtureIds)
        assertTrue("f2" in fixtureIds)

        // Verify settings
        assertEquals(0.6f, targetSettingsStore.masterDimmer.first())
        assertEquals("AzukiDark", targetSettingsStore.themePreference.first())
        assertEquals(true, targetSettingsStore.isSimulation.first())
        assertEquals("preset-abc", targetSettingsStore.activePresetId.first())
    }

    @Test
    fun importWithMissingSettingsKeysDoesNotOverwrite() = runTest {
        val (service, _, settingsStore) = createService()
        settingsStore.setMasterDimmer(0.9f)
        settingsStore.setThemePreference("MatchaDark")

        // Import with only one setting key
        val bundle = ExportBundle(
            settings = mapOf("themePreference" to "AzukiDark"),
        )
        val jsonStr = testJson.encodeToString(ExportBundle.serializer(), bundle)

        val result = service.import(jsonStr)
        assertIs<ImportResult.Success>(result)

        // masterDimmer should remain unchanged since it was not in the import
        assertEquals(0.9f, settingsStore.masterDimmer.first())
        // themePreference should be updated
        assertEquals("AzukiDark", settingsStore.themePreference.first())
    }
}
