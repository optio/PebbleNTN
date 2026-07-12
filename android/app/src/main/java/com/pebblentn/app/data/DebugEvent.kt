package com.pebblentn.app.data

import com.pebblentn.app.notification.NotificationSnapshot

/** Event types recorded in debug history. */
enum class DebugEventType { POSTED, REMOVED }

/** Disposition of a captured event. */
object DebugDisposition {
    const val MATCHED = "MATCHED"
    const val CAPTURED_UNMATCHED = "CAPTURED_UNMATCHED"
}

/** Privacy classification of a stored event's content. */
object PrivacyClassification {
    const val RAW = "RAW"
}

/** A retained debug event, joined and parsed for the debug-history UI. */
data class DebugEvent(
    val id: Long,
    val packageName: String,
    val eventType: DebugEventType,
    val eventTimestampMillis: Long,
    val receivedTimestampMillis: Long,
    val snapshot: NotificationSnapshot?,
    val matchedRuleId: String?,
    val disposition: String,
)
