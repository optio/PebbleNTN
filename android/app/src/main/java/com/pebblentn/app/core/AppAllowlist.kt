package com.pebblentn.app.core

/**
 * The privacy-critical gate: decides whether notifications from a package may be processed.
 *
 * Kept as a tiny pure interface so the notification listener can consult it **synchronously on the
 * callback thread, using only the package name, before any notification content is touched**
 * (REQ-ANDROID-003, REQ-ANDROID-006). Implementations must not perform I/O in [isEnabled].
 */
fun interface AppAllowlist {
    fun isEnabled(packageName: String): Boolean
}
