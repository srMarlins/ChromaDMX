package com.chromadmx.ios

import com.chromadmx.Greeting
import com.chromadmx.agent.di.agentModule
import com.chromadmx.di.chromaDiModule
import com.chromadmx.di.iosPlatformModule
import com.chromadmx.ui.di.uiModule
import org.koin.core.context.startKoin

/**
 * iOS-specific application setup and configuration for ChromaDMX.
 *
 * Called from Swift before presenting the ComposeUIViewController:
 * ```swift
 * import Shared
 *
 * @main
 * struct ChromaDMXApp: App {
 *     init() {
 *         IosApp.shared.initialize()
 *     }
 *     var body: some Scene {
 *         WindowGroup {
 *             ComposeView()
 *                 .ignoresSafeArea()
 *         }
 *     }
 * }
 * ```
 */
object IosApp {

    /**
     * Initialize the iOS application.
     *
     * Call this once from the Swift app entry point before presenting any UI.
     * Sets up Koin dependency injection with all shared and iOS platform modules.
     */
    fun initialize() {
        startKoin {
            modules(chromaDiModule, agentModule, iosPlatformModule, uiModule)
        }
    }

    /**
     * Get the platform greeting for verification.
     * Useful for confirming the KMP framework is loaded correctly.
     */
    fun greeting(): String = Greeting().greet()
}
