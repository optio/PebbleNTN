package com.pebblentn.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-authored or user-cloned rule (`user_rule`, spec/300-data/Database.md). Stored as canonical
 * JSON so the editor and engine share one representation.
 *
 * @property ruleId the rule's stable id (also the primary key).
 * @property sourceRuleId the official rule this was cloned from, or null for a fresh rule.
 * @property validationStatus last known validation state ("VALID" / "INVALID").
 * @property sourceRuleHash fingerprint of the official rule when it was copied, to tell whether the
 *   official rule changed since (#58); null for fresh rules and copies made before it was recorded.
 * @property dismissedOfficialHash the official rule's fingerprint when the user chose "Keep mine", so
 *   the update notice stays hidden until the official rule changes again.
 */
@Entity(tableName = "user_rule")
data class UserRuleEntity(
    @PrimaryKey val ruleId: String,
    val sourceRuleId: String?,
    val packageName: String,
    val canonicalJson: String,
    val enabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val validationStatus: String,
    val sourceRuleHash: String? = null,
    val dismissedOfficialHash: String? = null,
)
