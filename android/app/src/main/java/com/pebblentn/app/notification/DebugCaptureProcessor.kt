package com.pebblentn.app.notification

import com.pebblentn.app.data.DebugHistoryRepository

/**
 * M3 [NotificationProcessor]: stores each eligible posted notification in debug history and updates
 * the dashboard's "last eligible" indicator. Only reached for allowlisted packages
 * (REQ-DEBUG-002). The rule engine (M4) extends this to also evaluate rules and record the trace.
 */
class DebugCaptureProcessor(
    private val debugHistory: DebugHistoryRepository,
    private val lastEligibleStore: LastEligibleNotificationStore,
) : NotificationProcessor {

    override suspend fun onPosted(event: PostedNotification) {
        debugHistory.recordPosted(event)
        lastEligibleStore.record(event.snapshot.postTimeMillis)
    }

    override suspend fun onRemoved(packageName: String) {
        // Session-end handling arrives with the reducer/transport wiring in a later milestone.
    }
}
