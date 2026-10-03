package com.pebblentn.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pebblentn.app.core.EpochClock
import com.pebblentn.app.data.db.PebbleNtnDatabase
import com.pebblentn.app.rules.Condition
import com.pebblentn.app.rules.ConditionOperator
import com.pebblentn.app.rules.LiteralExtractor
import com.pebblentn.app.rules.Rule
import com.pebblentn.app.rules.RuleOutput
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserRuleRepositoryTest {

    private lateinit var db: PebbleNtnDatabase
    private lateinit var repo: UserRuleRepository

    private fun rule(id: String, enabled: Boolean = true, maneuver: String = "RIGHT") = Rule(
        id = id,
        enabled = enabled,
        priority = 100,
        packageNames = listOf("com.google.android.apps.maps"),
        conditions = listOf(Condition("combinedText", ConditionOperator.CONTAINS, "turn")),
        output = RuleOutput(maneuver = LiteralExtractor(maneuver)),
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PebbleNtnDatabase::class.java,
        ).allowMainThreadQueries().build()
        repo = UserRuleRepository(db.userRuleDao(), EpochClock { 1000 })
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun savedRuleAppearsInSnapshotAndObserve() = runTest {
        repo.save(rule("r1"), sourceRuleId = null)
        assertEquals(listOf("r1"), repo.userRulesSnapshot().map { it.id })
        assertEquals(1, repo.observeUserRules().first().size)
    }

    @Test
    fun cloneToUserSetsSourceAndEnables() = runTest {
        repo.cloneToUser(rule("official", enabled = false))
        val stored = repo.getUserRule("official")!!
        assertEquals("official", stored.sourceRuleId)
        assertTrue(stored.enabled)
    }

    @Test
    fun disablingRuleReflectsInSnapshot() = runTest {
        repo.save(rule("r1"), sourceRuleId = null)
        repo.setEnabled("r1", false)
        assertEquals(false, repo.userRulesSnapshot().single().enabled)
    }

    @Test
    fun deleteRemovesRule() = runTest {
        repo.save(rule("r1"), sourceRuleId = null)
        repo.delete("r1")
        assertTrue(repo.userRulesSnapshot().isEmpty())
        assertNull(repo.getUserRule("r1"))
    }

    @Test
    fun savePreservesCreatedAtOnUpdate() = runTest {
        repo.save(rule("r1", maneuver = "RIGHT"), sourceRuleId = null)
        val created = db.userRuleDao().getById("r1")!!.createdAt
        repo.save(rule("r1", maneuver = "LEFT"), sourceRuleId = null)
        val after = db.userRuleDao().getById("r1")!!
        assertEquals(created, after.createdAt)
        assertTrue(after.canonicalJson.contains("LEFT"))
    }

    @Test
    fun aDeletedRuleCanBeRestoredExactly() = runTest {
        repo.save(rule("r1"), sourceRuleId = "official-r1")
        repo.setEnabled("r1", false)
        val before = repo.getUserRule("r1")!!
        repo.delete("r1")
        assertEquals(null, repo.getUserRule("r1"))

        repo.restore(before)
        val after = repo.getUserRule("r1")!!
        assertEquals("disabled state survives the undo", false, after.enabled)
        assertEquals("official-r1", after.sourceRuleId)
        assertEquals(before.canonicalJson, after.canonicalJson)
    }

    // ---- #58: overrides of official rules ----

    @Test
    fun cloneRecordsTheOfficialFingerprintAndEditsKeepIt() = runTest {
        val official = rule("official")
        repo.cloneToUser(official)
        val fp = RuleOverrides.fingerprint(official)
        assertEquals(fp, repo.getUserRule("official")!!.sourceRuleHash)

        repo.save(official.copy(priority = 300), sourceRuleId = null) // edited in the editor
        assertEquals(fp, repo.getUserRule("official")!!.sourceRuleHash)
        assertEquals("official", repo.getUserRule("official")!!.sourceRuleId)
    }

    @Test
    fun backfillFingerprintsOnlyOldCopiesThatStillEqualTheirOfficialRule() = runTest {
        val official = rule("official")
        repo.save(official, sourceRuleId = "official") // an old, unchanged copy (no fingerprint)
        repo.save(rule("other", maneuver = "LEFT"), sourceRuleId = "official-other") // differs from its official
        repo.save(rule("fresh"), sourceRuleId = null) // not a copy at all
        repo.backfillSourceHashes(listOf(official, rule("official-other")))

        assertEquals(RuleOverrides.fingerprint(official), repo.getUserRule("official")!!.sourceRuleHash)
        assertNull(repo.getUserRule("other")!!.sourceRuleHash)
        assertNull(repo.getUserRule("fresh")!!.sourceRuleHash)
    }

    @Test
    fun keepMineIsStoredAndRevertRemovesOnlyTheGivenRules() = runTest {
        repo.cloneToUser(rule("official"))
        repo.save(rule("mine"), sourceRuleId = null)
        repo.keepMine("official", "abc")
        assertEquals("abc", repo.getUserRule("official")!!.dismissedOfficialHash)

        repo.deleteAll(listOf("official"))
        assertNull(repo.getUserRule("official"))
        assertEquals(listOf("mine"), repo.userRulesSnapshot().map { it.id })
    }
}
