package com.pebblentn.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.pebblentn.app.catalog.UserPackageStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * The package names the user added to navigation apps (FR5), per catalog app id, in the order they
 * were added. Kept in private SharedPreferences, so they survive restarts and app updates.
 *
 * Storage only: the checks (format, one name per app) are [com.pebblentn.app.catalog.CustomPackageValidator]'s,
 * applied by [com.pebblentn.app.catalog.EffectiveCatalog] before [add]. [add] still ignores a name the
 * app already has. A stored value that can't be read is logged and treated as no packages, rather than
 * crashing the app at start.
 */
class CustomPackagesRepository(private val prefs: SharedPreferences) : UserPackageStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
    )

    private val _packages = MutableStateFlow(load())

    /** The user-added packages per app id; apps without any are absent. */
    override val packages: StateFlow<Map<String, List<String>>> = _packages.asStateFlow()

    /** Add [packageName] to [appId] (after its existing ones); no change when the app already has it. */
    @Synchronized
    override fun add(appId: String, packageName: String) {
        val current = _packages.value[appId].orEmpty()
        if (packageName in current) return
        save(_packages.value + (appId to current + packageName))
    }

    /** Remove [packageName] from [appId]; no change when the app doesn't have it. */
    @Synchronized
    override fun remove(appId: String, packageName: String) {
        val current = _packages.value[appId] ?: return
        if (packageName !in current) return
        val remaining = current - packageName
        save(if (remaining.isEmpty()) _packages.value - appId else _packages.value + (appId to remaining))
    }

    private fun save(value: Map<String, List<String>>) {
        prefs.edit { putString(KEY_PACKAGES, json.encodeToString(serializer, value)) }
        _packages.value = value
    }

    private fun load(): Map<String, List<String>> {
        val stored = prefs.getString(KEY_PACKAGES, null) ?: return emptyMap()
        return try {
            json.decodeFromString(serializer, stored).filterValues { it.isNotEmpty() }
        } catch (e: SerializationException) {
            Timber.w(e, "Ignoring unreadable user-added package names")
            emptyMap()
        } catch (e: IllegalArgumentException) {
            Timber.w(e, "Ignoring unreadable user-added package names")
            emptyMap()
        }
    }

    companion object {
        private const val PREFS_NAME = "pebblentn_custom_packages"
        private const val KEY_PACKAGES = "packages_by_app"

        private val json = Json
        private val serializer = MapSerializer(String.serializer(), ListSerializer(String.serializer()))
    }
}
