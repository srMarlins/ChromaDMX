package com.chromadmx.di

import com.chromadmx.agent.config.AgentConfig
import com.chromadmx.agent.config.ApiKeyProvider
import com.chromadmx.core.db.DriverFactory
import com.chromadmx.core.persistence.FileStorage
import com.chromadmx.core.persistence.IosFileStorage
import com.chromadmx.networking.ble.BleProvisioner
import com.chromadmx.networking.ble.BleScanner
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * iOS-specific Koin dependency injection module.
 *
 * Provides iOS platform implementations for shared interfaces,
 * mirroring the Android platform module in ChromaDMXApp.kt.
 *
 * Combined with shared common modules in IosApp.initialize().
 */
val iosPlatformModule: Module = module {
    single<FileStorage> { IosFileStorage() }
    single { DriverFactory() }
    single { BleScanner() }
    single { BleProvisioner() }
    single {
        val provider = ApiKeyProvider()
        val googleKey = provider.getGoogleKey() ?: ""
        val anthropicKey = provider.getAnthropicKey() ?: ""
        when {
            googleKey.isNotBlank() -> AgentConfig(apiKey = googleKey, modelId = "gemini_2_5_flash")
            anthropicKey.isNotBlank() -> AgentConfig(apiKey = anthropicKey, modelId = "sonnet_4_5")
            else -> AgentConfig()
        }
    }
}
