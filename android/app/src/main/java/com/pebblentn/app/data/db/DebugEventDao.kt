package com.pebblentn.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** When an event was last seen: a grouped event moves up with its latest occurrence (#86). */
private const val LAST_RECEIVED = "COALESCE(lastReceivedTimestamp, receivedTimestamp)"

/** Data access for debug events, including transactional retention trimming. */
@Dao
interface DebugEventDao {

    @Insert
    suspend fun insert(entity: NotificationDebugEventEntity): Long

    /** Delete all but the newest [limit] events (by last received time, then id). */
    @Query(
        "DELETE FROM notification_debug_event WHERE id NOT IN (" +
            "SELECT id FROM notification_debug_event ORDER BY $LAST_RECEIVED DESC, id DESC LIMIT :limit)",
    )
    suspend fun trimTo(limit: Int)

    /** The most recently updated event: the only one a new notification can be grouped into (#86). */
    @Query("SELECT * FROM notification_debug_event ORDER BY $LAST_RECEIVED DESC, id DESC LIMIT 1")
    suspend fun latest(): NotificationDebugEventEntity?

    @Query(
        "UPDATE notification_debug_event SET occurrences = occurrences + 1, lastEventTimestamp = :eventTimestamp, " +
            "lastReceivedTimestamp = :receivedTimestamp, lastSnapshotJson = :snapshotJson WHERE id = :id",
    )
    suspend fun addOccurrence(id: Long, eventTimestamp: Long, receivedTimestamp: Long, snapshotJson: String)

    /**
     * Record [entity]: when the latest event has the same non-null [NotificationDebugEventEntity.groupKey],
     * count it as one more occurrence of that event (keeping this one as its latest example);
     * otherwise insert it and trim. One transaction. Returns the id of the row that holds it.
     */
    @Transaction
    suspend fun insertOrGroup(entity: NotificationDebugEventEntity, limit: Int): Long {
        val latest = latest()
        if (entity.groupKey != null && latest != null && latest.groupKey == entity.groupKey) {
            addOccurrence(latest.id, entity.eventTimestamp, entity.receivedTimestamp, entity.selectedSnapshotJson)
            return latest.id
        }
        return insertAndTrim(entity, limit)
    }

    /**
     * Insert an event and, if [limit] is positive, trim to the newest [limit] in one transaction so
     * the table never briefly exceeds the retention bound (spec/300-data/Database.md).
     */
    @Transaction
    suspend fun insertAndTrim(entity: NotificationDebugEventEntity, limit: Int): Long {
        val id = insert(entity)
        if (limit > 0) trimTo(limit)
        return id
    }

    @Query("SELECT * FROM notification_debug_event ORDER BY $LAST_RECEIVED DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<NotificationDebugEventEntity>>

    @Query("SELECT * FROM notification_debug_event WHERE id = :id")
    suspend fun getById(id: Long): NotificationDebugEventEntity?

    @Query("SELECT COUNT(*) FROM notification_debug_event")
    suspend fun count(): Int

    /** Live count of events with a given disposition; drives the "help us add support" nudge. */
    @Query("SELECT COUNT(*) FROM notification_debug_event WHERE disposition = :disposition")
    fun observeCountByDisposition(disposition: String): Flow<Int>

    @Query("DELETE FROM notification_debug_event WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM notification_debug_event")
    suspend fun deleteAll()
}
