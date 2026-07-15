package com.pebblentn.app.data

import com.pebblentn.app.core.NavigationInstruction
import com.pebblentn.app.notification.NotificationSnapshot
import com.pebblentn.app.rules.RuleTraceEntry

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
    /**
     * What the watch was shown for this notification: the normalized instruction the matched rule
     * produced. Null when nothing matched (nothing was sent). This is the extraction result, so it
     * is exactly what the watch rendered, not a re-derivation.
     */
    val instruction: NavigationInstruction? = null,
    /**
     * The per-rule evaluation trace (which rules were tried and why each matched, was skipped, or
     * failed its conditions). Persisted alongside the event; surfaced in the detail screen so a rule
     * author can see exactly why a given notification produced this watch output.
     */
    val trace: List<RuleTraceEntry> = emptyList(),
)
