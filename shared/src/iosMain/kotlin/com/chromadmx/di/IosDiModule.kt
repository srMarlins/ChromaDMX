package com.chromadmx.di

import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * iOS-specific Koin dependency injection module.
 *
 * Provides iOS platform implementations for shared interfaces:
 * - UDP transport via Network.framework
 * - Camera source via AVFoundation
 * - Ableton Link bridge via LinkKit cinterop
 *
 * This module is combined with the shared common modules when
 * initializing Koin in IosApp.initialize().
 *
 * Usage:
 * ```kotlin
 * startKoin {
 *     modules(
 *         commonModule,     // Shared business logic
 *         iosPlatformModule // This module - iOS implementations
 *     )
 * }
 * ```
 */
val iosPlatformModule: Module = module {
    // TODO: Register iOS platform implementations as modules are created
    //
    // single<UdpTransport> { IosUdpTransport() }
    // single<CameraSource> { IosCameraSource() }
    // single<BeatClock> { IosAbletonLinkBridge() }
}
