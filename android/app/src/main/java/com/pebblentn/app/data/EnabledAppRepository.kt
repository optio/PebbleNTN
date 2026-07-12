package com.pebblentn.app.data

import com.pebblentn.app.catalog.NavigationAppCatalog
import com.pebblentn.app.core.AppAllowlist
import com.pebblentn.app.core.EpochClock
import com.pebblentn.app.data.db.AppEnablementDao
import com.pebblentn.app.data.db.SupportedAppSettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Owns which navigation apps are enabled, and answers the allowlist question for the listener.
 *
 * The listener needs a synchronous, non-blocking [isEnabled] on the callback thread, so this
 * repository keeps a `@Volatile` cache of enabled package names and reads only that cache in
 * [isEnabled] — never the database (REQ-ANDROID-003, REQ-ANDROID-006). The cache is refreshed
 * whenever enablement changes; callers must [refreshCache] (or [syncInstalledApps]) at least once
 * on startup before relying on the gate.
 */
class EnabledAppRepository(
    private val dao: AppEnablementDao,
    private val catalog: NavigationAppCatalog,
    private val clock: EpochClock = EpochClock.SYSTEM,
) : AppAllowlist {

    @Volatile
    private var enabledPackages: Set<String> = emptySet()

    override fun isEnabled(packageName: String): Boolean = packageName in enabledPackages

    /** Snapshot of the currently enabled package names (for diagnostics/tests). */
    val enabledPackageSnapshot: Set<String> get() = enabledPackages

    /**
     * Ensure every installed catalog app has a settings row, defaulting to its catalog
     * `defaultEnabled` on first discovery (REQ-ANDROID-004). Existing rows are left untouched so a
     * user's later choice is never overwritten. Non-installed apps get no row.
     */
    suspend fun syncInstalledApps(installedPackages: Set<String>) {
        val now = clock.nowMillis()
        for (entry in catalog.apps) {
            if (entry.packageNames.any { it in installedPackages }) {
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

    /** Stored, enablement for catalog apps that have been discovered, for the settings UI. */
    fun observeEnablement(): Flow<List<AppEnablement>> =
        dao.observeAll().map { rows ->
            val byAppId = rows.associateBy { it.appId }
            catalog.apps.mapNotNull { entry ->
                val row = byAppId[entry.appId] ?: return@mapNotNull null
                AppEnablement(
                    appId = entry.appId,
                    displayName = entry.displayName,
                    packageNames = entry.packageNames,
                    enabled = row.enabled,
                    captureOnly = entry.captureOnly,
                )
            }
        }
}
