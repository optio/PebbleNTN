package com.pebblentn.app.data

import android.content.Context
import android.content.SharedPreferences
import com.pebblentn.app.core.WatchCapabilities
import com.pebblentn.app.core.WatchPlatform
import com.pebblentn.app.core.WatchSetting
import com.pebblentn.app.core.WatchSettingsModel
import com.pebblentn.app.core.WatchSettingsSyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Where the phone keeps its mirror of the watch settings (REQ-ANDROID-021, #102). */
interface WatchSettingsStore {
    val state: StateFlow<WatchSettingsSyncState>

    /** Replace the state with [transform] of it, atomically, and keep it; returns the new state. */
    fun update(transform: (WatchSettingsSyncState) -> WatchSettingsSyncState): WatchSettingsSyncState
}

/** A store that lives only as long as the process: the default for tests and previews. */
class InMemoryWatchSettingsStore(initial: WatchSettingsSyncState = WatchSettingsSyncState()) : WatchSettingsStore {
    private val _state = MutableStateFlow(initial)
    override val state: StateFlow<WatchSettingsSyncState> = _state.asStateFlow()

    @Synchronized
    override fun update(transform: (WatchSettingsSyncState) -> WatchSettingsSyncState): WatchSettingsSyncState {
        val updated = transform(_state.value)
        _state.value = updated
        return updated
    }
}

/**
 * Persists the mirror of the watch settings, the phone's revision, the edits waiting for the
 * watchapp and whether real settings ever arrived, so the screen and the queue survive a restart
 * (REQ-ANDROID-021). Stored in its own SharedPreferences file, [PREFS_NAME].
 *
 * The state is cached in a `@Volatile` field and exposed as a [StateFlow]; reads never do I/O.
 */
class WatchSettingsMirrorRepository(context: Context) : WatchSettingsStore {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var cached: WatchSettingsSyncState = load()

    private val _state = MutableStateFlow(cached)
    override val state: StateFlow<WatchSettingsSyncState> = _state.asStateFlow()

    @Synchronized
    override fun update(transform: (WatchSettingsSyncState) -> WatchSettingsSyncState): WatchSettingsSyncState {
        val updated = transform(cached)
        if (updated != cached) {
            cached = updated
            _state.value = updated
            save(updated)
        }
        return updated
    }

    private fun load(): WatchSettingsSyncState {
        val mirror = if (prefs.contains(KEY_REVISION)) {
            val capabilities = WatchCapabilities(prefs.getInt(KEY_CAPABILITIES, 0))
            WatchSettingsModel.fromWatch(
                carried = WatchSetting.entries
                    .filter { prefs.contains(valueKey(it)) }
                    .associateWith { prefs.getInt(valueKey(it), 0) },
                revision = prefs.getInt(KEY_REVISION, 0),
                platform = WatchPlatform.fromCode(prefs.getInt(KEY_PLATFORM, WatchPlatform.UNKNOWN.code)),
                capabilities = capabilities,
            )
        } else {
            null
        }
        val pending = prefs.getStringSet(KEY_PENDING, emptySet()).orEmpty()
            .mapNotNull { name -> WatchSetting.entries.firstOrNull { it.name == name } }
            .toSet()
        return WatchSettingsSyncState(
            mirror = mirror,
            pending = if (mirror == null) emptySet() else pending,
            receivedFromWatch = prefs.getBoolean(KEY_RECEIVED, false),
            watchappOutdated = prefs.getBoolean(KEY_OUTDATED, false),
        )
    }

    private fun save(state: WatchSettingsSyncState) {
        val editor = prefs.edit().clear()
        state.mirror?.let { mirror ->
            editor.putInt(KEY_REVISION, mirror.revision)
                .putInt(KEY_PLATFORM, mirror.platform.code)
                .putInt(KEY_CAPABILITIES, mirror.capabilities.mask)
            for (setting in WatchSetting.entries) editor.putInt(valueKey(setting), mirror[setting])
        }
        editor.putStringSet(KEY_PENDING, state.pending.map { it.name }.toSet())
            .putBoolean(KEY_RECEIVED, state.receivedFromWatch)
            .putBoolean(KEY_OUTDATED, state.watchappOutdated)
            .apply()
    }

    private fun valueKey(setting: WatchSetting) = "value_${setting.name.lowercase()}"

    private companion object {
        const val PREFS_NAME = "pebblentn_watch_settings"
        const val KEY_REVISION = "revision"
        const val KEY_PLATFORM = "platform"
        const val KEY_CAPABILITIES = "capabilities"
        const val KEY_PENDING = "pending"
        const val KEY_RECEIVED = "received_from_watch"
        const val KEY_OUTDATED = "watchapp_outdated"
    }
}
