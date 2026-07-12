package com.pebblentn.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Data access for per-app enablement settings. */
@Dao
interface AppEnablementDao {

    @Query("SELECT * FROM supported_app_settings")
    fun observeAll(): Flow<List<SupportedAppSettingsEntity>>

    @Query("SELECT * FROM supported_app_settings")
    suspend fun getAll(): List<SupportedAppSettingsEntity>

    @Query("SELECT * FROM supported_app_settings WHERE appId = :appId")
    suspend fun getByAppId(appId: String): SupportedAppSettingsEntity?

    /** Insert a new row only if one does not already exist for this app. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: SupportedAppSettingsEntity): Long

    @Query("UPDATE supported_app_settings SET enabled = :enabled, updatedAt = :updatedAt WHERE appId = :appId")
    suspend fun setEnabled(appId: String, enabled: Boolean, updatedAt: Long)

    @Query("UPDATE supported_app_settings SET captureUnmatched = :capture, updatedAt = :updatedAt WHERE appId = :appId")
    suspend fun setCaptureUnmatched(appId: String, capture: Boolean, updatedAt: Long)
}
