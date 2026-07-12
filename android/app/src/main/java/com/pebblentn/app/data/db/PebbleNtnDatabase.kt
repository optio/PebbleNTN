package com.pebblentn.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The app's Room database. Entities are added milestone by milestone; version bumps require a
 * migration and a migration test (AGENTS.md rule 15). M2 introduces app enablement only.
 */
@Database(
    entities = [SupportedAppSettingsEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class PebbleNtnDatabase : RoomDatabase() {
    abstract fun appEnablementDao(): AppEnablementDao

    companion object {
        const val NAME = "pebblentn.db"
    }
}
