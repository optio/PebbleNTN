package com.pebblentn.app

import android.app.Application
import com.pebblentn.app.di.AppContainer
import timber.log.Timber

/**
 * Application entry point. Builds the manual [AppContainer] (no Hilt/Dagger, per the coding
 * standards) and shares it with the listener service and view models.
 */
class PebbleNtnApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        container = AppContainer(this)
        container.start()
    }
}
