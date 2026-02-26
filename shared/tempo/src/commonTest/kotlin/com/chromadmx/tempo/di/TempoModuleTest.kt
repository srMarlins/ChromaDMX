package com.chromadmx.tempo.di

import com.chromadmx.tempo.clock.BeatClock
import com.chromadmx.tempo.link.AbletonLinkClock
import com.chromadmx.tempo.link.LinkFallbackClock
import com.chromadmx.tempo.link.LinkSessionApi
import com.chromadmx.tempo.tap.TapTempoClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatform.getKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/**
 * Tests that the Koin [tempoModule] provides all expected bindings.
 */
class TempoModuleTest {

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    private fun startTestKoin() {
        startKoin {
            modules(
                module {
                    // Provide a CoroutineScope that the tempo module depends on
                    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
                },
                tempoModule
            )
        }
    }

    @Test
    fun moduleProvidesBeatClock() {
        startTestKoin()
        val beatClock = getKoin().get<BeatClock>()
        assertNotNull(beatClock)
    }

    @Test
    fun beatClockIsLinkFallbackClock() {
        startTestKoin()
        val beatClock = getKoin().get<BeatClock>()
        assertIs<LinkFallbackClock>(beatClock)
    }

    @Test
    fun moduleProvidesTapTempoClock() {
        startTestKoin()
        val tapClock = getKoin().get<TapTempoClock>()
        assertNotNull(tapClock)
    }

    @Test
    fun moduleProvidesAbletonLinkClock() {
        startTestKoin()
        val linkClock = getKoin().get<AbletonLinkClock>()
        assertNotNull(linkClock)
    }

    @Test
    fun moduleProvidesLinkSessionApi() {
        startTestKoin()
        val session = getKoin().get<LinkSessionApi>()
        assertNotNull(session)
    }

    @Test
    fun moduleProvidesLinkFallbackClock() {
        startTestKoin()
        val fallbackClock = getKoin().get<LinkFallbackClock>()
        assertNotNull(fallbackClock)
    }
}
