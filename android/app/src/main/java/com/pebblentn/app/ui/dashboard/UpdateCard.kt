package com.pebblentn.app.ui.dashboard

import com.pebblentn.app.update.InstallSource

/**
 * What the update card offers (REQ-ANDROID-020): the main "Update from {source}" button for the
 * detected [main] source, the [others] listed under "Other sources", and whether to add the F-Droid
 * delay line. Pure, so the choice is unit-tested apart from the Compose card.
 */
data class UpdateCardModel(
    val main: InstallSource,
    val others: List<InstallSource>,
    val showFdroidDelay: Boolean,
)

/** Every source in the order "Other sources" lists them. */
private val SOURCE_ORDER = listOf(InstallSource.GITHUB, InstallSource.FDROID, InstallSource.OBTAINIUM)

/**
 * The card for an app installed from [source]: that source first, the other two in a stable order,
 * and the delay line only for F-Droid, which builds and publishes releases about a day later.
 */
fun updateCardModel(source: InstallSource): UpdateCardModel = UpdateCardModel(
    main = source,
    others = SOURCE_ORDER - source,
    showFdroidDelay = source == InstallSource.FDROID,
)
