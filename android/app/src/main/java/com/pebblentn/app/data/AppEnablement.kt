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
)
