package com.chromadmx.tempo.di

import com.chromadmx.tempo.clock.BeatClock
import com.chromadmx.tempo.link.AbletonLinkClock
import com.chromadmx.tempo.link.LinkFallbackClock
import com.chromadmx.tempo.link.LinkSession
import com.chromadmx.tempo.link.LinkSessionApi
import com.chromadmx.tempo.tap.TapTempoClock
import kotlinx.coroutines.CoroutineScope
import org.koin.dsl.module

/**
 * Koin DI module for the tempo subsystem.
 *
 * Provides [BeatClock] with automatic Ableton Link / tap-tempo fallback:
 *
 * - [LinkSession] — platform-specific bridge to the native Link SDK.
 * - [TapTempoClock] — manual tap-tempo as fallback when no Link peers.
 * - [AbletonLinkClock] — polls Link session for BPM and phase.
 * - [LinkFallbackClock] — composite clock that auto-switches between Link and tap.
 *
 * ## Usage
 *
 * The primary [BeatClock] binding resolves to [LinkFallbackClock], which
 * transparently switches between Link and tap-tempo based on peer availability.
 *
 * ```kotlin
 * val beatClock: BeatClock by inject()
 * beatClock.start()
 * // Observe beatClock.bpm, beatClock.beatPhase, etc.
 * ```
 *
 * ## Overriding
 *
 * To force tap-tempo only (e.g., in settings), use the named binding:
 * ```kotlin
 * val tapClock: TapTempoClock = get(named("tapClock"))
 * ```
 */
val tempoModule = module {

    // Platform-specific Link session (expect/actual)
    single<LinkSessionApi> { LinkSession() }

    // Tap-tempo clock (manual BPM)
    single { TapTempoClock(scope = get<CoroutineScope>()) }

    // Ableton Link clock (network-synced BPM)
    single {
        AbletonLinkClock(
            scope = get<CoroutineScope>(),
            linkSession = get<LinkSessionApi>()
        )
    }

    // Composite clock: auto-fallback from Link to tap-tempo
    single {
        LinkFallbackClock(
            scope = get<CoroutineScope>(),
            linkClock = get<AbletonLinkClock>(),
            tapClock = get<TapTempoClock>()
        )
    }

    // Primary BeatClock binding — resolves to LinkFallbackClock
    single<BeatClock> { get<LinkFallbackClock>() }
}
