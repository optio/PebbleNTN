package com.pebblentn.app.notification

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pebblentn.app.core.EpochClock
import com.pebblentn.app.data.DebugDisposition
import com.pebblentn.app.data.DebugHistoryRepository
import com.pebblentn.app.data.db.PebbleNtnDatabase
import com.pebblentn.app.rules.Condition
import com.pebblentn.app.rules.ConditionOperator
import com.pebblentn.app.rules.DistanceExtractor
import com.pebblentn.app.rules.LayeredRules
import com.pebblentn.app.rules.LiteralExtractor
import com.pebblentn.app.rules.Rule
import com.pebblentn.app.rules.RuleEngine
import com.pebblentn.app.rules.RuleOutput
import com.pebblentn.app.rules.RuleRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DebugCaptureProcessorTest {

    private lateinit var db: PebbleNtnDatabase
    private lateinit var debugHistory: DebugHistoryRepository

    private val turnRightRule = Rule(
        id = "turn-right",
        enabled = true,
        priority = 100,
        packageNames = listOf("com.google.android.apps.maps"),
        conditions = listOf(Condition("combinedText", ConditionOperator.CONTAINS_IGNORE_CASE, "turn right")),
        output = RuleOutput(maneuver = LiteralExtractor("RIGHT"), distanceMeters = DistanceExtractor("combinedText")),
    )

    private fun repositoryOf(vararg rules: Rule) = RuleRepository { LayeredRules(bundled = rules.toList()) }

    private fun processor(repo: RuleRepository) = DebugCaptureProcessor(
        debugHistory = debugHistory,
        lastEligibleStore = LastEligibleNotificationStore(),
        ruleEngine = RuleEngine(),
        ruleRepository = repo,
        clock = EpochClock { 0 },
    )

    private fun posted(text: String) = PostedNotification(
        snapshot = NotificationSnapshot(packageName = "com.google.android.apps.maps", notificationId = 1, text = text, postTimeMillis = 10),
        notificationKey = "key",
        tag = null,
        receivedAtMillis = 20,
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PebbleNtnDatabase::class.java,
        ).allowMainThreadQueries().build()
        debugHistory = DebugHistoryRepository(db.debugEventDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun matchedRuleIsRecordedWithExtraction() = runTest {
        processor(repositoryOf(turnRightRule)).onPosted(posted("In 500 m, turn right"))

        val event = debugHistory.observeRecent().first().single()
        assertEquals(DebugDisposition.MATCHED, event.disposition)
        assertEquals("turn-right", event.matchedRuleId)
    }

    @Test
    fun unmatchedIsRecordedAsCapturedUnmatched() = runTest {
        processor(repositoryOf(turnRightRule)).onPosted(posted("In 500 m, turn left"))

        val event = debugHistory.observeRecent().first().single()
        assertEquals(DebugDisposition.CAPTURED_UNMATCHED, event.disposition)
        assertEquals(null, event.matchedRuleId)
    }

    @Test
    fun noRulesRecordsUnmatched() = runTest {
        processor(repositoryOf()).onPosted(posted("turn right"))
        val event = debugHistory.observeRecent().first().single()
        assertEquals(DebugDisposition.CAPTURED_UNMATCHED, event.disposition)
    }
}
