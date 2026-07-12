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
    suspend fun save(rule: Rule, sourceRuleId: String?, validationStatus: String = RuleValidationStatus.VALID) {
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
            ),
        )
        refreshCache()
    }

    /** Clone an official rule into an editable, enabled user rule (user layer overrides bundled). */
    suspend fun cloneToUser(official: Rule) {
        save(official.copy(enabled = true), sourceRuleId = official.id)
    }

    suspend fun setEnabled(ruleId: String, enabled: Boolean) {
        dao.setEnabled(ruleId, enabled, clock.nowMillis())
        refreshCache()
    }

    suspend fun delete(ruleId: String) {
        dao.deleteById(ruleId)
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
        )
    }
}
