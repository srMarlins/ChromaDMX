package com.chromadmx.android

import android.app.Application
import com.chromadmx.agent.config.AgentConfig
import com.chromadmx.agent.di.agentModule
import com.chromadmx.di.chromaDiModule
import com.chromadmx.ui.di.uiModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module

/**
 * Application class that initializes Koin dependency injection.
 *
 * Registered in AndroidManifest.xml via android:name.
 */
class ChromaDMXApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val androidKeysModule = module {
            single {
                val googleKey = BuildConfig.GOOGLE_API_KEY
                val anthropicKey = BuildConfig.ANTHROPIC_API_KEY
                when {
                    googleKey.isNotBlank() -> AgentConfig(apiKey = googleKey, modelId = "gemini_2_5_flash")
                    anthropicKey.isNotBlank() -> AgentConfig(apiKey = anthropicKey, modelId = "sonnet_4_5")
                    else -> AgentConfig()
                }
            }
        }

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@ChromaDMXApp)
            allowOverride(true)
            modules(chromaDiModule, agentModule, androidKeysModule, uiModule)
        }
    }
}
