package com.chromadmx.ui.viewmodel

import com.chromadmx.core.model.Genre
import com.chromadmx.core.persistence.FileStorage
import com.chromadmx.networking.discovery.NodeDiscovery
import com.chromadmx.networking.model.DmxNode
import com.chromadmx.networking.transport.PlatformUdpTransport
import com.chromadmx.simulation.fixtures.RigPreset
import com.chromadmx.ui.onboarding.OnboardingStep
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    // -- Fakes --

    private val fakeStorage = object : FileStorage {
        private val files = mutableMapOf<String, String>()
        override fun saveFile(path: String, content: String) { files[path] = content }
        override fun readFile(path: String): String? = files[path]
        override fun deleteFile(path: String): Boolean = files.remove(path) != null
        override fun listFiles(directory: String): List<String> =
            files.keys.filter { it.startsWith(directory) }.map { it.substringAfterLast("/") }
        override fun exists(path: String): Boolean = files.containsKey(path)
        override fun mkdirs(directory: String) {}
    }

    private val transport = PlatformUdpTransport()
    private val nodeDiscovery = NodeDiscovery(transport)

    private fun createVm(
        storage: FileStorage = fakeStorage,
        discovery: NodeDiscovery = nodeDiscovery,
        scope: kotlinx.coroutines.CoroutineScope? = null,
    ): OnboardingViewModel {
        val vmScope = scope ?: kotlinx.coroutines.CoroutineScope(
            kotlinx.coroutines.Dispatchers.Default + kotlinx.coroutines.SupervisorJob()
        )
        return OnboardingViewModel(
            scope = vmScope,
            nodeDiscovery = discovery,
            fileStorage = storage,
            presetLibrary = null,
        )
    }

    // -- OnboardingStep Tests --

    @Test
    fun stepsContainAllSixStepsInOrder() {
        val steps = OnboardingStep.steps
        assertEquals(6, steps.size)
        assertIs<OnboardingStep.Splash>(steps[0])
        assertIs<OnboardingStep.NetworkDiscovery>(steps[1])
        assertIs<OnboardingStep.FixtureScan>(steps[2])
        assertIs<OnboardingStep.VibeCheck>(steps[3])
        assertIs<OnboardingStep.StagePreview>(steps[4])
        assertIs<OnboardingStep.Complete>(steps[5])
    }

    // -- Initial State Tests --

    @Test
    fun initialStepIsSplash() = runTest {
        val vm = createVm(scope = backgroundScope)
        assertIs<OnboardingStep.Splash>(vm.currentStep.value)
    }

    @Test
    fun initiallyNoNodesDiscovered() = runTest {
        val vm = createVm(scope = backgroundScope)
        assertTrue(vm.discoveredNodes.value.isEmpty())
    }

    @Test
    fun initiallyNotScanning() = runTest {
        val vm = createVm(scope = backgroundScope)
        assertFalse(vm.isScanning.value)
    }

    @Test
    fun initiallyNotSimulationMode() = runTest {
        val vm = createVm(scope = backgroundScope)
        assertFalse(vm.isSimulationMode.value)
    }

    @Test
    fun initiallyNoGenreSelected() = runTest {
        val vm = createVm(scope = backgroundScope)
        assertNull(vm.selectedGenre.value)
    }

    @Test
    fun initialRigPresetIsSmallDj() = runTest {
        val vm = createVm(scope = backgroundScope)
        assertEquals(RigPreset.SMALL_DJ, vm.selectedRigPreset.value)
    }

    // -- First Launch Detection --

    @Test
    fun isFirstLaunchReturnsTrueWhenNoPrefsSaved() {
        val vm = createVm()
        assertTrue(vm.isFirstLaunch())
    }

    @Test
    fun isFirstLaunchReturnsFalseAfterOnboardingComplete() = runTest {
        val vm = createVm(scope = backgroundScope)
        // Manually write the pref to simulate completion
        fakeStorage.mkdirs("prefs")
        fakeStorage.saveFile(OnboardingViewModel.PREFS_PATH, "onboarding_complete=true")
        assertFalse(vm.isFirstLaunch())
    }

    // -- Step Advancement --

    @Test
    fun startSetsSplashStep() = runTest {
        val vm = createVm(scope = backgroundScope)
        vm.start()
        assertIs<OnboardingStep.Splash>(vm.currentStep.value)
    }

    @Test
    fun advanceFromSplashGoesToNetworkDiscovery() = runTest {
        val vm = createVm(scope = backgroundScope)
        vm.start()
        vm.advance()
        assertIs<OnboardingStep.NetworkDiscovery>(vm.currentStep.value)
    }

    @Test
    fun advanceFromNetworkDiscoveryGoesToFixtureScan() = runTest {
        val vm = createVm(scope = backgroundScope)
        vm.start()
        vm.advance() // -> NetworkDiscovery
        vm.advance() // -> FixtureScan
        assertIs<OnboardingStep.FixtureScan>(vm.currentStep.value)
    }

    @Test
    fun advanceFromFixtureScanGoesToVibeCheck() = runTest {
        val vm = createVm(scope = backgroundScope)
        vm.start()
        vm.advance() // -> NetworkDiscovery
        vm.advance() // -> FixtureScan
        vm.advance() // -> VibeCheck
        assertIs<OnboardingStep.VibeCheck>(vm.currentStep.value)
    }

    @Test
    fun advanceFromVibeCheckGoesToStagePreview() = runTest {
        val vm = createVm(scope = backgroundScope)
        vm.start()
        vm.advance() // -> NetworkDiscovery
        vm.advance() // -> FixtureScan
        vm.advance() // -> VibeCheck
        vm.advance() // -> StagePreview
        assertIs<OnboardingStep.StagePreview>(vm.currentStep.value)
    }

    @Test
    fun advanceFromStagePreviewGoesToComplete() = runTest {
        val vm = createVm(scope = backgroundScope)
        vm.start()
        vm.advance() // -> NetworkDiscovery
        vm.advance() // -> FixtureScan
        vm.advance() // -> VibeCheck
        vm.advance() // -> StagePreview
        vm.advance() // -> Complete
        assertIs<OnboardingStep.Complete>(vm.currentStep.value)
    }

    @Test
    fun advanceBeyondCompleteDoesNothing() = runTest {
        val vm = createVm(scope = backgroundScope)
        vm.start()
        repeat(6) { vm.advance() } // Go all the way to Complete
        assertIs<OnboardingStep.Complete>(vm.currentStep.value)
        vm.advance() // Should be no-op
        assertIs<OnboardingStep.Complete>(vm.currentStep.value)
    }

    // -- Splash Auto-Advance --

    @Test
    fun splashAutoAdvancesAfterDelay() = runTest {
        val vm = createVm(scope = backgroundScope)
        vm.start()
        assertIs<OnboardingStep.Splash>(vm.currentStep.value)

        advanceTimeBy(OnboardingViewModel.SPLASH_DURATION_MS + 100)

        assertIs<OnboardingStep.NetworkDiscovery>(vm.currentStep.value)
    }

    // -- Skip to Complete --

    @Test
    fun skipToCompleteFromAnyStep() = runTest {
        val vm = createVm(scope = backgroundScope)
        vm.start()
        vm.advance() // -> NetworkDiscovery
        vm.skipToComplete()
        assertIs<OnboardingStep.Complete>(vm.currentStep.value)
    }

    @Test
    fun skipToCompleteMarksOnboardingDone() = runTest {
        val vm = createVm(scope = backgroundScope)
        vm.start()
        vm.skipToComplete()
        assertFalse(vm.isFirstLaunch())
    }

    // -- Simulation Mode --

    @Test
    fun enterSimulationModeActivatesFlag() = runTest {
        val vm = createVm(scope = backgroundScope)
        vm.start()
        vm.advance() // -> NetworkDiscovery
        vm.enterSimulationMode()
        assertTrue(vm.isSimulationMode.value)
    }

    @Test
    fun enterSimulationModeAdvancesToFixtureScan() = runTest {
        val vm = createVm(scope = backgroundScope)
        vm.start()
        vm.advance() // -> NetworkDiscovery
        vm.enterSimulationMode()
        assertIs<OnboardingStep.FixtureScan>(vm.currentStep.value)
    }

    @Test
    fun enterSimulationModeStopsScanning() = runTest {
        val vm = createVm(scope = backgroundScope)
        vm.start()
        vm.advance() // -> NetworkDiscovery
        vm.enterSimulationMode()
        assertFalse(vm.isScanning.value)
    }

    // -- Rig Preset Selection --

    @Test
    fun selectRigPresetUpdatesState() = runTest {
        val vm = createVm(scope = backgroundScope)
        vm.selectRigPreset(RigPreset.TRUSS_RIG)
        assertEquals(RigPreset.TRUSS_RIG, vm.selectedRigPreset.value)
    }

    @Test
    fun selectRigPresetUpdatesFixtureCount() = runTest {
        val vm = createVm(scope = backgroundScope)
        vm.selectRigPreset(RigPreset.SMALL_DJ)
        assertTrue(vm.simulationFixtureCount.value > 0)
    }

    @Test
    fun selectFestivalStagePreset() = runTest {
        val vm = createVm(scope = backgroundScope)
        vm.selectRigPreset(RigPreset.FESTIVAL_STAGE)
        assertEquals(RigPreset.FESTIVAL_STAGE, vm.selectedRigPreset.value)
        assertTrue(vm.simulationFixtureCount.value > 0)
    }

    // -- Genre Selection --

    @Test
    fun genreListContainsEightGenres() {
        val vm = createVm()
        assertEquals(8, vm.genres.size)
    }

    @Test
    fun genreListMatchesCoreGenreEnum() {
        val vm = createVm()
        val coreGenres = Genre.entries.toSet()
        val vmGenres = vm.genres.mapNotNull { it.genre }.toSet()
        assertEquals(coreGenres, vmGenres)
    }

    @Test
    fun genreIdsAreUnique() {
        val vm = createVm()
        val ids = vm.genres.map { it.id }
        assertEquals(ids.distinct().size, ids.size)
    }

    @Test
    fun selectGenreUpdatesState() = runTest {
        val vm = createVm(scope = backgroundScope)
        val techno = vm.genres.first { it.id == "techno" }
        vm.selectGenre(techno)
        assertEquals(techno, vm.selectedGenre.value)
    }

    @Test
    fun confirmGenreAdvancesToStagePreview() = runTest {
        val vm = createVm(scope = backgroundScope)
        vm.start()
        vm.advance() // -> NetworkDiscovery
        vm.advance() // -> FixtureScan
        vm.advance() // -> VibeCheck
        vm.selectGenre(vm.genres.first())
        vm.confirmGenre()
        assertIs<OnboardingStep.StagePreview>(vm.currentStep.value)
    }

    // -- Stage Preview --

    @Test
    fun stagePreviewMarksOnboardingComplete() = runTest {
        val vm = createVm(scope = backgroundScope)
        vm.start()
        vm.advance() // -> NetworkDiscovery
        vm.advance() // -> FixtureScan
        vm.advance() // -> VibeCheck
        vm.advance() // -> StagePreview
        assertIs<OnboardingStep.StagePreview>(vm.currentStep.value)
        // Should have persisted completion
        assertFalse(vm.isFirstLaunch())
    }

    @Test
    fun stagePreviewAutoAdvancesToComplete() = runTest {
        val vm = createVm(scope = backgroundScope)
        vm.start()
        vm.advance() // -> NetworkDiscovery
        vm.advance() // -> FixtureScan
        vm.advance() // -> VibeCheck
        vm.advance() // -> StagePreview

        advanceTimeBy(OnboardingViewModel.STAGE_PREVIEW_DURATION_MS + 100)

        assertIs<OnboardingStep.Complete>(vm.currentStep.value)
    }

    @Test
    fun skipStagePreviewAdvancesToComplete() = runTest {
        val vm = createVm(scope = backgroundScope)
        vm.start()
        vm.advance() // -> NetworkDiscovery
        vm.advance() // -> FixtureScan
        vm.advance() // -> VibeCheck
        vm.advance() // -> StagePreview
        vm.skipStagePreview()
        assertIs<OnboardingStep.Complete>(vm.currentStep.value)
    }

    // -- Persistent Storage --

    @Test
    fun saveAndRetrieveLastKnownNodes() {
        val vm = createVm()
        // Simulate saving through internal mechanism by writing directly
        fakeStorage.mkdirs("prefs")
        fakeStorage.saveFile(OnboardingViewModel.NODES_PATH, "192.168.1.1,192.168.1.2")
        val nodes = vm.getLastKnownNodes()
        assertEquals(listOf("192.168.1.1", "192.168.1.2"), nodes)
    }

    @Test
    fun getLastKnownNodesReturnsEmptyWhenNoData() {
        val vm = createVm()
        assertTrue(vm.getLastKnownNodes().isEmpty())
    }

    @Test
    fun saveAndRetrieveRigPreset() {
        val vm = createVm()
        // Simulate saving through internal mechanism
        fakeStorage.mkdirs("prefs")
        fakeStorage.saveFile(OnboardingViewModel.RIG_PRESET_PATH, RigPreset.FESTIVAL_STAGE.name)
        val preset = vm.getSavedRigPreset()
        assertEquals(RigPreset.FESTIVAL_STAGE, preset)
    }

    @Test
    fun getSavedRigPresetReturnsNullWhenNoData() {
        val vm = createVm()
        assertNull(vm.getSavedRigPreset())
    }

    // -- Network Changed Detection --

    @Test
    fun networkChangedDefaultsToFalse() = runTest {
        val vm = createVm(scope = backgroundScope)
        assertFalse(vm.networkChanged.value)
    }

    @Test
    fun repeatLaunchCompleteDefaultsToFalse() = runTest {
        val vm = createVm(scope = backgroundScope)
        assertFalse(vm.repeatLaunchComplete.value)
    }

    // -- Cleanup --

    @Test
    fun onClearedDoesNotThrow() = runTest {
        val vm = createVm(scope = backgroundScope)
        vm.start()
        vm.onCleared()
        // Should not throw
    }

    // -- Full Flow Integration --

    @Test
    fun fullFlowFromSplashToComplete() = runTest {
        val vm = createVm(scope = backgroundScope)
        assertTrue(vm.isFirstLaunch())

        // Start onboarding
        vm.start()
        assertIs<OnboardingStep.Splash>(vm.currentStep.value)

        // Advance through each step
        vm.advance() // -> NetworkDiscovery
        assertIs<OnboardingStep.NetworkDiscovery>(vm.currentStep.value)

        // Enter simulation mode
        vm.enterSimulationMode()
        assertTrue(vm.isSimulationMode.value)
        assertIs<OnboardingStep.FixtureScan>(vm.currentStep.value)

        // Select rig preset
        vm.selectRigPreset(RigPreset.TRUSS_RIG)
        assertEquals(RigPreset.TRUSS_RIG, vm.selectedRigPreset.value)

        // Continue to VibeCheck
        vm.advance()
        assertIs<OnboardingStep.VibeCheck>(vm.currentStep.value)

        // Select genre
        val house = vm.genres.first { it.id == "house" }
        vm.selectGenre(house)
        assertEquals(house, vm.selectedGenre.value)

        // Confirm genre -> StagePreview
        vm.confirmGenre()
        assertIs<OnboardingStep.StagePreview>(vm.currentStep.value)

        // Skip stage preview -> Complete
        vm.skipStagePreview()
        assertIs<OnboardingStep.Complete>(vm.currentStep.value)

        // Onboarding should be marked complete
        assertFalse(vm.isFirstLaunch())

        // Cleanup
        vm.onCleared()
    }

    @Test
    fun retryNetworkScanResetsScanning() = runTest {
        val vm = createVm(scope = backgroundScope)
        vm.start()
        vm.advance() // -> NetworkDiscovery
        // retryNetworkScan should reset and start scanning
        vm.retryNetworkScan()
        assertTrue(vm.isScanning.value)
    }

    @Test
    fun fixturesLoadedStartsAtZero() = runTest {
        val vm = createVm(scope = backgroundScope)
        assertEquals(0, vm.fixturesLoaded.value)
    }

    @Test
    fun matchingPresetCountDefaultsToZero() = runTest {
        val vm = createVm(scope = backgroundScope)
        assertEquals(0, vm.matchingPresetCount.value)
    }

    // -- GenreOption Tests --

    @Test
    fun genreOptionHasRequiredFields() {
        val option = GenreOption("test", "Test Genre", 0xFFFF0000, Genre.TECHNO)
        assertEquals("test", option.id)
        assertEquals("Test Genre", option.displayName)
        assertEquals(0xFFFF0000, option.color)
        assertEquals(Genre.TECHNO, option.genre)
    }

    @Test
    fun genreOptionGenreDefaultsToNull() {
        val option = GenreOption("test", "Test", 0xFF000000)
        assertNull(option.genre)
    }
}
