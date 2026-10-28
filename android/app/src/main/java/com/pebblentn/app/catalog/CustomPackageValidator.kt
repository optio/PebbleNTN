package com.pebblentn.app.catalog

/** The outcome of checking a package name the user wants to add to an app (FR5). */
sealed interface PackageCheck {

    /** Accepted: [packageName] is the trimmed name to store, its case kept. */
    data class Valid(val packageName: String) : PackageCheck

    /**
     * Rejected for [reason]. [ownerAppId] is the app that already uses the name, for
     * [Reason.BUILT_IN_ELSEWHERE] and [Reason.ADDED_ELSEWHERE].
     */
    data class Invalid(val reason: Reason, val ownerAppId: String? = null) : PackageCheck

    enum class Reason {
        /** Nothing but whitespace. */
        EMPTY,

        /** Longer than [CustomPackageValidator.MAX_LENGTH]. */
        TOO_LONG,

        /** Not an Android package name. */
        FORMAT,

        /** Already one of this app's built-in packages. */
        BUILT_IN_HERE,

        /** A built-in package of another app. */
        BUILT_IN_ELSEWHERE,

        /** Already added to this app. */
        ADDED_HERE,

        /** Already added to another app. */
        ADDED_ELSEWHERE,
    }
}

/**
 * Checks a package name before it is added to an app (FR5): trimmed, an Android package name (at
 * least two dot-separated segments, each a letter followed by letters, digits or underscores), and
 * used by no other package of PebbleNTN, built in or user-added, so one name always means one app.
 * Android package names are case-sensitive (`com.baidu.BaiduMap`), so the name is stored exactly as
 * typed; the duplicate check ignores case, so two names differing only in case never coexist. Pure.
 */
object CustomPackageValidator {

    /** Longer names are rejected; Android's own limit is about this size. */
    const val MAX_LENGTH = 255

    private val FORMAT = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")

    /** True when [packageName] is already in the stored form: trimmed and well formed. */
    fun isWellFormed(packageName: String): Boolean =
        packageName.length <= MAX_LENGTH && FORMAT.matches(packageName)

    /**
     * Check [input] for [appId].
     *
     * @param builtInOwners every bundled package mapped to the app id that owns it.
     * @param userAdded the user-added packages per app id.
     */
    fun check(
        input: String,
        appId: String,
        builtInOwners: Map<String, String>,
        userAdded: Map<String, List<String>>,
    ): PackageCheck {
        val name = input.trim()
        val invalid = when {
            name.isEmpty() -> PackageCheck.Invalid(PackageCheck.Reason.EMPTY)
            name.length > MAX_LENGTH -> PackageCheck.Invalid(PackageCheck.Reason.TOO_LONG)
            !FORMAT.matches(name) -> PackageCheck.Invalid(PackageCheck.Reason.FORMAT)
            else -> duplicate(name, appId, builtInOwners, userAdded)
        }
        return invalid ?: PackageCheck.Valid(name)
    }

    private fun duplicate(
        name: String,
        appId: String,
        builtInOwners: Map<String, String>,
        userAdded: Map<String, List<String>>,
    ): PackageCheck.Invalid? {
        builtInOwners.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.let { (_, owner) ->
            return if (owner == appId) {
                PackageCheck.Invalid(PackageCheck.Reason.BUILT_IN_HERE)
            } else {
                PackageCheck.Invalid(PackageCheck.Reason.BUILT_IN_ELSEWHERE, owner)
            }
        }
        userAdded.entries.firstOrNull { (_, packages) -> packages.any { it.equals(name, ignoreCase = true) } }?.let { (owner, _) ->
            return if (owner == appId) {
                PackageCheck.Invalid(PackageCheck.Reason.ADDED_HERE)
            } else {
                PackageCheck.Invalid(PackageCheck.Reason.ADDED_ELSEWHERE, owner)
            }
        }
        return null
    }
}
