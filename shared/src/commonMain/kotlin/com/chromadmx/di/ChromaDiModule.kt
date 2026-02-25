package com.chromadmx.di

import com.chromadmx.core.model.Fixture3D
import com.chromadmx.engine.bridge.DmxBridge
import com.chromadmx.engine.bridge.DmxOutputBridge
import com.chromadmx.engine.effect.EffectRegistry
import com.chromadmx.engine.effects.Chase3DEffect
import com.chromadmx.engine.effects.GradientSweep3DEffect
import com.chromadmx.engine.effects.ParticleBurst3DEffect
import com.chromadmx.engine.effects.PerlinNoise3DEffect
import com.chromadmx.engine.effects.RadialPulse3DEffect
import com.chromadmx.engine.effects.RainbowSweep3DEffect
import com.chromadmx.engine.effects.SolidColorEffect
import com.chromadmx.engine.effects.StrobeEffect
import com.chromadmx.engine.effects.WaveEffect3DEffect
import com.chromadmx.engine.effect.EffectStack
import com.chromadmx.engine.pipeline.EffectEngine
import com.chromadmx.engine.preset.PresetLibrary
import com.chromadmx.networking.discovery.NodeDiscovery
import com.chromadmx.networking.output.DmxOutputService
import com.chromadmx.networking.transport.PlatformUdpTransport
import com.chromadmx.tempo.clock.BeatClock
import com.chromadmx.tempo.tap.TapTempoClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

/**
 * Central DI module that wires all shared business logic:
 * tempo, networking, engine, and fixture provider.
 *
 * This lives in shared code (KMP-compatible). Platform-specific modules
 * (Android/iOS) only need to add platform transports if overriding defaults.
 */
val chromaDiModule = module {

    // --- Coroutine scope ---
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    // --- Tempo ---
    // Currently using TapTempoClock directly. To enable Ableton Link with
    // automatic fallback to tap-tempo, replace this binding with:
    //   includes(com.chromadmx.tempo.di.tempoModule)
    // and remove the single<BeatClock> line below.
    // See: shared/tempo/src/commonMain/.../di/TempoModule.kt
    single<BeatClock> { TapTempoClock(scope = get()) }

    // --- Networking ---
    single { PlatformUdpTransport() }
    single { NodeDiscovery(transport = get()) }
    single { DmxOutputService(transport = get()) }

    // --- Engine ---
    single {
        EffectRegistry().apply {
            register(SolidColorEffect())
            register(StrobeEffect())
            register(Chase3DEffect())
            register(GradientSweep3DEffect())
            register(RainbowSweep3DEffect())
            register(RadialPulse3DEffect())
            register(WaveEffect3DEffect())
            register(ParticleBurst3DEffect())
            register(PerlinNoise3DEffect())
        }
    }
    single {
        EffectEngine(scope = get(), fixtures = emptyList()).apply {
            beatStateProvider = { get<BeatClock>().beatState.value }
            start()
        }
    }
    single { get<EffectEngine>().effectStack }

    // --- Engine -> DMX Bridge ---
    single {
        val engine = get<EffectEngine>()
        DmxBridge(engine.fixtures, emptyMap())
    }
    single {
        val engine = get<EffectEngine>()
        val bridge = get<DmxBridge>()
        val outputService = get<DmxOutputService>()
        DmxOutputBridge(
            colorOutput = engine.colorOutput,
            dmxBridge = bridge,
            onFrame = { frame -> outputService.updateFrame(frame) },
            scope = get()
        ).apply { start() }
    }

    // --- Presets ---
    single { PresetLibrary(get(), get(), get()) }

    // --- Fixture provider (empty default — StageViewModel manages fixtures) ---
    single<() -> List<Fixture3D>> { { emptyList() } }
}
