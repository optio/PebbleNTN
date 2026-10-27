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
 * @property hasOfficialRules true when bundled/official parsing rules exist for this app (its own,
 *   or those of [rulesFrom]).
 * @property captureAvailable true when the app may be captured in debug/capture-only mode.
 * @property defaultEnabled whether an installed instance is enabled on first discovery.
 * @property channelHints optional notification channel ids that carry navigation updates.
 * @property rulesFrom the [appId] of the entry whose rules this app uses (#113): both are one rule
 *   family, and every rule of the family applies to all of its packages. Null for an app with its
 *   own rules (or none).
 * @property website an https page where the app is obtained, linked instead of Google Play.
 * @property playListed false hides the app on Google Play installs (see
 *   [NavigationAppCatalog.forGooglePlay]).
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
    val rulesFrom: String? = null,
    val website: String? = null,
    val playListed: Boolean = true,
) {
    /** True when the app can only be captured (no official watch output is claimed). */
    val captureOnly: Boolean get() = !hasOfficialRules

    /** The app whose rules apply to this one: [rulesFrom], or this app itself. */
    val rulesAppId: String get() = rulesFrom ?: appId
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

    /** Which packages share their rules ([NavigationAppEntry.rulesFrom]); see [RuleFamilies]. */
    fun ruleFamilies(): RuleFamilies = RuleFamilies.of(
        apps.groupBy { it.rulesAppId }.values.map { group -> group.flatMapTo(linkedSetOf()) { it.packageNames } },
    )

    /**
     * The catalog as a Google Play install uses it (#113, REQ-ANDROID-022): an app with
     * `playListed: false` is not an entry of its own, so it is never listed and its [website] is never
     * linked. Its packages join the entry whose rules it uses, after that entry's own (the first
     * package keeps its meaning), so they follow that app's switch and keep its rules. An unlisted
     * app without [NavigationAppEntry.rulesFrom] is left out entirely. Every remaining link goes to
     * Google Play.
     */
    fun forGooglePlay(): NavigationAppCatalog {
        val unlisted = apps.filterNot { it.playListed }
        val joining = unlisted.filter { it.rulesFrom != null }.groupBy { it.rulesFrom!! }
        return copy(
            apps = apps.filter { it.playListed }.map { entry ->
                entry.copy(
                    packageNames = entry.packageNames + joining[entry.appId].orEmpty().flatMap { it.packageNames },
                    website = null,
                )
            },
        )
    }

    companion object {
        const val SUPPORTED_SCHEMA_VERSION = 1

        private val json = Json { ignoreUnknownKeys = false }

        /**
         * Parse and validate a catalog document.
         *
         * @throws IllegalArgumentException if the schema version is unsupported, app ids or package
         *   names are duplicated (a package must map to exactly one app), a `rulesFrom` does not name
         *   another entry with rules of its own, or a `website` is not https.
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
            val byId = catalog.apps.associateBy { it.appId }
            for (entry in catalog.apps) {
                entry.rulesFrom?.let { validateRulesFrom(entry.appId, byId[it], it) }
                entry.website?.let { site ->
                    require(site.startsWith("https://") && site.none(Char::isWhitespace)) {
                        "${entry.appId}: website must be an https URL"
                    }
                }
            }
            return catalog
        }

        private fun validateRulesFrom(appId: String, target: NavigationAppEntry?, source: String) {
            require(source != appId) { "$appId: rulesFrom names itself" }
            require(target != null) { "$appId: rulesFrom names unknown app $source" }
            require(target.rulesFrom == null) { "$appId: rulesFrom $source uses another app's rules itself" }
            require(target.hasOfficialRules) { "$appId: rulesFrom $source has no rules of its own" }
        }
    }
}

/**
 * Rule families (#113): the packages that share one app's rules — the app's own packages plus those of
 * every entry whose [NavigationAppEntry.rulesFrom] names it. A rule applies to a notification when the
 * notification's package is in the same family as any package the rule lists, for bundled, downloaded
 * and user rules alike. A package outside the catalog is a family of its own, so it matches exactly as
 * before. Pure and immutable.
 */
class RuleFamilies private constructor(private val familyByPackage: Map<String, Set<String>>) {

    /** Every package sharing rules with [packageName], itself included. */
    fun familyOf(packageName: String): Set<String> = familyByPackage[packageName] ?: setOf(packageName)

    /** True when a rule listing [rulePackages] applies to a notification posted by [packageName]. */
    fun applies(packageName: String, rulePackages: Collection<String>): Boolean =
        packageName in rulePackages || familyOf(packageName).any { it in rulePackages }

    companion object {
        /** No families: every package matches only the rules that list it (tests and tools). */
        val NONE = RuleFamilies(emptyMap())

        /** Families from disjoint package groups; a package in two groups is a catalog error. */
        fun of(groups: Collection<Set<String>>): RuleFamilies {
            val map = HashMap<String, Set<String>>()
            for (group in groups) {
                for (pkg in group) {
                    require(map.put(pkg, group) == null) { "$pkg is in two rule families" }
                }
            }
            return RuleFamilies(map)
        }
    }
}

/**
 * Supported apps that aren't installed, for the navigation-apps screen (#28): apps with directions
 * first, then by name. An uninstalled app is listed like any other (#88). PebbleNTN's own debug
 * fixture app is never offered.
 */
fun supportedNotInstalled(catalog: NavigationAppCatalog, installedAppIds: Set<String>): List<NavigationAppEntry> =
    catalog.apps
        .filter { it.appId !in installedAppIds }
        .filterNot { entry -> entry.packageNames.all { it.startsWith("com.pebblentn.") } }
        .sortedWith(compareByDescending<NavigationAppEntry> { it.hasOfficialRules }.thenBy { it.displayName.lowercase() })
