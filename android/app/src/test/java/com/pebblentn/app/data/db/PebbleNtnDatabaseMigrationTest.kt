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
            // v3–v5 tables are present too (migrations run in sequence).
            assertEquals(null, db.userRuleDao().getById("none"))
            assertEquals(null, db.navigationStateDao().get())
            assertEquals(null, db.officialRulesetDao().getByStatus("ACTIVE"))
        }
        db.close()
    }

    @Test
    fun migrate5To6KeepsUserRulesAndAddsOverrideColumns() {
        // --- A v5 database with every table, and one user rule. ---
        val path = context.getDatabasePath(dbName)
        path.parentFile?.mkdirs()
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(path, null).use { v5 ->
            listOf(
                "CREATE TABLE IF NOT EXISTS `supported_app_settings` (`appId` TEXT NOT NULL, `packageName` TEXT NOT NULL, `enabled` INTEGER NOT NULL, `captureUnmatched` INTEGER NOT NULL, `firstSeenAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`appId`))",
                "CREATE TABLE IF NOT EXISTS `notification_debug_event` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `eventTimestamp` INTEGER NOT NULL, `receivedTimestamp` INTEGER NOT NULL, `packageName` TEXT NOT NULL, `notificationKeyHash` TEXT NOT NULL, `notificationId` INTEGER NOT NULL, `tagHash` TEXT, `channelId` TEXT, `eventType` TEXT NOT NULL, `selectedSnapshotJson` TEXT NOT NULL, `activeRulesetVersions` TEXT, `matchedRuleId` TEXT, `extractionJson` TEXT, `traceJson` TEXT, `disposition` TEXT NOT NULL, `transportStatus` TEXT, `privacyClassification` TEXT NOT NULL)",
                "CREATE TABLE IF NOT EXISTS `user_rule` (`ruleId` TEXT NOT NULL, `sourceRuleId` TEXT, `packageName` TEXT NOT NULL, `canonicalJson` TEXT NOT NULL, `enabled` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `validationStatus` TEXT NOT NULL, PRIMARY KEY(`ruleId`))",
                "CREATE TABLE IF NOT EXISTS `navigation_state` (`id` INTEGER NOT NULL, `sessionId` INTEGER, `active` INTEGER NOT NULL, `normalizedStateJson` TEXT NOT NULL, `stateTimestampSeconds` INTEGER NOT NULL, `nextSessionId` INTEGER NOT NULL, `launchedSessionId` INTEGER, PRIMARY KEY(`id`))",
                "CREATE TABLE IF NOT EXISTS `official_ruleset` (`version` TEXT NOT NULL, `source` TEXT NOT NULL, `schemaVersion` INTEGER NOT NULL, `signatureStatus` TEXT NOT NULL, `activationStatus` TEXT NOT NULL, `installedTimestamp` INTEGER NOT NULL, `payloadHash` TEXT NOT NULL, `canonicalJson` TEXT NOT NULL, PRIMARY KEY(`version`))",
                "INSERT INTO user_rule VALUES ('comaps-navigation-step','comaps-navigation-step','app.comaps.google','{}',1,100,100,'VALID')",
            ).forEach(v5::execSQL)
            v5.version = 5
        }

        val db = Room.databaseBuilder(context, PebbleNtnDatabase::class.java, dbName)
            .addMigrations(*PebbleNtnDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            val row = runBlocking { db.userRuleDao().getById("comaps-navigation-step") }!!
            assertEquals("comaps-navigation-step", row.sourceRuleId)
            assertEquals(null, row.sourceRuleHash)
            assertEquals(null, row.dismissedOfficialHash)
        } finally {
            db.close()
        }
    }
}
