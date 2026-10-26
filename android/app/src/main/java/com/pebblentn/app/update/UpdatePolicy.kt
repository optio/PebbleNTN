package com.pebblentn.app.update

/**
 * What the update features do for an install source (REQ-ANDROID-020, REQ-ANDROID-022). Pure, so
 * the rules are JVM-tested apart from the repository and the screens that apply them.
 *
 * A Google Play install gets its updates from Google Play only (Play's Device and Network Abuse
 * policy): it never asks GitHub for a newer version, ignores the GitHub data an earlier GitHub
 * install stored, and its update card offers no other source. The offline watchapp-version
 * comparison (REQ-ANDROID-019) stays for every source.
 */
data class UpdatePolicy(
    /** The GitHub check may run: on demand, from the stale item, or weekly when turned on. */
    val allowsGithubCheck: Boolean,
    /** The GitHub version, weekly setting and check time stored by an earlier install are ignored. */
    val ignoresStoredGithubVersion: Boolean,
    /** The update card lists the other sources under "Other sources". */
    val offersOtherSources: Boolean,
    /** The update card says the source can take a while to offer a new version. */
    val showsInstallDelayNote: Boolean,
) {
    companion object {
        /** The policy for an app installed from [source]. */
        fun forSource(source: InstallSource): UpdatePolicy = when (source) {
            InstallSource.PLAY -> STORE_ONLY
            InstallSource.FDROID -> SELF_UPDATING.copy(showsInstallDelayNote = true)
            InstallSource.GITHUB, InstallSource.OBTAINIUM -> SELF_UPDATING
        }

        private val SELF_UPDATING = UpdatePolicy(
            allowsGithubCheck = true,
            ignoresStoredGithubVersion = false,
            offersOtherSources = true,
            showsInstallDelayNote = false,
        )

        private val STORE_ONLY = UpdatePolicy(
            allowsGithubCheck = false,
            ignoresStoredGithubVersion = true,
            offersOtherSources = false,
            showsInstallDelayNote = true,
        )
    }
}
