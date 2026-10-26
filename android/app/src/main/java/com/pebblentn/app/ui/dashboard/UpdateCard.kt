package com.pebblentn.app.ui.dashboard

import com.pebblentn.app.update.InstallSource
import com.pebblentn.app.update.UpdatePolicy

/**
 * What the update card offers (REQ-ANDROID-020, REQ-ANDROID-022): the main "Update from {source}"
 * button for the detected [main] source, the [others] listed under "Other sources" (none on a
 * Google Play install, which offers no update outside Play), which delay line to add, and whether
 * the body tells the user to uninstall first when the download will not install over the app (never
 * on Play, which installs the update itself). Pure, so the choice is unit-tested apart from the
 * Compose card.
 */
data class UpdateCardModel(
    val main: InstallSource,
    val others: List<InstallSource>,
    val showFdroidDelay: Boolean,
    val showPlayDelay: Boolean = false,
    val showUninstallHint: Boolean = true,
) {
    /** "Other sources" is shown only when there is a source to list. */
    val offersOtherSources: Boolean get() = others.isNotEmpty()
}

/** Every self-updating source in the order "Other sources" lists them. Google Play is never listed. */
private val SOURCE_ORDER = listOf(InstallSource.GITHUB, InstallSource.FDROID, InstallSource.OBTAINIUM)

/**
 * The card for an app installed from [source]: that source first, the other two in a stable order,
 * and the delay line for F-Droid (about a day later) and Google Play (review, a day or two). A Google
 * Play card has only its own button.
 */
fun updateCardModel(source: InstallSource): UpdateCardModel {
    val policy = UpdatePolicy.forSource(source)
    return UpdateCardModel(
        main = source,
        others = if (policy.offersOtherSources) SOURCE_ORDER - source else emptyList(),
        showFdroidDelay = policy.showsInstallDelayNote && source == InstallSource.FDROID,
        showPlayDelay = policy.showsInstallDelayNote && source == InstallSource.PLAY,
        showUninstallHint = source != InstallSource.PLAY,
    )
}
