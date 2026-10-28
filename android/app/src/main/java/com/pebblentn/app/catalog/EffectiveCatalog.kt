package com.pebblentn.app.catalog

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Where the user-added package names are kept ([com.pebblentn.app.data.CustomPackagesRepository]). */
interface UserPackageStore {
    /** The user-added packages per app id, in the order they were added. */
    val packages: StateFlow<Map<String, List<String>>>

    fun add(appId: String, packageName: String)

    fun remove(appId: String, packageName: String)
}

/**
 * The navigation-app catalog this install actually uses (FR6): the bundled catalog with each app's
 * user-added packages appended after its built-in ones, then, on a Google Play install, its Play view
 * ([NavigationAppCatalog.forGooglePlay]). It feeds the rule families, enablement and the listener
 * allowlist, the installed-app discovery and the debug labels, and it changes as soon as the user adds
 * or removes a package: [catalog] emits the new catalog and [families] answers with it, no restart.
 *
 * @param bundled the catalog as bundled, before the Play view.
 * @param googlePlay true on a Google Play install.
 */
class EffectiveCatalog(
    private val bundled: NavigationAppCatalog,
    private val googlePlay: Boolean,
    private val store: UserPackageStore,
) {

    /** The built-in catalog as this install shows it, without user-added packages. */
    val builtIn: NavigationAppCatalog = view(bundled)

    /** Every bundled package, mapped to the app that shows it here (on Play, a joined app's package is its target's). */
    private val builtInOwners: Map<String, String> =
        bundled.apps.flatMap { entry -> entry.packageNames.map { it to entry.appId } }.toMap() +
            builtIn.apps.flatMap { entry -> entry.packageNames.map { it to entry.appId } }.toMap()

    private val _catalog = MutableStateFlow(build(store.packages.value))

    @Volatile
    private var currentFamilies: RuleFamilies = _catalog.value.ruleFamilies()

    /** The effective catalog; a new value after every [add] or [remove]. */
    val catalog: StateFlow<NavigationAppCatalog> = _catalog.asStateFlow()

    /** The current effective catalog. */
    val current: NavigationAppCatalog get() = _catalog.value

    /** The rule families of the current effective catalog, always up to date. */
    val families: RuleFamilies = RuleFamilies.live { currentFamilies }

    /** The built-in packages of [appId] as this install uses them, or empty for an unknown app. */
    fun builtInPackagesOf(appId: String): List<String> =
        builtIn.apps.firstOrNull { it.appId == appId }?.packageNames.orEmpty()

    /** The packages the user added to [appId] that are in use (a now built-in one is not). */
    fun userAddedPackagesOf(appId: String): List<String> = current.userAddedByApp()[appId].orEmpty()

    /** The display name of [appId] here, or the id itself when the catalog has no such app. */
    fun displayNameOf(appId: String): String =
        (builtIn.apps.firstOrNull { it.appId == appId } ?: bundled.apps.firstOrNull { it.appId == appId })?.displayName ?: appId

    /** Check [input] for [appId] (FR5) and, when valid, store it and rebuild the catalog. */
    @Synchronized
    fun add(appId: String, input: String): PackageCheck {
        val check = CustomPackageValidator.check(input, appId, builtInOwners, store.packages.value)
        if (check is PackageCheck.Valid) {
            store.add(appId, check.packageName)
            rebuild()
        }
        return check
    }

    /** Remove the user-added [packageName] from [appId] and rebuild the catalog. */
    @Synchronized
    fun remove(appId: String, packageName: String) {
        store.remove(appId, packageName)
        rebuild()
    }

    private fun rebuild() {
        val next = build(store.packages.value)
        // Families first, so whoever reacts to the new catalog already matches with it.
        currentFamilies = next.ruleFamilies()
        _catalog.value = next
    }

    private fun build(userAdded: Map<String, List<String>>): NavigationAppCatalog =
        view(bundled.withUserAddedPackages(userAdded))

    private fun view(catalog: NavigationAppCatalog): NavigationAppCatalog =
        if (googlePlay) catalog.forGooglePlay() else catalog
}
