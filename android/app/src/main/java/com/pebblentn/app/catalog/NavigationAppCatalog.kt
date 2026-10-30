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
 *   [NavigationAppCatalog.forGooglePlay]): with [rulesFrom] it joins that app, without it stays
 *   working but hidden (#120).
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

/**
 * The parsed navigation-app catalog.
 *
 * @property joinedRoots set by [forGooglePlay] only, never read from JSON: each package that joined
 *   another entry there (an unlisted app with [NavigationAppEntry.rulesFrom]) mapped to that entry's
 *   first package, so the rule families keep its root package ([RuleFamilies.resourceFallbackFor]).
 * @property userAddedPackages set by [withUserAddedPackages] only, never read from JSON: each
 *   user-added package mapped to the [NavigationAppEntry.appId] the user added it to.
 * @property hiddenAppIds set by [forGooglePlay] only, never read from JSON: the entries kept for
 *   matching, enablement and the listener allowlist but never shown (#120: an unlisted app without
 *   [NavigationAppEntry.rulesFrom], such as Vela Maps). Everything the user sees reads [visibleApps].
 */
@kotlinx.serialization.Serializable
data class NavigationAppCatalog(
    val schemaVersion: Int,
    val apps: List<NavigationAppEntry>,
    @kotlinx.serialization.Transient val joinedRoots: Map<String, String> = emptyMap(),
    @kotlinx.serialization.Transient val userAddedPackages: Map<String, String> = emptyMap(),
    @kotlinx.serialization.Transient val hiddenAppIds: Set<String> = emptySet(),
) {
    /**
     * The entries the user sees (#120): [apps] without the [hiddenAppIds]. Every list, link and count
     * in the UI reads this; matching, enablement and the allowlist read [apps].
     */
    val visibleApps: List<NavigationAppEntry> get() = apps.filterNot { it.appId in hiddenAppIds }

    /** False for an entry kept on Google Play only so its notifications keep working (#120). */
    fun isVisible(appId: String): Boolean = appId !in hiddenAppIds

    /** The catalog entry that publishes [packageName], or null if the package is not in the catalog. */
    fun entryForPackage(packageName: String): NavigationAppEntry? =
        apps.firstOrNull { packageName in it.packageNames }

    /** Every package name declared by any catalog entry (user-added ones included). */
    val allPackageNames: Set<String> get() = apps.flatMapTo(mutableSetOf()) { it.packageNames }

    /** True when the user added [packageName] (it is not a bundled catalog package). */
    fun isUserAdded(packageName: String): Boolean = packageName in userAddedPackages

    /** The user-added packages per app id, in the order they were added; apps without any are absent. */
    fun userAddedByApp(): Map<String, List<String>> =
        userAddedPackages.entries.groupBy({ it.value }, { it.key })

    /**
     * Which packages share their rules ([NavigationAppEntry.rulesFrom]); see [RuleFamilies]. Each
     * package of an entry with `rulesFrom` records the root package of its family: the first package
     * of the entry it takes its rules from (on Google Play, of the entry it joined, [joinedRoots]).
     * A user-added package of any other entry records that entry's first package. The root never
     * depends on the catalog order.
     */
    fun ruleFamilies(): RuleFamilies {
        val byId = apps.associateBy { it.appId }
        val roots = apps.filter { it.rulesFrom != null }.flatMap { entry ->
            val root = byId[entry.rulesFrom]?.packageNames?.firstOrNull() ?: return@flatMap emptyList()
            entry.packageNames.map { it to root }
        }.toMap()
        val userAddedRoots = userAddedPackages.keys.mapNotNull { pkg ->
            entryForPackage(pkg)?.packageNames?.firstOrNull()?.let { pkg to it }
        }.toMap()
        return RuleFamilies.of(
            apps.groupBy { it.rulesAppId }.values.map { group -> group.flatMapTo(linkedSetOf()) { it.packageNames } },
            roots = userAddedRoots + roots + joinedRoots,
        )
    }

    /**
     * This catalog with the user's own package names (FR6): each app's packages from [byAppId] are
     * appended after its built-in ones, so the first package keeps its meaning and they join its
     * family, switch and labels. A package already claimed (built in, or added earlier to any app), a
     * malformed one, or one for an unknown app is skipped, so a catalog update that makes a user-added
     * package built in can never break the catalog's one-package-one-app invariant.
     */
    fun withUserAddedPackages(byAppId: Map<String, List<String>>): NavigationAppCatalog {
        if (byAppId.isEmpty()) return this
        // Stored as typed (package names are case-sensitive), but claimed case-insensitively, as the
        // validator checks duplicates.
        val claimed = allPackageNames.mapTo(mutableSetOf()) { it.lowercase() }
        val added = LinkedHashMap<String, String>()
        val updated = apps.map { entry ->
            val extra = byAppId[entry.appId].orEmpty().filter { pkg ->
                CustomPackageValidator.isWellFormed(pkg) && claimed.add(pkg.lowercase())
            }
            extra.forEach { added[it] = entry.appId }
            if (extra.isEmpty()) entry else entry.copy(packageNames = entry.packageNames + extra)
        }
        return copy(apps = updated, userAddedPackages = userAddedPackages + added)
    }

    /**
     * The catalog as a Google Play install uses it (#113, REQ-ANDROID-022): an app with
     * `playListed: false` is not an entry of its own, so it is never listed and its [website] is never
     * linked. Its packages join the entry whose rules it uses, after that entry's own (the first
     * package keeps its meaning), so they follow that app's switch and keep its rules. An unlisted
     * app without [NavigationAppEntry.rulesFrom] (Vela Maps, #120) stays an entry of its own, with
     * its own rules, switch (on by default) and allowlist place, but in [hiddenAppIds], so it is
     * never shown. No website is linked on Play. A joined package keeps its family root, the entry's
     * first package ([joinedRoots]).
     */
    fun forGooglePlay(): NavigationAppCatalog {
        val unlisted = apps.filterNot { it.playListed }
        val joining = unlisted.filter { it.rulesFrom != null }.groupBy { it.rulesFrom!! }
        val hidden = unlisted.filter { it.rulesFrom == null }.mapTo(linkedSetOf()) { it.appId }
        val listedById = apps.filter { it.playListed }.associateBy { it.appId }
        val roots = joining.flatMap { (targetId, joined) ->
            val root = listedById[targetId]?.packageNames?.firstOrNull() ?: return@flatMap emptyList()
            joined.flatMap { it.packageNames }.map { it to root }
        }.toMap()
        val playApps = apps.filter { it.playListed || it.appId in hidden }.map { entry ->
            entry.copy(
                packageNames = entry.packageNames + joining[entry.appId].orEmpty().flatMap { it.packageNames },
                website = null,
            )
        }
        val kept = playApps.flatMapTo(mutableSetOf()) { it.packageNames }
        return copy(
            joinedRoots = joinedRoots + roots,
            apps = playApps,
            // A user-added package of an app left out entirely is left out with it.
            userAddedPackages = userAddedPackages.filterKeys { it in kept },
            hiddenAppIds = hiddenAppIds + hidden,
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
 * before. The families built by [of] are pure and immutable; [live] follows a changing catalog.
 *
 * A package that takes another app's rules also records the family's root package, the first package
 * of that app, which [resourceFallbackFor] exposes for the icon lookup (#113 follow-up). So does a
 * user-added package: its root is its app's first built-in package.
 */
interface RuleFamilies {

    /** Every package sharing rules with [packageName], itself included. */
    fun familyOf(packageName: String): Set<String>

    /** True when a rule listing [rulePackages] applies to a notification posted by [packageName]. */
    fun applies(packageName: String, rulePackages: Collection<String>): Boolean =
        packageName in rulePackages || familyOf(packageName).any { it in rulePackages }

    /**
     * The package name to retry a drawable lookup under when [packageName] (an app using another
     * app's rules, or a user-added package) has none of its own: the root package of its family. Null
     * for every other package, the root itself included.
     */
    fun resourceFallbackFor(packageName: String): String?

    companion object {
        /** No families: every package matches only the rules that list it (tests and tools). */
        val NONE: RuleFamilies = of(emptyList())

        /**
         * Families from disjoint package groups; a package in two groups is a catalog error. [roots]
         * maps a package to its family's root package; both must be in the same group.
         */
        fun of(groups: Collection<Set<String>>, roots: Map<String, String> = emptyMap()): RuleFamilies {
            val map = HashMap<String, Set<String>>()
            for (group in groups) {
                for (pkg in group) {
                    require(map.put(pkg, group) == null) { "$pkg is in two rule families" }
                }
            }
            for ((pkg, root) in roots) {
                require(map[pkg]?.contains(root) == true) { "$pkg: root package $root is not in its rule family" }
            }
            return Fixed(map, roots.toMap())
        }

        /**
         * A view that always answers with [current]'s families, for components built once that must
         * follow the user-added packages without a restart ([EffectiveCatalog]).
         */
        fun live(current: () -> RuleFamilies): RuleFamilies = object : RuleFamilies {
            override fun familyOf(packageName: String): Set<String> = current().familyOf(packageName)
            override fun applies(packageName: String, rulePackages: Collection<String>): Boolean =
                current().applies(packageName, rulePackages)
            override fun resourceFallbackFor(packageName: String): String? = current().resourceFallbackFor(packageName)
        }
    }

    /** Immutable families built by [of]. */
    private class Fixed(
        private val familyByPackage: Map<String, Set<String>>,
        private val rootByPackage: Map<String, String>,
    ) : RuleFamilies {
        override fun familyOf(packageName: String): Set<String> = familyByPackage[packageName] ?: setOf(packageName)
        override fun resourceFallbackFor(packageName: String): String? =
            rootByPackage[packageName]?.takeIf { it != packageName }
    }
}

/**
 * Supported apps that aren't installed, for the navigation-apps screen (#28): apps with directions
 * first, then by name. An uninstalled app is listed like any other (#88). PebbleNTN's own debug
 * fixture app is never offered, nor an app hidden on Google Play (#120).
 */
fun supportedNotInstalled(catalog: NavigationAppCatalog, installedAppIds: Set<String>): List<NavigationAppEntry> =
    catalog.visibleApps
        .filter { it.appId !in installedAppIds }
        .filterNot { entry -> entry.packageNames.all { it.startsWith("com.pebblentn.") } }
        .sortedWith(compareByDescending<NavigationAppEntry> { it.hasOfficialRules }.thenBy { it.displayName.lowercase() })
