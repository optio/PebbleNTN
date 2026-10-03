package com.pebblentn.app.data

import com.pebblentn.app.notification.NotificationSnapshot
import com.pebblentn.app.rules.LayeredRules
import com.pebblentn.app.rules.Rule
import com.pebblentn.app.rules.RuleEngine
import com.pebblentn.app.rules.RulesetCodec
import java.security.MessageDigest

/** How a user rule relates to the official (bundled) rule it overrides (REQ-ANDROID-016, #58). */
enum class OverrideStatus {
    /** A copy whose content still equals the official rule: redundant, the official one does the same. */
    IDENTICAL,

    /** A copy the user edited; the official rule hasn't changed since it was copied. */
    EDITED,

    /** An unedited copy, and the official rule has changed since: the user is missing the update. */
    OFFICIAL_UPDATED,

    /** An edited copy, and the official rule has changed since. */
    OFFICIAL_UPDATED_EDITED,

    /** A copy made before copies were fingerprinted that differs from the official rule. */
    DIFFERS,

    /** Not a copy, but it matches recent notifications an official rule would otherwise handle. */
    TAKES_OVER,
}

/**
 * One user rule that overrides official rules. [officialFingerprint] identifies the official rule's
 * current content for a copy, so "Keep mine" can be remembered until the official rule changes again.
 */
data class RuleOverride(
    val userRuleId: String,
    val officialRuleIds: List<String>,
    val status: OverrideStatus,
    val officialFingerprint: String?,
    /** The official rule changed since this copy was made and the user hasn't chosen to keep theirs. */
    val needsAttention: Boolean,
)

/**
 * Finds the user rules that override official rules. User rules always take precedence, so a copy
 * of an official rule keeps winning even after the app ships a better official rule (#58); this
 * makes that visible. Pure: no I/O.
 */
object RuleOverrides {

    /** Content fingerprint of a rule, ignoring its enabled state. */
    fun fingerprint(rule: Rule): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(RulesetCodec.canonicalizeRule(rule.copy(enabled = true)).toByteArray(Charsets.UTF_8))
        return bytes.take(16).joinToString("") { "%02x".format(it) }
    }

    /**
     * Overrides among the enabled, valid [userRules]. A copy is matched to its official rule by
     * `sourceRuleId`, or by sharing an official rule's id. Any other user rule overrides the official
     * rules that would have handled the [captures] it matches (recent notifications, so this reflects
     * what the user actually navigates with).
     */
    fun analyze(
        userRules: List<UserRule>,
        official: List<Rule>,
        captures: List<NotificationSnapshot> = emptyList(),
        locale: String? = null,
        engine: RuleEngine = RuleEngine(),
    ): List<RuleOverride> {
        val officialById = official.associateBy { it.id }
        val officialLayer = LayeredRules(bundled = official)
        return userRules.mapNotNull { userRule ->
            val rule = userRule.rule?.takeIf { userRule.enabled } ?: return@mapNotNull null
            val source = (userRule.sourceRuleId ?: userRule.ruleId).let(officialById::get)
                ?: officialById[userRule.ruleId]
            if (source != null) {
                copyOverride(userRule, rule, source)
            } else {
                takesOver(userRule, rule, captures, officialLayer, locale, engine)
            }
        }
    }

    private fun copyOverride(userRule: UserRule, rule: Rule, official: Rule): RuleOverride {
        val mine = fingerprint(rule)
        val theirs = fingerprint(official)
        val copiedFrom = userRule.sourceRuleHash
        val status = when {
            mine == theirs -> OverrideStatus.IDENTICAL
            copiedFrom == null -> OverrideStatus.DIFFERS
            copiedFrom == theirs -> OverrideStatus.EDITED
            mine == copiedFrom -> OverrideStatus.OFFICIAL_UPDATED
            else -> OverrideStatus.OFFICIAL_UPDATED_EDITED
        }
        val updated = status == OverrideStatus.OFFICIAL_UPDATED || status == OverrideStatus.OFFICIAL_UPDATED_EDITED
        return RuleOverride(
            userRuleId = userRule.ruleId,
            officialRuleIds = listOf(official.id),
            status = status,
            officialFingerprint = theirs,
            needsAttention = updated && userRule.dismissedOfficialHash != theirs,
        )
    }

    private fun takesOver(
        userRule: UserRule,
        rule: Rule,
        captures: List<NotificationSnapshot>,
        officialLayer: LayeredRules,
        locale: String?,
        engine: RuleEngine,
    ): RuleOverride? {
        val mineOnly = LayeredRules(user = listOf(rule))
        val taken = captures.asSequence()
            .filter { it.packageName in rule.packageNames }
            .filter { engine.evaluate(it, mineOnly, locale).matched }
            .mapNotNull { engine.evaluate(it, officialLayer, locale).matchedRuleId }
            .distinct()
            .sorted()
            .toList()
        if (taken.isEmpty()) return null
        return RuleOverride(userRule.ruleId, taken, OverrideStatus.TAKES_OVER, officialFingerprint = null, needsAttention = false)
    }
}
