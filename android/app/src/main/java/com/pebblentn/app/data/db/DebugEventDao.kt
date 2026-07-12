package com.pebblentn.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** Data access for debug events, including transactional retention trimming. */
@Dao
interface DebugEventDao {

    @Insert
    suspend fun insert(entity: NotificationDebugEventEntity): Long

    /** Delete all but the newest [limit] events (by received time, then id). */
    @Query(
        "DELETE FROM notification_debug_event WHERE id NOT IN (" +
            "SELECT id FROM notification_debug_event ORDER BY receivedTimestamp DESC, id DESC LIMIT :limit)",
    )
    suspend fun trimTo(limit: Int)

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

    @Query("SELECT * FROM notification_debug_event ORDER BY receivedTimestamp DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<NotificationDebugEventEntity>>

    @Query("SELECT * FROM notification_debug_event WHERE id = :id")
    suspend fun getById(id: Long): NotificationDebugEventEntity?

    @Query("SELECT COUNT(*) FROM notification_debug_event")
    suspend fun count(): Int

    @Query("DELETE FROM notification_debug_event WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM notification_debug_event")
    suspend fun deleteAll()
}
