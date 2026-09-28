package com.pebblentn.app.data

import android.content.Context
import android.content.SharedPreferences
import com.pebblentn.app.core.WatchSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists and exposes user-configurable watch behavior settings (REQ-ANDROID-009, REQ-WATCH-005).
 *
 * Stores settings in [PREFS_NAME] (`"pebblentn_settings"`), matching [AppEnabledRepository].
 *
 * [isAutoLaunchEnabled] is cached in a `@Volatile` field and never performs I/O on callback threads.
 */
class WatchSettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var cached: WatchSettings = WatchSettings(
        autoLaunchOnSessionStart = prefs.getBoolean(KEY_AUTO_LAUNCH, DEFAULT_AUTO_LAUNCH),
    )

    private val _settings = MutableStateFlow(cached)
    val settings: StateFlow<WatchSettings> = _settings.asStateFlow()

    private val _autoLaunchEnabled = MutableStateFlow(cached.autoLaunchOnSessionStart)
    val autoLaunchEnabled: StateFlow<Boolean> = _autoLaunchEnabled.asStateFlow()

    /** Synchronous read for the transport / callback threads. Never does I/O. */
    fun isAutoLaunchEnabled(): Boolean = cached.autoLaunchOnSessionStart

    fun setAutoLaunch(enabled: Boolean) {
        val updated = cached.copy(autoLaunchOnSessionStart = enabled)
        cached = updated
        _settings.value = updated
        _autoLaunchEnabled.value = enabled
        prefs.edit().putBoolean(KEY_AUTO_LAUNCH, enabled).apply()
    }

    companion object {
        const val DEFAULT_AUTO_LAUNCH = true
        private const val PREFS_NAME = "pebblentn_settings"
        private const val KEY_AUTO_LAUNCH = "watch_auto_launch"
    }
}
