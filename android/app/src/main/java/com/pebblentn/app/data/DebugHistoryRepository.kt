package com.pebblentn.app.data

import com.pebblentn.app.core.Hashing
import com.pebblentn.app.data.db.DebugEventDao
import com.pebblentn.app.data.db.NotificationDebugEventEntity
import com.pebblentn.app.core.NavigationInstruction
import com.pebblentn.app.notification.NotificationSnapshot
import com.pebblentn.app.notification.PostedNotification
import com.pebblentn.app.rules.RuleEvaluation
import com.pebblentn.app.rules.RuleTraceEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Stores and reads eligible-notification debug events. Notification keys/tags are hashed before
 * storage; only the selected snapshot JSON carries content. Insertion trims to the retention bound
 * in one transaction (REQ-ANDROID-008, spec/300-data/Database.md).
 */
class DebugHistoryRepository(
    private val dao: DebugEventDao,
    private val json: Json = Json,
) {
    /** Current retention bound; [UNLIMITED] disables trimming. Default is [DEFAULT_RETENTION]. */
    @Volatile
    var retentionLimit: Int = DEFAULT_RETENTION

    /** Record an eligible post, optionally with the rule [evaluation] that ran against it. */
    suspend fun recordPosted(event: PostedNotification, evaluation: RuleEvaluation? = null): Long {
        val snapshot = event.snapshot
        val entity = NotificationDebugEventEntity(
            eventTimestamp = snapshot.postTimeMillis,
            receivedTimestamp = event.receivedAtMillis,
            packageName = snapshot.packageName,
            notificationKeyHash = Hashing.sha256Hex(event.notificationKey),
            notificationId = snapshot.notificationId,
            tagHash = event.tag?.let(Hashing::sha256Hex),
            channelId = snapshot.channelId,
            eventType = DebugEventType.POSTED.name,
            selectedSnapshotJson = json.encodeToString(NotificationSnapshot.serializer(), snapshot),
            activeRulesetVersions = null,
            matchedRuleId = evaluation?.matchedRuleId,
            extractionJson = evaluation?.instruction?.let {
                json.encodeToString(NavigationInstruction.serializer(), it)
            },
            traceJson = evaluation?.let {
                json.encodeToString(ListSerializer(RuleTraceEntry.serializer()), it.trace)
            },
            disposition = if (evaluation?.matched == true) DebugDisposition.MATCHED else DebugDisposition.CAPTURED_UNMATCHED,
            transportStatus = null,
            privacyClassification = PrivacyClassification.RAW,
        )
        return dao.insertAndTrim(entity, retentionLimit)
    }

    fun observeRecent(limit: Int = DEFAULT_PAGE): Flow<List<DebugEvent>> =
        dao.observeRecent(limit).map { rows -> rows.map(::toDomain) }

    suspend fun getById(id: Long): DebugEvent? = dao.getById(id)?.let(::toDomain)

    suspend fun count(): Int = dao.count()

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun deleteAll() = dao.deleteAll()

    private fun toDomain(entity: NotificationDebugEventEntity): DebugEvent {
        val snapshot = runCatching {
            json.decodeFromString(NotificationSnapshot.serializer(), entity.selectedSnapshotJson)
        }.getOrNull()
        val instruction = entity.extractionJson?.let {
            runCatching { json.decodeFromString(NavigationInstruction.serializer(), it) }.getOrNull()
        }
        val trace = entity.traceJson?.let {
            runCatching { json.decodeFromString(ListSerializer(RuleTraceEntry.serializer()), it) }.getOrNull()
        }.orEmpty()
        return DebugEvent(
            id = entity.id,
            packageName = entity.packageName,
            eventType = runCatching { DebugEventType.valueOf(entity.eventType) }.getOrDefault(DebugEventType.POSTED),
            eventTimestampMillis = entity.eventTimestamp,
            receivedTimestampMillis = entity.receivedTimestamp,
            snapshot = snapshot,
            matchedRuleId = entity.matchedRuleId,
            disposition = entity.disposition,
            instruction = instruction,
            trace = trace,
        )
    }

    companion object {
        const val UNLIMITED = 0
        const val DEFAULT_RETENTION = 500
        const val DEFAULT_PAGE = 200

        /** Retention choices offered in settings (REQ-ANDROID-008). */
        val RETENTION_CHOICES = listOf(50, 100, 500, 1000, UNLIMITED)
    }
}
