package com.chromadmx.ios

import com.chromadmx.Greeting

/**
 * iOS-specific application setup and configuration for ChromaDMX.
 *
 * This object provides iOS platform initialization that runs before
 * the Compose UI is presented. It handles:
 * - Koin dependency injection setup with iOS-specific modules
 * - Platform service initialization (Network.framework UDP, AVFoundation camera)
 * - Any iOS-specific lifecycle coordination
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
 *
 *     var body: some ScenePreset {
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
     * Sets up dependency injection and platform services.
     */
    fun initialize() {
        // TODO: Initialize Koin with iOS-specific modules
        // startKoin {
        //     modules(
        //         sharedModule,
        //         iosNetworkModule,    // Network.framework UDP transport
        //         iosCameraModule,     // AVFoundation camera source
        //         iosPlatformModule,   // iOS-specific platform services
        //     )
        // }
    }

    /**
     * Get the platform greeting for verification.
     * Useful for confirming the KMP framework is loaded correctly.
     */
    fun greeting(): String = Greeting().greet()
}
