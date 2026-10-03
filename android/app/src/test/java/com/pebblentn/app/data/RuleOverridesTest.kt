package com.pebblentn.app.data

import com.pebblentn.app.notification.NotificationSnapshot
import com.pebblentn.app.rules.Condition
import com.pebblentn.app.rules.ConditionOperator
import com.pebblentn.app.rules.LiteralExtractor
import com.pebblentn.app.rules.Rule
import com.pebblentn.app.rules.RuleOutput
import com.pebblentn.app.rules.RulesetCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** REQ-ANDROID-016 (#58): which user rules override official rules, and when to say so. */
class RuleOverridesTest {

    private val maps = "com.google.android.apps.maps"

    private fun rule(id: String, word: String = "turn", maneuver: String = "RIGHT", enabled: Boolean = true) = Rule(
        id = id,
        enabled = enabled,
        priority = 100,
        packageNames = listOf(maps),
        conditions = listOf(Condition("combinedText", ConditionOperator.CONTAINS, word)),
        output = RuleOutput(maneuver = LiteralExtractor(maneuver)),
    )

    private fun user(
        rule: Rule,
        sourceRuleId: String? = null,
        sourceRuleHash: String? = null,
        dismissed: String? = null,
        enabled: Boolean = true,
    ) = UserRule(
        ruleId = rule.id,
        sourceRuleId = sourceRuleId,
        packageName = maps,
        rule = rule.copy(enabled = enabled),
        canonicalJson = RulesetCodec.canonicalizeRule(rule),
        enabled = enabled,
        validationStatus = RuleValidationStatus.VALID,
        updatedAt = 0,
        sourceRuleHash = sourceRuleHash,
        dismissedOfficialHash = dismissed,
    )

    private val officialV1 = rule("official-right")
    private val officialV2 = rule("official-right", word = "right")
    private val fp1 = RuleOverrides.fingerprint(officialV1)

    private fun analyze(vararg users: UserRule, official: Rule = officialV1, captures: List<NotificationSnapshot> = emptyList()) =
        RuleOverrides.analyze(users.toList(), listOf(official), captures)

    @Test
    fun fingerprintIgnoresTheEnabledFlag() {
        assertEquals(fp1, RuleOverrides.fingerprint(officialV1.copy(enabled = false)))
        assertTrue(fp1 != RuleOverrides.fingerprint(officialV2))
    }

    @Test
    fun anUnchangedCopyIsIdenticalAndNeedsNoNotice() {
        val o = analyze(user(officialV1, "official-right", fp1)).single()
        assertEquals(OverrideStatus.IDENTICAL, o.status)
        assertEquals(listOf("official-right"), o.officialRuleIds)
        assertFalse(o.needsAttention)
    }

    @Test
    fun anEditedCopyOfAnUnchangedOfficialRuleIsEdited() {
        val edited = officialV1.copy(priority = 300)
        assertEquals(OverrideStatus.EDITED, analyze(user(edited, "official-right", fp1)).single().status)
    }

    @Test
    fun aNewerOfficialRuleNeedsAttentionForUnchangedAndEditedCopies() {
        val unchanged = analyze(user(officialV1, "official-right", fp1), official = officialV2).single()
        assertEquals(OverrideStatus.OFFICIAL_UPDATED, unchanged.status)
        assertTrue(unchanged.needsAttention)

        val edited = analyze(user(officialV1.copy(priority = 300), "official-right", fp1), official = officialV2).single()
        assertEquals(OverrideStatus.OFFICIAL_UPDATED_EDITED, edited.status)
        assertTrue(edited.needsAttention)
    }

    @Test
    fun keepMineHidesTheNoticeUntilTheOfficialRuleChangesAgain() {
        val fp2 = RuleOverrides.fingerprint(officialV2)
        assertFalse(analyze(user(officialV1, "official-right", fp1, dismissed = fp2), official = officialV2).single().needsAttention)
        val officialV3 = rule("official-right", word = "right turn")
        assertTrue(analyze(user(officialV1, "official-right", fp1, dismissed = fp2), official = officialV3).single().needsAttention)
    }

    @Test
    fun anOldCopyWithoutFingerprintThatDiffersIsUnknown() {
        val o = analyze(user(officialV1.copy(priority = 300), "official-right", sourceRuleHash = null)).single()
        assertEquals(OverrideStatus.DIFFERS, o.status)
        assertFalse(o.needsAttention)
    }

    @Test
    fun aRuleSharingAnOfficialIdCountsAsACopy() {
        assertEquals(OverrideStatus.IDENTICAL, analyze(user(officialV1)).single().status)
    }

    @Test
    fun aDifferentRuleTakesOverOnlyWhereItMatchesTheSameNotifications() {
        val mine = user(rule("my-rule", word = "turn right", maneuver = "SHARP_RIGHT"))
        val turnRight = NotificationSnapshot(packageName = maps, notificationId = 1, title = "100 m · turn right onto Main St")
        val walk = NotificationSnapshot(packageName = maps, notificationId = 1, title = "Walk towards Main St")

        val o = analyze(mine, captures = listOf(turnRight, walk)).single()
        assertEquals(OverrideStatus.TAKES_OVER, o.status)
        assertEquals(listOf("official-right"), o.officialRuleIds)
        assertNull(o.officialFingerprint)

        assertTrue(analyze(mine, captures = listOf(walk)).isEmpty())
    }

    @Test
    fun disabledOrUnrelatedRulesOverrideNothing() {
        assertTrue(analyze(user(officialV1, "official-right", fp1, enabled = false)).isEmpty())
        assertTrue(analyze(user(rule("my-rule", word = "ferry"))).isEmpty())
    }
}
