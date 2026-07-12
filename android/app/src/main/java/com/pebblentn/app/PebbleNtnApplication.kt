package com.pebblentn.app

import android.app.Application
import timber.log.Timber

/**
 * Application entry point.
 *
 * M0 scaffold: only wires debug logging. Repositories, the notification listener, the rule
 * engine and the Pebble transport are introduced in later milestones behind explicit
 * constructor injection (no Hilt/Dagger, per the coding standards).
 */
class PebbleNtnApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
