package com.pebblentn.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The app's Room database. Entities are added milestone by milestone; version bumps require a
 * migration and a migration test (AGENTS.md rule 15).
 *
 * v1 (M2): supported_app_settings.
 * v2 (M3): + notification_debug_event.
 * v3 (M5): + user_rule.
 * v4 (M9): + navigation_state.
 * v5 (M11): + official_ruleset.
 */
@Database(
    entities = [
        SupportedAppSettingsEntity::class,
        NotificationDebugEventEntity::class,
        UserRuleEntity::class,
        NavigationStateEntity::class,
        OfficialRulesetEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class PebbleNtnDatabase : RoomDatabase() {
    abstract fun appEnablementDao(): AppEnablementDao
    abstract fun debugEventDao(): DebugEventDao
    abstract fun userRuleDao(): UserRuleDao
    abstract fun navigationStateDao(): NavigationStateDao
    abstract fun officialRulesetDao(): OfficialRulesetDao

    companion object {
        const val NAME = "pebblentn.db"

        /** Adds the notification_debug_event table. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `notification_debug_event` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `eventTimestamp` INTEGER NOT NULL,
                        `receivedTimestamp` INTEGER NOT NULL,
                        `packageName` TEXT NOT NULL,
                        `notificationKeyHash` TEXT NOT NULL,
                        `notificationId` INTEGER NOT NULL,
                        `tagHash` TEXT,
                        `channelId` TEXT,
                        `eventType` TEXT NOT NULL,
                        `selectedSnapshotJson` TEXT NOT NULL,
                        `activeRulesetVersions` TEXT,
                        `matchedRuleId` TEXT,
                        `extractionJson` TEXT,
                        `traceJson` TEXT,
                        `disposition` TEXT NOT NULL,
                        `transportStatus` TEXT,
                        `privacyClassification` TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        /** Adds the user_rule table. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `user_rule` (
                        `ruleId` TEXT NOT NULL PRIMARY KEY,
                        `sourceRuleId` TEXT,
                        `packageName` TEXT NOT NULL,
                        `canonicalJson` TEXT NOT NULL,
                        `enabled` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `validationStatus` TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        /** Adds the navigation_state singleton table. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `navigation_state` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `sessionId` INTEGER,
                        `active` INTEGER NOT NULL,
                        `normalizedStateJson` TEXT NOT NULL,
                        `stateTimestampSeconds` INTEGER NOT NULL,
                        `nextSessionId` INTEGER NOT NULL,
                        `launchedSessionId` INTEGER
                    )
                    """.trimIndent(),
                )
            }
        }

        /** Adds the official_ruleset table (remote official rules, off by default). */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `official_ruleset` (
                        `version` TEXT NOT NULL PRIMARY KEY,
                        `source` TEXT NOT NULL,
                        `schemaVersion` INTEGER NOT NULL,
                        `signatureStatus` TEXT NOT NULL,
                        `activationStatus` TEXT NOT NULL,
                        `installedTimestamp` INTEGER NOT NULL,
                        `payloadHash` TEXT NOT NULL,
                        `canonicalJson` TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        val ALL_MIGRATIONS: Array<Migration> =
            arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
    }
}
