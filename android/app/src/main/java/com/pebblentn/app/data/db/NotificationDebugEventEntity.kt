package com.pebblentn.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A retained eligible-notification debug event (`notification_debug_event`, spec/300-data).
 *
 * Only enabled-package events reach this table (REQ-DEBUG-002). Notification keys and tags are
 * stored as hashes, never in the clear; content is limited to the selected snapshot JSON. Fields
 * populated by the rule engine and transport (matchedRuleId, extraction/trace JSON, ruleset
 * versions, transport status) are nullable and filled in from M4 onward. The PebbleNTN and
 * navigation-app versions (#84) are nullable: rows recorded before schema v7 have none.
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
    /** PebbleNTN's version when the event was recorded (#84); null on rows from before v7. */
    val appVersionName: String? = null,
    val appVersionCode: Long? = null,
    /** The navigation app's version when the event was recorded (#84); null when unknown. */
    val sourceAppVersionName: String? = null,
    val sourceAppVersionCode: Long? = null,
    /**
     * Consecutive repeats of the same card (#86): the grouping key ([com.pebblentn.app.data.DebugEventGrouping]),
     * how many notifications the row stands for, and the latest one's times and snapshot. The row's own
     * timestamps and snapshot are the first occurrence. Null / 1 on rows from before schema v8.
     */
    val groupKey: String? = null,
    @ColumnInfo(defaultValue = "1") val occurrences: Int = 1,
    val lastEventTimestamp: Long? = null,
    val lastReceivedTimestamp: Long? = null,
    val lastSnapshotJson: String? = null,
)
