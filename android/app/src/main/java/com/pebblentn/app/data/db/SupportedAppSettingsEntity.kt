package com.pebblentn.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Per-app enablement row (`supported_app_settings`, spec/300-data/Database.md).
 *
 * Keyed by the catalog [appId]. [packageName] stores the app's representative package for display;
 * the authoritative package↔app mapping (an app may publish under several packages) comes from the
 * bundled catalog, not this column.
 */
@Entity(tableName = "supported_app_settings")
data class SupportedAppSettingsEntity(
    @PrimaryKey val appId: String,
    val packageName: String,
    val enabled: Boolean,
    val captureUnmatched: Boolean,
    val firstSeenAt: Long,
    val updatedAt: Long,
)
