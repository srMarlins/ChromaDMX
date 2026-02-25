package com.chromadmx.android

import android.app.Application
import com.chromadmx.android.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Application class that initializes Koin dependency injection.
 *
 * Registered in AndroidManifest.xml via android:name.
 */
class ChromaDMXApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@ChromaDMXApp)
            modules(appModule)
        }
    }
}
