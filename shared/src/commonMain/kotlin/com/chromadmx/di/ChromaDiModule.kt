package com.chromadmx.di

import com.chromadmx.core.model.Fixture3D
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
import com.chromadmx.networking.discovery.NodeDiscovery
import com.chromadmx.networking.output.DmxOutputService
import com.chromadmx.networking.transport.PlatformUdpTransport
import com.chromadmx.pipeline.DmxPipeline
import com.chromadmx.tempo.clock.BeatClock
import com.chromadmx.tempo.tap.TapTempoClock
import com.chromadmx.vision.RealDmxController
import com.chromadmx.vision.calibration.DmxController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.qualifier.named
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

    // --- Fixture provider (shared state) ---
    single { MutableStateFlow<List<Fixture3D>>(emptyList()) }
    single<() -> List<Fixture3D>> {
        val state = get<MutableStateFlow<List<Fixture3D>>>()
        { state.value }
    }

    // --- DMX Pipeline (bridging engine to networking) ---
    single {
        DmxPipeline(
            scope = get(),
            engine = get(),
            dmxOutput = get(),
            fixturesProvider = get()
        )
    }

    // --- Vision calibration ---
    single<DmxController> {
        RealDmxController(
            engine = get(),
            fixturesProvider = get()
        )
    }
}
