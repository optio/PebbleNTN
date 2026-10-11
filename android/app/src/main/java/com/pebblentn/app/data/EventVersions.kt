package com.pebblentn.app.data

import android.content.Context

/**
 * The versions recorded with each debug event (#84): which PebbleNTN build and which navigation-app
 * build produced it, and which rulesets evaluated it. A shared log can then be tied to the exact
 * app versions it came from, even when the history spans app updates.
 */
data class EventVersions(
    val appVersionName: String? = null,
    val appVersionCode: Long? = null,
    val sourceAppVersionName: String? = null,
    val sourceAppVersionCode: Long? = null,
    /** The bundled rulesets that hold rules for the package, comma-separated, or null when none. */
    val rulesetVersions: String? = null,
)

/** Supplies [EventVersions] for a navigation app's package. */
fun interface EventVersionsProvider {
    fun forPackage(packageName: String): EventVersions

    companion object {
        val NONE = EventVersionsProvider { EventVersions() }
    }
}

/**
 * The device implementation. The navigation app's version comes from the package manager (the
 * package is visible through the manifest's `<queries>`); an app that is not visible has no version.
 */
class SystemEventVersionsProvider(
    private val context: Context,
    private val appVersionName: String,
    private val appVersionCode: Long,
    private val rulesetVersions: (String) -> List<String>,
) : EventVersionsProvider {

    override fun forPackage(packageName: String): EventVersions {
        val info = runCatching { context.packageManager.getPackageInfo(packageName, 0) }.getOrNull()
        return EventVersions(
            appVersionName = appVersionName,
            appVersionCode = appVersionCode,
            sourceAppVersionName = info?.versionName,
            sourceAppVersionCode = info?.longVersionCode,
            rulesetVersions = rulesetVersions(packageName).takeIf { it.isNotEmpty() }?.joinToString(","),
        )
    }
}
