package com.pebblentn.app.notification

import kotlinx.serialization.Serializable

/**
 * The immutable, minimal record of an eligible notification (REQ-SEC-003, spec/300-data).
 *
 * It holds only the documented selected fields. It deliberately has no place for PendingIntents,
 * actions, RemoteViews, icons, people/contact identifiers or arbitrary extras — those are never
 * extracted, so they cannot be stored or exported.
 */
@Serializable
data class NotificationSnapshot(
    val packageName: String,
    val notificationId: Int,
    val channelId: String? = null,
    val category: String? = null,
    val template: String? = null,
    val postTimeMillis: Long = 0,
    val whenTimeMillis: Long? = null,
    val title: String? = null,
    val text: String? = null,
    val subText: String? = null,
    val bigText: String? = null,
    val summaryText: String? = null,
    val infoText: String? = null,
) {
    /**
     * Text fields joined for rule matching. Rules reference `combinedText` (see the bundled
     * example ruleset) as well as the individual named fields.
     */
    val combinedText: String
        get() = listOfNotNull(title, text, bigText, subText, summaryText, infoText)
            .filter { it.isNotBlank() }
            .joinToString(separator = " ")
}
