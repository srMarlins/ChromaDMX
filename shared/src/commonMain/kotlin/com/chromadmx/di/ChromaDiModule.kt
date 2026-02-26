package com.chromadmx.di

import com.chromadmx.core.db.ChromaDmxDatabase
import com.chromadmx.core.db.DriverFactory
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.persistence.FixtureRepository
import com.chromadmx.core.persistence.FixtureStore
import com.chromadmx.core.persistence.NetworkStateRepository
import com.chromadmx.core.persistence.PresetRepository
import com.chromadmx.core.persistence.SettingsRepository
import com.chromadmx.core.persistence.SettingsStore
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
import com.chromadmx.engine.pipeline.EffectEngine
import com.chromadmx.engine.preset.PresetLibrary
import com.chromadmx.networking.DmxTransport
import com.chromadmx.networking.DmxTransportRouter
import com.chromadmx.networking.FixtureDiscovery
import com.chromadmx.networking.FixtureDiscoveryRouter
import com.chromadmx.networking.discovery.NodeDiscovery
import com.chromadmx.networking.output.DmxOutputService
import com.chromadmx.networking.transport.PlatformUdpTransport
import com.chromadmx.simulation.network.SimulatedDiscovery
import com.chromadmx.simulation.network.SimulatedTransport
import com.chromadmx.tempo.clock.BeatClock
import com.chromadmx.tempo.tap.TapTempoClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Central DI module that wires all shared business logic:
 * tempo, networking (real + simulated via routers), engine, and persistence.
 *
 * The router pattern ([DmxTransportRouter], [FixtureDiscoveryRouter]) enables
 * runtime switching between real hardware and simulation without restarting.
 * Named qualifiers ("real" / "simulated") disambiguate the two implementations
 * while the unqualified [DmxTransport] and [FixtureDiscovery] bindings resolve
 * to the routers, giving consumers a single dependency.
 */
val chromaDiModule = module {

    // --- Coroutine scope ---
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    // --- Tempo ---
    single<BeatClock> { TapTempoClock(scope = get()) }

    // --- Networking: Real ---
    single { PlatformUdpTransport() }
    single { NodeDiscovery(transport = get()) }
    single(named("real")) { DmxOutputService(transport = get()) } bind DmxTransport::class

    // --- Networking: Simulated ---
    single(named("simulated")) { SimulatedTransport() } bind DmxTransport::class
    single(named("simulated")) { SimulatedDiscovery() } bind FixtureDiscovery::class

    // --- Networking: Routers ---
    single {
        DmxTransportRouter(
            real = get(named("real")),
            simulated = get(named("simulated")),
            scope = get(),
        )
    } bind DmxTransport::class

    single {
        FixtureDiscoveryRouter(
            real = get<NodeDiscovery>(),
            simulated = get(named("simulated")),
            scope = get(),
        )
    } bind FixtureDiscovery::class

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

    // --- Engine -> DMX Bridge (routed through transport router) ---
    single {
        val engine = get<EffectEngine>()
        DmxBridge(engine.fixtures, emptyMap())
    }
    single {
        val engine = get<EffectEngine>()
        val bridge = get<DmxBridge>()
        val router = get<DmxTransportRouter>()
        DmxOutputBridge(
            colorOutput = engine.colorOutput,
            dmxBridge = bridge,
            onFrame = { frame -> router.updateFrame(frame) },
            scope = get()
        ).apply { start() }
    }

    // --- Database ---
    single { get<DriverFactory>().createDriver() }
    single { ChromaDmxDatabase(get()) }
    single { FixtureRepository(get()) } bind FixtureStore::class
    single { NetworkStateRepository(get()) }
    single { PresetRepository(get()) }
    single { SettingsRepository(get()) } bind SettingsStore::class

    // --- Presets ---
    single { PresetLibrary(get(), get(), get()) }

    // --- Fixture provider (empty default — StageViewModelV2 manages fixtures) ---
    single<() -> List<Fixture3D>> { { emptyList() } }
}
