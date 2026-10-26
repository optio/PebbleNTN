package com.pebblentn.app.update

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.TimeUnit

/** What a check produced, so the UI can give the user immediate feedback on a manual check. */
enum class UpdateCheckOutcome {
    /** Not forced and the weekly interval has not elapsed; nothing was fetched. */
    SKIPPED_NOT_DUE,
    UP_TO_DATE,
    UPDATE_AVAILABLE,
    /** The fetch failed (offline, error). Last-known state is retained. */
    FAILED,
    /**
     * This install source never asks GitHub (a Google Play install, [UpdatePolicy.allowsGithubCheck]
     * false); nothing was fetched (REQ-ANDROID-022).
     */
    NOT_ALLOWED,
}

/** Update state for the UI. */
data class UpdateState(
    val currentVersion: String,
    /** Latest version last learned from GitHub, or null if never successfully checked. */
    val latestVersion: String?,
    val checking: Boolean = false,
    /**
     * The watchapp version last seen in a genuine WATCH_READY, or null if none ever arrived
     * (REQ-ANDROID-019). Both apps share one version number, so a newer watchapp means a newer
     * phone app has been published.
     */
    val watchAppVersion: String? = null,
    /**
     * Where this app was installed from. A Google Play install ignores [latestVersion], which can
     * only come from a GitHub check an earlier GitHub install made (REQ-ANDROID-022).
     */
    val installSource: InstallSource = InstallSource.GITHUB,
) {
    /** GitHub reports a newer version (tolerant tag compare), and this install source uses GitHub. */
    private val githubNewer: Boolean
        get() {
            if (UpdatePolicy.forSource(installSource).ignoresStoredGithubVersion) return false
            return latestVersion?.let { AppVersions.isNewer(currentVersion, it) } ?: false
        }

    /** The watch reports a strictly newer version; an unparsable one is ignored (REQ-ANDROID-019). */
    private val watchNewer: Boolean
        get() = AppVersions.isNewerStrict(currentVersion, watchAppVersion)

    /** An update is known: from the internet check, or from the watch's version with no network. */
    val updateAvailable: Boolean
        get() = githubNewer || watchNewer

    /**
     * The version to offer: the higher of the GitHub version (when it is newer) and the watch's
     * version (when it is strictly newer), or null when there is no update. This is what the card
     * shows and what the watch is told (REQ-ANDROID-017, REQ-ANDROID-019).
     */
    val effectiveLatest: String?
        get() {
            val github = latestVersion?.takeIf { githubNewer }
            val watch = watchAppVersion?.takeIf { watchNewer }
            return when {
                github == null -> watch
                watch == null -> github
                AppVersions.compare(watch, github) > 0 -> watch
                else -> github
            }
        }
}

/**
 * Knows whether a newer app version exists. The default way is offline: it remembers the watchapp
 * version the watch last announced ([recordWatchAppVersion]), and a newer watchapp reveals an
 * update with no network access at all (REQ-ANDROID-019). The GitHub check is an optional extra
 * (REQ-ANDROID-013): on demand, or at most once a week when the user turns it on. Its weekly
 * cadence and last-known latest version are persisted, so the "update available" prompt survives
 * restarts without re-hitting the network every launch. It performs the app's only network access
 * and sends no user data (see [GitHubReleaseFetcher]).
 *
 * On a Google Play install ([installSource] PLAY, REQ-ANDROID-022) the GitHub part is off: no check
 * ever runs, and the GitHub version, weekly setting and check time an earlier GitHub install stored
 * are ignored (they stay in the preferences untouched, so nothing is lost if the user goes back).
 */
class UpdateCheckRepository(
    context: Context,
    private val currentVersion: String,
    private val fetcher: ReleaseFetcher = GitHubReleaseFetcher(),
    private val now: () -> Long = { System.currentTimeMillis() },
    private val installSource: InstallSource = InstallSource.GITHUB,
) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val policy = UpdatePolicy.forSource(installSource)

    private val _state = MutableStateFlow(
        UpdateState(
            currentVersion,
            latestVersion = if (policy.ignoresStoredGithubVersion) null else prefs.getString(KEY_LATEST_VERSION, null),
            watchAppVersion = prefs.getString(KEY_WATCH_APP_VERSION, null),
            installSource = installSource,
        ),
    )
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    /**
     * Record the watchapp version a genuine WATCH_READY carried (key 11), so a newer watchapp shows
     * the update card with no network call (REQ-ANDROID-019). The Core Devices app forwards
     * WATCH_READY (confirmed in v0.0.74 with `companionApp` declared), so this is the default update
     * check. A synthesized readiness carries no version and never calls this. A blank version is no
     * version and is ignored. The preference is written only when the version changed (flash wear); returns true
     * only then.
     */
    fun recordWatchAppVersion(version: String): Boolean {
        val trimmed = version.trim()
        if (trimmed.isEmpty()) return false
        synchronized(this) {
            if (_state.value.watchAppVersion == trimmed) return false
            prefs.edit().putString(KEY_WATCH_APP_VERSION, trimmed).apply()
            _state.update { it.copy(watchAppVersion = trimmed) }
        }
        return true
    }

    private val _autoCheckEnabled = MutableStateFlow(
        policy.allowsGithubCheck && prefs.getBoolean(KEY_AUTO_CHECK, DEFAULT_AUTO_CHECK),
    )
    /**
     * Whether the weekly automatic check may run. Off by default: with it off the app makes NO
     * network connection on its own — only an explicit "Check for updates" tap does. The INTERNET
     * permission is a normal install-time permission (Android grants it silently; it cannot be
     * requested at runtime), so this setting, not the permission, is what gates automatic access.
     */
    val autoCheckEnabled: StateFlow<Boolean> = _autoCheckEnabled.asStateFlow()

    /** Turn the weekly check on or off. Ignored on an install source that never asks GitHub. */
    fun setAutoCheckEnabled(enabled: Boolean) {
        if (!policy.allowsGithubCheck) {
            timber.log.Timber.w("Weekly update check is not available for %s installs", installSource)
            return
        }
        _autoCheckEnabled.value = enabled
        prefs.edit().putBoolean(KEY_AUTO_CHECK, enabled).apply()
    }

    private val _lastCheckMillis = MutableStateFlow(prefs.getLong(KEY_LAST_CHECK, 0L))
    /**
     * When a check last ran (succeeded or failed), in epoch millis; 0 when one never has. This is the
     * same `last_check_millis` the weekly throttle reads (REQ-ANDROID-018, #94).
     */
    val lastCheckMillis: StateFlow<Long> = _lastCheckMillis.asStateFlow()

    /**
     * True when no check has ever run, or the last one was more than [STALE_AFTER_MS] ago. The
     * dashboard then puts "Check for updates" first, so installs with weekly checks off still learn
     * about updates (REQ-ANDROID-018, #94). Reading this makes no network call.
     */
    fun isCheckStale(now: Long = this.now()): Boolean {
        // A source that never asks GitHub has nothing to go stale (REQ-ANDROID-022).
        if (!policy.allowsGithubCheck) return false
        val last = _lastCheckMillis.value
        return last == 0L || now - last > STALE_AFTER_MS
    }

    /**
     * Check for a newer version. When [force] is false the network is only hit if at least a week
     * has passed since the last attempt; a manual check passes [force] = true. Safe to call on every
     * launch — it self-throttles. On a source that never asks GitHub it returns
     * [UpdateCheckOutcome.NOT_ALLOWED] without touching the network or the preferences.
     */
    suspend fun checkForUpdate(force: Boolean): UpdateCheckOutcome {
        if (!policy.allowsGithubCheck) return UpdateCheckOutcome.NOT_ALLOWED
        val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0L)
        val timestamp = now()
        if (!force && timestamp - lastCheck < CHECK_INTERVAL_MS) {
            return UpdateCheckOutcome.SKIPPED_NOT_DUE
        }

        _state.value = _state.value.copy(checking = true)
        val latest = fetcher.latestVersion()
        // Record the attempt time either way so a failing check does not retry on every launch.
        val editor = prefs.edit().putLong(KEY_LAST_CHECK, timestamp)
        _lastCheckMillis.value = timestamp
        if (latest == null) {
            editor.apply()
            _state.value = _state.value.copy(checking = false)
            return UpdateCheckOutcome.FAILED
        }
        editor.putString(KEY_LATEST_VERSION, latest).apply()
        // Keep the watch's version: the card offers the higher of the two (REQ-ANDROID-019).
        _state.update { it.copy(latestVersion = latest, checking = false) }
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
        private const val KEY_WATCH_APP_VERSION = "watch_app_version"
        /** Automatic weekly checks are opt-in, so the app never touches the network unprompted. */
        const val DEFAULT_AUTO_CHECK = false
        val CHECK_INTERVAL_MS: Long = TimeUnit.DAYS.toMillis(7)
        /** A check older than this is stale and is offered first on the dashboard (REQ-ANDROID-018, #94). */
        val STALE_AFTER_MS: Long = TimeUnit.DAYS.toMillis(30)
    }
}
