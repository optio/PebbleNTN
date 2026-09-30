package com.pebblentn.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether the PebbleNTN watchapp is known to be installed, for the setup checklist (#28).
 *
 * PebbleKit cannot list the apps installed on a watch, and a failed send never says "not
 * installed". So this only records proof that it is there: the companion reporting our watchapp
 * opened, any message from it, or a successful send to it ([markSeen]). A user can also confirm it
 * by hand ([confirmManually]) before the watchapp has ever been opened. An uninstall later cannot
 * be detected, so once known it stays known.
 */
class WatchappPresenceRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _detected = MutableStateFlow(prefs.getLong(KEY_SEEN_AT, 0L) > 0L || prefs.getBoolean(KEY_MANUAL, false))
    val detected: StateFlow<Boolean> = _detected.asStateFlow()

    /** Proof the watchapp is installed arrived; cheap to call on every signal. */
    fun markSeen(atMillis: Long) {
        if (prefs.getLong(KEY_SEEN_AT, 0L) == 0L) prefs.edit().putLong(KEY_SEEN_AT, atMillis).apply()
        _detected.value = true
    }

    /** The user says the watchapp is installed, before it has been opened. */
    fun confirmManually() {
        prefs.edit().putBoolean(KEY_MANUAL, true).apply()
        _detected.value = true
    }

    private companion object {
        const val PREFS_NAME = "pebblentn_settings"
        const val KEY_SEEN_AT = "watchapp_seen_at"
        const val KEY_MANUAL = "watchapp_confirmed_manually"
    }
}
