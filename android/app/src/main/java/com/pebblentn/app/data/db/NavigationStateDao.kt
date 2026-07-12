package com.pebblentn.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** Data access for the singleton cached navigation state. */
@Dao
interface NavigationStateDao {

    @Query("SELECT * FROM navigation_state WHERE id = 0")
    suspend fun get(): NavigationStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: NavigationStateEntity)

    @Query("DELETE FROM navigation_state")
    suspend fun clear()
}
