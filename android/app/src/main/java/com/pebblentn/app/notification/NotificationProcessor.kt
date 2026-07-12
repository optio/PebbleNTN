package com.pebblentn.app.notification

/**
 * Handles notifications that have already passed the package allowlist. This seam is where M3 adds
 * snapshotting and debug-history storage; in M2 the only implementation records that an eligible
 * notification arrived (for the dashboard's "last eligible notification" indicator).
 *
 * Implementations receive only the package name and metadata timestamps — never notification
 * content in M2.
 */
interface NotificationProcessor {
    suspend fun onEligiblePosted(packageName: String, postedAtMillis: Long)
    suspend fun onEligibleRemoved(packageName: String)
}
