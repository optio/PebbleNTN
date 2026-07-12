package com.pebblentn.app.notification

/**
 * M2 [NotificationProcessor]: records that an eligible notification arrived, for the dashboard
 * indicator. It intentionally reads no notification content. M3 replaces/extends this with the
 * snapshot factory and debug-history storage.
 */
class RecordingNotificationProcessor(
    private val store: LastEligibleNotificationStore,
) : NotificationProcessor {

    override suspend fun onEligiblePosted(packageName: String, postedAtMillis: Long) {
        store.record(postedAtMillis)
    }

    override suspend fun onEligibleRemoved(packageName: String) {
        // No-op in M2; session-end handling arrives with the reducer wiring in a later milestone.
    }
}
