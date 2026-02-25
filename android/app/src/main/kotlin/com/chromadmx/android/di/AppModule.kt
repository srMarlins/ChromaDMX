package com.chromadmx.android.di

import org.koin.dsl.module

/**
 * Main Koin DI module for the ChromaDMX Android app.
 *
 * ViewModels, services, and platform implementations will be registered here
 * as shared modules are integrated (e.g., EffectEngine, DMXTransport, VisionProcessor).
 */
val appModule = module {
    // Placeholder — modules will be added as shared components are integrated:
    // - EffectEngine (shared)
    // - DMXTransport (platform-actual)
    // - VisionProcessor (platform-actual)
    // - ViewModels for each screen
}
