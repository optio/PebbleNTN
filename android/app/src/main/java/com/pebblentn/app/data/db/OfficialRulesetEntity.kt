package com.pebblentn.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Metadata + payload for an official ruleset installed on the device (`official_ruleset`,
 * spec/300-data/Database.md). Downloaded rulesets are only activated after signature, schema and
 * self-test checks pass; the previous active ruleset is retained as last-known-good (REQ-SEC-006).
 */
@Entity(tableName = "official_ruleset")
data class OfficialRulesetEntity(
    @PrimaryKey val version: String,
    val source: String,
    val schemaVersion: Int,
    val signatureStatus: String,
    val activationStatus: String,
    val installedTimestamp: Long,
    val payloadHash: String,
    val canonicalJson: String,
) {
    companion object {
        const val SOURCE_DOWNLOADED = "downloaded"

        const val SIGNATURE_VERIFIED = "VERIFIED"

        const val STATUS_ACTIVE = "ACTIVE"
        const val STATUS_LAST_KNOWN_GOOD = "LAST_KNOWN_GOOD"
    }
}
