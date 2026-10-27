package com.pebblentn.app.data

/**
 * A catalog app joined with its stored enablement, for the Navigation Apps UI.
 *
 * @property captureOnly true when the app has no official rules (show the capture-only badge).
 */
data class AppEnablement(
    val appId: String,
    val displayName: String,
    val packageNames: List<String>,
    val enabled: Boolean,
    val captureOnly: Boolean,
    /** False once the app is uninstalled: its row (and the user's choice) is kept for a reinstall. */
    val installed: Boolean = true,
    /** The app whose official rules apply ([com.pebblentn.app.catalog.NavigationAppEntry.rulesAppId], #113). */
    val rulesAppId: String = appId,
)
