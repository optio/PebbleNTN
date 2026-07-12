package com.pebblentn.app.catalog

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * One bundled navigation-app catalog entry (spec/200-architecture/Android.md → "Supported-app
 * catalog"). Declarative data only; the schema is `schemas/app-catalog.schema.json`.
 *
 * @property appId stable, kebab-case identifier used as the settings primary key.
 * @property displayName user-facing name.
 * @property packageNames Android package(s) this app publishes notifications under.
 * @property hasOfficialRules true when bundled/official parsing rules exist for this app.
 * @property captureAvailable true when the app may be captured in debug/capture-only mode.
 * @property defaultEnabled whether an installed instance is enabled on first discovery.
 * @property channelHints optional notification channel ids that carry navigation updates.
 */
@kotlinx.serialization.Serializable
data class NavigationAppEntry(
    val appId: String,
    val displayName: String,
    val packageNames: List<String>,
    val hasOfficialRules: Boolean,
    val captureAvailable: Boolean,
    val defaultEnabled: Boolean,
    val channelHints: List<String> = emptyList(),
) {
    /** True when the app can only be captured (no official watch output is claimed). */
    val captureOnly: Boolean get() = !hasOfficialRules
}

/** The parsed navigation-app catalog. */
@kotlinx.serialization.Serializable
data class NavigationAppCatalog(
    val schemaVersion: Int,
    val apps: List<NavigationAppEntry>,
) {
    /** The catalog entry that publishes [packageName], or null if the package is not in the catalog. */
    fun entryForPackage(packageName: String): NavigationAppEntry? =
        apps.firstOrNull { packageName in it.packageNames }

    /** Every package name declared by any catalog entry. */
    val allPackageNames: Set<String> get() = apps.flatMapTo(mutableSetOf()) { it.packageNames }

    companion object {
        const val SUPPORTED_SCHEMA_VERSION = 1

        private val json = Json { ignoreUnknownKeys = false }

        /**
         * Parse and validate a catalog document.
         *
         * @throws IllegalArgumentException if the schema version is unsupported, or app ids or
         *   package names are duplicated (a package must map to exactly one app).
         * @throws SerializationException if [text] is not valid catalog JSON.
         */
        fun parse(text: String): NavigationAppCatalog {
            val catalog = json.decodeFromString<NavigationAppCatalog>(text)
            require(catalog.schemaVersion == SUPPORTED_SCHEMA_VERSION) {
                "unsupported catalog schemaVersion ${catalog.schemaVersion}"
            }
            val ids = catalog.apps.map { it.appId }
            require(ids.size == ids.toSet().size) { "duplicate appId in catalog" }
            val packages = catalog.apps.flatMap { it.packageNames }
            require(packages.size == packages.toSet().size) {
                "a package name is claimed by more than one catalog app"
            }
            return catalog
        }
    }
}
