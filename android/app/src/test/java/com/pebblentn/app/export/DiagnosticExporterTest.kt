package com.pebblentn.app.export

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pebblentn.app.data.DebugHistoryRepository
import com.pebblentn.app.data.UserRuleRepository
import com.pebblentn.app.data.db.PebbleNtnDatabase
import com.pebblentn.app.notification.NotificationSnapshot
import com.pebblentn.app.notification.PostedNotification
import com.pebblentn.app.rules.Condition
import com.pebblentn.app.rules.ConditionOperator
import com.pebblentn.app.rules.LiteralExtractor
import com.pebblentn.app.rules.Rule
import com.pebblentn.app.rules.RuleOutput
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DiagnosticExporterTest {

    private lateinit var db: PebbleNtnDatabase
    private lateinit var exporter: DiagnosticExporter

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PebbleNtnDatabase::class.java,
        ).allowMainThreadQueries().build()

        val debugHistory = DebugHistoryRepository(db.debugEventDao())
        val userRules = UserRuleRepository(db.userRuleDao())
        debugHistory.recordPosted(
            PostedNotification(
                snapshot = NotificationSnapshot(packageName = "com.google.android.apps.maps", notificationId = 1, title = "Turn right", text = "onto Elm Street"),
                notificationKey = "k",
                tag = null,
                receivedAtMillis = 1,
            ),
        )
        userRules.save(
            Rule("r", true, 100, listOf("com.google.android.apps.maps"),
                conditions = listOf(Condition("combinedText", ConditionOperator.CONTAINS, "turn")),
                output = RuleOutput(maneuver = LiteralExtractor("RIGHT"))),
            sourceRuleId = null,
        )
        exporter = DiagnosticExporter(debugHistory, userRules, appVersion = "0.0.1", androidRelease = "14", now = { "2026-07-12T00:00:00Z" })
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun rulesOnlyExcludesEvents() = runTest {
        val out = exporter.build(ExportMode.RULES_ONLY)
        assertTrue(out.contains("\"r\""))
        assertFalse(out.contains("Elm"))
    }

    @Test
    fun privacySafeRedactsButKeepsRules() = runTest {
        val out = exporter.build(ExportMode.PRIVACY_SAFE)
        assertTrue(out.contains("\"r\""))
        assertFalse(out.contains("Elm"))
        assertTrue(out.contains(Redactor.PLACEHOLDER))
    }

    @Test
    fun fullIncludesRaw() = runTest {
        val out = exporter.build(ExportMode.FULL)
        assertTrue(out.contains("Elm Street"))
    }
}
