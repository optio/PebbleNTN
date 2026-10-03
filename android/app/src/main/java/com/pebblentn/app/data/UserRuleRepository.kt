package com.pebblentn.app.data

import com.pebblentn.app.core.EpochClock
import com.pebblentn.app.data.db.UserRuleDao
import com.pebblentn.app.data.db.UserRuleEntity
import com.pebblentn.app.rules.Rule
import com.pebblentn.app.rules.RulesetCodec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Stores user rules and exposes them as the engine's user layer. Like the allowlist, it keeps a
 * `@Volatile` snapshot of the parsed user rules so the engine can read them without touching the
 * database; the snapshot is refreshed after every write and must be primed once on startup.
 */
class UserRuleRepository(
    private val dao: UserRuleDao,
    private val clock: EpochClock = EpochClock.SYSTEM,
) {
    @Volatile
    private var snapshot: List<Rule> = emptyList()

    /** Parsed user rules (enabled state reflected), for the engine's user layer. */
    fun userRulesSnapshot(): List<Rule> = snapshot

    suspend fun refreshCache() {
        snapshot = dao.getAll().mapNotNull { entity ->
            runCatching { RulesetCodec.parseRule(entity.canonicalJson) }
                .getOrNull()
                ?.copy(enabled = entity.enabled)
        }
    }

    fun observeUserRules(): Flow<List<UserRule>> =
        dao.observeAll().map { rows -> rows.map(::toDomain) }

    suspend fun getUserRule(ruleId: String): UserRule? = dao.getById(ruleId)?.let(::toDomain)

    /** Insert or update a user rule from its parsed form. */
    suspend fun save(
        rule: Rule,
        sourceRuleId: String?,
        validationStatus: String = RuleValidationStatus.VALID,
        sourceRuleHash: String? = null,
    ) {
        val now = clock.nowMillis()
        val existing = dao.getById(rule.id)
        dao.upsert(
            UserRuleEntity(
                ruleId = rule.id,
                sourceRuleId = sourceRuleId ?: existing?.sourceRuleId,
                packageName = rule.packageNames.firstOrNull().orEmpty(),
                canonicalJson = RulesetCodec.canonicalizeRule(rule),
                enabled = rule.enabled,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                validationStatus = validationStatus,
                sourceRuleHash = sourceRuleHash ?: existing?.sourceRuleHash,
                dismissedOfficialHash = existing?.dismissedOfficialHash,
            ),
        )
        refreshCache()
    }

    /** Clone an official rule into an editable, enabled user rule (user layer overrides bundled). */
    suspend fun cloneToUser(official: Rule) {
        // Remember what was copied, so the app can tell when the official rule changes (#58).
        save(official.copy(enabled = true), sourceRuleId = official.id, sourceRuleHash = RuleOverrides.fingerprint(official))
    }

    /**
     * Copies made before copies were fingerprinted (#58): one that still equals its official rule is
     * fingerprinted now, so a later change to the official rule is recognised as an update. One that
     * already differs stays unknown, since it can't be told whether the user or the update changed it.
     */
    suspend fun backfillSourceHashes(official: List<Rule>) {
        val officialById = official.associateBy { it.id }
        for (entity in dao.getAll()) {
            if (entity.sourceRuleHash != null) continue
            val source = officialById[entity.sourceRuleId ?: entity.ruleId] ?: officialById[entity.ruleId] ?: continue
            val rule = runCatching { RulesetCodec.parseRule(entity.canonicalJson) }.getOrNull() ?: continue
            val theirs = RuleOverrides.fingerprint(source)
            if (RuleOverrides.fingerprint(rule) == theirs) dao.setSourceRuleHash(entity.ruleId, theirs)
        }
    }

    /** "Keep mine" (#58): hide the update notice for this rule until the official rule changes again. */
    suspend fun keepMine(ruleId: String, officialFingerprint: String) {
        dao.setDismissedOfficialHash(ruleId, officialFingerprint)
    }

    /** Revert to the official rules: remove the given user rules (only overlapping ones are offered). */
    suspend fun deleteAll(ruleIds: Collection<String>) {
        ruleIds.forEach { dao.deleteById(it) }
        refreshCache()
    }

    suspend fun setEnabled(ruleId: String, enabled: Boolean) {
        dao.setEnabled(ruleId, enabled, clock.nowMillis())
        refreshCache()
    }

    suspend fun delete(ruleId: String) {
        dao.deleteById(ruleId)
        refreshCache()
    }

    /** Put back a rule exactly as it was, e.g. to undo a delete (#28), even an invalid one. */
    suspend fun restore(rule: UserRule) {
        dao.upsert(
            UserRuleEntity(
                ruleId = rule.ruleId,
                sourceRuleId = rule.sourceRuleId,
                packageName = rule.packageName,
                canonicalJson = rule.canonicalJson,
                enabled = rule.enabled,
                createdAt = rule.updatedAt,
                updatedAt = rule.updatedAt,
                validationStatus = rule.validationStatus,
                sourceRuleHash = rule.sourceRuleHash,
                dismissedOfficialHash = rule.dismissedOfficialHash,
            ),
        )
        refreshCache()
    }

    private fun toDomain(entity: UserRuleEntity): UserRule {
        val rule = runCatching { RulesetCodec.parseRule(entity.canonicalJson) }.getOrNull()
        return UserRule(
            ruleId = entity.ruleId,
            sourceRuleId = entity.sourceRuleId,
            packageName = entity.packageName,
            rule = rule?.copy(enabled = entity.enabled),
            canonicalJson = entity.canonicalJson,
            enabled = entity.enabled,
            validationStatus = entity.validationStatus,
            updatedAt = entity.updatedAt,
            sourceRuleHash = entity.sourceRuleHash,
            dismissedOfficialHash = entity.dismissedOfficialHash,
        )
    }
}
