package com.pebblentn.app.notification

/**
 * An eligible posted notification, assembled only after the package allowlist has passed. Carries
 * the minimal snapshot plus the raw key/tag (used solely for hashing, never stored in the clear)
 * and the receipt time.
 */
data class PostedNotification(
    val snapshot: NotificationSnapshot,
    val notificationKey: String,
    val tag: String?,
    val receivedAtMillis: Long,
)

/**
 * Handles notifications that have already passed the package allowlist. In M3 the implementation
 * snapshots and stores eligible events; the rule engine and transport extend it in later milestones.
 */
interface NotificationProcessor {
    suspend fun onPosted(event: PostedNotification)
    suspend fun onRemoved(packageName: String)
}
