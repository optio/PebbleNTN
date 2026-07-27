package com.pebblentn.app.update

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

/** What a check produced, so the UI can give the user immediate feedback on a manual check. */
enum class UpdateCheckOutcome {
    /** Not forced and the weekly interval has not elapsed; nothing was fetched. */
    SKIPPED_NOT_DUE,
    UP_TO_DATE,
    UPDATE_AVAILABLE,
    /** The fetch failed (offline, error). Last-known state is retained. */
    FAILED,
}

/** Update state for the UI. */
data class UpdateState(
    val currentVersion: String,
    /** Latest version last learned from GitHub, or null if never successfully checked. */
    val latestVersion: String?,
    val checking: Boolean = false,
) {
    val updateAvailable: Boolean
        get() = latestVersion?.let { AppVersions.isNewer(currentVersion, it) } ?: false
}

/**
 * Checks GitHub for a newer app version at most once a week (or on demand) and remembers the result
 * (REQ-ANDROID-013). The weekly cadence and the last-known latest version are persisted, so the
 * "update available" prompt survives restarts without re-hitting the network every launch. This
 * performs the app's only network access and sends no user data (see [GitHubReleaseFetcher]).
 */
class UpdateCheckRepository(
    context: Context,
    private val currentVersion: String,
    private val fetcher: ReleaseFetcher = GitHubReleaseFetcher(),
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        UpdateState(currentVersion, prefs.getString(KEY_LATEST_VERSION, null)),
    )
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val _autoCheckEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_CHECK, DEFAULT_AUTO_CHECK))
    /**
     * Whether the weekly automatic check may run. Off by default: with it off the app makes NO
     * network connection on its own — only an explicit "Check for updates" tap does. The INTERNET
     * permission is a normal install-time permission (Android grants it silently; it cannot be
     * requested at runtime), so this setting, not the permission, is what gates automatic access.
     */
    val autoCheckEnabled: StateFlow<Boolean> = _autoCheckEnabled.asStateFlow()

    fun setAutoCheckEnabled(enabled: Boolean) {
        _autoCheckEnabled.value = enabled
        prefs.edit().putBoolean(KEY_AUTO_CHECK, enabled).apply()
    }

    /**
     * Check for a newer version. When [force] is false the network is only hit if at least a week
     * has passed since the last attempt; a manual check passes [force] = true. Safe to call on every
     * launch — it self-throttles.
     */
    suspend fun checkForUpdate(force: Boolean): UpdateCheckOutcome {
        val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0L)
        val timestamp = now()
        if (!force && timestamp - lastCheck < CHECK_INTERVAL_MS) {
            return UpdateCheckOutcome.SKIPPED_NOT_DUE
        }

        _state.value = _state.value.copy(checking = true)
        val latest = fetcher.latestVersion()
        // Record the attempt time either way so a failing check does not retry on every launch.
        val editor = prefs.edit().putLong(KEY_LAST_CHECK, timestamp)
        if (latest == null) {
            editor.apply()
            _state.value = _state.value.copy(checking = false)
            return UpdateCheckOutcome.FAILED
        }
        editor.putString(KEY_LATEST_VERSION, latest).apply()
        _state.value = UpdateState(currentVersion, latest, checking = false)
        return if (AppVersions.isNewer(currentVersion, latest)) {
            UpdateCheckOutcome.UPDATE_AVAILABLE
        } else {
            UpdateCheckOutcome.UP_TO_DATE
        }
    }

    companion object {
        private const val PREFS_NAME = "pebblentn_update_check"
        private const val KEY_LAST_CHECK = "last_check_millis"
        private const val KEY_LATEST_VERSION = "latest_version"
        private const val KEY_AUTO_CHECK = "auto_check_enabled"
        /** Automatic weekly checks are opt-in, so the app never touches the network unprompted. */
        const val DEFAULT_AUTO_CHECK = false
        val CHECK_INTERVAL_MS: Long = TimeUnit.DAYS.toMillis(7)
    }
}
