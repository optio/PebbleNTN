package com.pebblentn.app.data

import com.pebblentn.app.catalog.NavigationAppCatalog
import com.pebblentn.app.catalog.NavigationAppEntry
import com.pebblentn.app.core.AppAllowlist
import com.pebblentn.app.core.EpochClock
import com.pebblentn.app.data.db.AppEnablementDao
import com.pebblentn.app.data.db.SupportedAppSettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

/**
 * Owns which navigation apps are enabled, and answers the allowlist question for the listener.
 *
 * The listener needs a synchronous, non-blocking [isEnabled] on the callback thread, so this
 * repository keeps a `@Volatile` cache of enabled package names and reads only that cache in
 * [isEnabled] — never the database (REQ-ANDROID-003, REQ-ANDROID-006). The cache is refreshed
 * whenever enablement changes; callers must [refreshCache] (or [syncInstalledApps]) at least once
 * on startup before relying on the gate.
 *
 * Enablement follows the catalog it is given: on a Google Play install that is
 * [NavigationAppCatalog.forGooglePlay], where MicroG Maps' package belongs to the Google Maps entry,
 * so the Google Maps switch gates it (#113). The catalog may change at runtime (user-added packages,
 * [com.pebblentn.app.catalog.EffectiveCatalog]): every call reads its current value, and the owner
 * re-runs [syncInstalledApps] when it changes. An app with a user-added package counts as installed:
 * Android may not let PebbleNTN see that package, and the user said it is there.
 */
class EnabledAppRepository(
    private val dao: AppEnablementDao,
    private val catalogs: StateFlow<NavigationAppCatalog>,
    private val clock: EpochClock = EpochClock.SYSTEM,
) : AppAllowlist {

    /** A repository over a fixed [catalog]. */
    constructor(dao: AppEnablementDao, catalog: NavigationAppCatalog, clock: EpochClock = EpochClock.SYSTEM) :
        this(dao, MutableStateFlow(catalog), clock)

    private val catalog: NavigationAppCatalog get() = catalogs.value

    private fun NavigationAppCatalog.hasUserAdded(entry: NavigationAppEntry): Boolean =
        entry.packageNames.any(::isUserAdded)

    @Volatile
    private var enabledPackages: Set<String> = emptySet()

    override fun isEnabled(packageName: String): Boolean = packageName in enabledPackages

    /**
     * The catalog packages installed at the last [syncInstalledApps]; null before the first one. A
     * row outlives its app on purpose (the user's choice returns with a reinstall); the settings UI
     * lists such an app as no longer installed.
     */
    private val installedPackages = MutableStateFlow<Set<String>?>(null)

    /** Snapshot of the currently enabled package names (for diagnostics/tests). */
    val enabledPackageSnapshot: Set<String> get() = enabledPackages

    /**
     * Ensure every installed catalog app has a settings row, defaulting to its catalog
     * `defaultEnabled` on first discovery (REQ-ANDROID-004). Existing rows are left untouched so a
     * user's later choice is never overwritten. Non-installed apps get no row, and an uninstalled
     * app keeps its row and is reported with `installed = false`.
     */
    suspend fun syncInstalledApps(installedPackages: Set<String>) {
        this.installedPackages.value = installedPackages
        val now = clock.nowMillis()
        val catalog = catalog
        for (entry in catalog.apps) {
            if (entry.packageNames.any { it in installedPackages } || catalog.hasUserAdded(entry)) {
                dao.insertIfAbsent(
                    SupportedAppSettingsEntity(
                        appId = entry.appId,
                        packageName = entry.packageNames.first(),
                        enabled = entry.defaultEnabled,
                        captureUnmatched = false,
                        firstSeenAt = now,
                        updatedAt = now,
                    ),
                )
            }
        }
        refreshCache()
    }

    /** Enable or disable an app by catalog id, then refresh the allowlist cache. */
    suspend fun setEnabled(appId: String, enabled: Boolean) {
        dao.setEnabled(appId, enabled, clock.nowMillis())
        refreshCache()
    }

    /** Rebuild the in-memory allowlist cache from the database + catalog mapping. */
    suspend fun refreshCache() {
        val rowsByAppId = dao.getAll().associateBy { it.appId }
        enabledPackages = catalog.apps
            .filter { rowsByAppId[it.appId]?.enabled == true }
            .flatMap { it.packageNames }
            .toSet()
    }

    /**
     * Stored enablement of the discovered catalog apps, each marked installed or not, for the
     * settings UI. Before the first sync every app counts as installed, as nothing is known yet.
     */
    fun observeEnablement(): Flow<List<AppEnablement>> =
        combine(dao.observeAll(), installedPackages, catalogs) { rows, installed, catalog ->
            val byAppId = rows.associateBy { it.appId }
            catalog.apps.mapNotNull { entry ->
                val row = byAppId[entry.appId] ?: return@mapNotNull null
                AppEnablement(
                    appId = entry.appId,
                    displayName = entry.displayName,
                    packageNames = entry.packageNames,
                    enabled = row.enabled,
                    captureOnly = entry.captureOnly,
                    installed = installed == null || entry.packageNames.any { it in installed } || catalog.hasUserAdded(entry),
                    rulesAppId = entry.rulesAppId,
                )
            }
        }
}
