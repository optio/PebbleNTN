package com.pebblentn.app.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Migration test (AGENTS.md rule 15) for schema v1 -> v2 (adds notification_debug_event).
 *
 * Builds a real v1 database file with the exact v1 schema + a data row, then opens the Room v2
 * database with [PebbleNtnDatabase.MIGRATION_1_2] and asserts the migration preserves existing data
 * and Room's own schema validation passes (which requires the new table to exist and match).
 */
@RunWith(RobolectricTestRunner::class)
class PebbleNtnDatabaseMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "migration-test.db"

    @Before
    fun setUp() {
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migrate1To2PreservesDataAndAddsDebugTable() {
        // --- Create a v1 database exactly as Room v1 would have. ---
        val path = context.getDatabasePath(dbName)
        path.parentFile?.mkdirs()
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(path, null).use { v1 ->
            v1.execSQL(
                "CREATE TABLE IF NOT EXISTS `supported_app_settings` (" +
                    "`appId` TEXT NOT NULL, `packageName` TEXT NOT NULL, `enabled` INTEGER NOT NULL, " +
                    "`captureUnmatched` INTEGER NOT NULL, `firstSeenAt` INTEGER NOT NULL, " +
                    "`updatedAt` INTEGER NOT NULL, PRIMARY KEY(`appId`))",
            )
            v1.execSQL(
                "INSERT INTO supported_app_settings VALUES ('google-maps','com.google.android.apps.maps',1,0,100,100)",
            )
            v1.version = 1
        }

        // --- Open v2 through Room with the migration. Room validates the resulting schema. ---
        val db = Room.databaseBuilder(context, PebbleNtnDatabase::class.java, dbName)
            .addMigrations(*PebbleNtnDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()

        runBlocking {
            // Pre-existing row survived the migration.
            assertEquals("com.google.android.apps.maps", db.appEnablementDao().getByAppId("google-maps")?.packageName)
            // The new table exists and is usable (empty).
            assertEquals(0, db.debugEventDao().count())
        }
        db.close()
    }
}
