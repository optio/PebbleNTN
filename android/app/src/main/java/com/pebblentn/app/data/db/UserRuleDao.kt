package com.pebblentn.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Data access for user rules. */
@Dao
interface UserRuleDao {

    @Query("SELECT * FROM user_rule ORDER BY packageName, ruleId")
    fun observeAll(): Flow<List<UserRuleEntity>>

    @Query("SELECT * FROM user_rule ORDER BY packageName, ruleId")
    suspend fun getAll(): List<UserRuleEntity>

    @Query("SELECT * FROM user_rule WHERE ruleId = :ruleId")
    suspend fun getById(ruleId: String): UserRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserRuleEntity)

    @Query("UPDATE user_rule SET enabled = :enabled, updatedAt = :updatedAt WHERE ruleId = :ruleId")
    suspend fun setEnabled(ruleId: String, enabled: Boolean, updatedAt: Long)

    @Query("UPDATE user_rule SET sourceRuleHash = :hash WHERE ruleId = :ruleId")
    suspend fun setSourceRuleHash(ruleId: String, hash: String)

    @Query("UPDATE user_rule SET dismissedOfficialHash = :hash WHERE ruleId = :ruleId")
    suspend fun setDismissedOfficialHash(ruleId: String, hash: String)

    @Query("DELETE FROM user_rule WHERE ruleId = :ruleId")
    suspend fun deleteById(ruleId: String)

    @Query("DELETE FROM user_rule")
    suspend fun deleteAll()
}
