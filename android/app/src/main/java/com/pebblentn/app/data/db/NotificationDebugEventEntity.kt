package com.pebblentn.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A retained eligible-notification debug event (`notification_debug_event`, spec/300-data).
 *
 * Only enabled-package events reach this table (REQ-DEBUG-002). Notification keys and tags are
 * stored as hashes, never in the clear; content is limited to the selected snapshot JSON. Fields
 * populated by the rule engine and transport (matchedRuleId, extraction/trace JSON, ruleset
 * versions, transport status) are nullable and filled in from M4 onward.
 */
@Entity(tableName = "notification_debug_event")
data class NotificationDebugEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventTimestamp: Long,
    val receivedTimestamp: Long,
    val packageName: String,
    val notificationKeyHash: String,
    val notificationId: Int,
    val tagHash: String?,
    val channelId: String?,
    val eventType: String,
    val selectedSnapshotJson: String,
    val activeRulesetVersions: String?,
    val matchedRuleId: String?,
    val extractionJson: String?,
    val traceJson: String?,
    val disposition: String,
    val transportStatus: String?,
    val privacyClassification: String,
)
