package com.pebblentn.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

/** Data access for official rulesets, with atomic activation + rollback. */
@Dao
interface OfficialRulesetDao {

    @Query("SELECT * FROM official_ruleset WHERE activationStatus = :status LIMIT 1")
    suspend fun getByStatus(status: String): OfficialRulesetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: OfficialRulesetEntity)

    @Query("UPDATE official_ruleset SET activationStatus = :status WHERE version = :version")
    suspend fun setStatus(version: String, status: String)

    @Query("DELETE FROM official_ruleset WHERE activationStatus = :status")
    suspend fun deleteByStatus(status: String)

    @Query("DELETE FROM official_ruleset WHERE version = :version")
    suspend fun deleteByVersion(version: String)

    /**
     * Atomically activate [entity]: drop the previous last-known-good, demote the current active to
     * last-known-good, then install [entity] as active. Keeps at most one active + one LKG.
     */
    @Transaction
    suspend fun activate(entity: OfficialRulesetEntity) {
        deleteByStatus(OfficialRulesetEntity.STATUS_LAST_KNOWN_GOOD)
        getByStatus(OfficialRulesetEntity.STATUS_ACTIVE)?.let {
            setStatus(it.version, OfficialRulesetEntity.STATUS_LAST_KNOWN_GOOD)
        }
        upsert(entity)
    }

    /** Atomically roll back: delete the active ruleset and promote last-known-good to active. */
    @Transaction
    suspend fun rollback(): Boolean {
        val lkg = getByStatus(OfficialRulesetEntity.STATUS_LAST_KNOWN_GOOD) ?: return false
        getByStatus(OfficialRulesetEntity.STATUS_ACTIVE)?.let { deleteByVersion(it.version) }
        setStatus(lkg.version, OfficialRulesetEntity.STATUS_ACTIVE)
        return true
    }
}
