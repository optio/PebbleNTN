package com.pebblentn.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The master switch: whether PebbleNTN processes notifications at all (REQ-ANDROID-011).
 *
 * Enabled by default. When it is off, the notification listener stays connected — Android would
 * otherwise re-prompt for access — but [NotificationDispatcher][com.pebblentn.app.notification.NotificationDispatcher]
 * drops every event *before* the package allowlist is consulted and before any notification content
 * is read, so a disabled app reads nothing and stores nothing.
 *
 * [isEnabled] is read synchronously on the listener's callback thread, so the value is cached in a
 * `@Volatile` field and never hits disk on that path.
 */
class AppEnabledRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var cached: Boolean = prefs.getBoolean(KEY_ENABLED, DEFAULT_ENABLED)

    private val _enabled = MutableStateFlow(cached)

    /** Observable state for the UI. */
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    /** Synchronous read for the notification callback thread. Never does I/O. */
    fun isEnabled(): Boolean = cached

    fun setEnabled(enabled: Boolean) {
        cached = enabled
        _enabled.value = enabled
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    companion object {
        const val DEFAULT_ENABLED = true
        private const val PREFS_NAME = "pebblentn_settings"
        private const val KEY_ENABLED = "app_enabled"
    }
}
