package com.pebblentn.app.ui

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pebblentn.app.data.DebugHistoryRepository
import com.pebblentn.app.data.UserRuleRepository
import com.pebblentn.app.data.db.PebbleNtnDatabase
import com.pebblentn.app.catalog.NavigationAppCatalog
import com.pebblentn.app.rules.RulePreviewService
import com.pebblentn.app.rules.RuleValidationResult
import com.pebblentn.app.ui.rules.RulesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RulesViewModelTest {

    private lateinit var history: DebugHistoryRepository

    private lateinit var db: PebbleNtnDatabase
    private lateinit var vm: RulesViewModel
    private lateinit var userRepo: UserRuleRepository

    private val validRule = """
        {"id":"turn-right","enabled":true,"priority":100,"packageNames":["com.google.android.apps.maps"],
         "conditions":[{"field":"combinedText","operator":"containsIgnoreCase","value":"turn right"}],
         "output":{"maneuver":{"type":"literal","value":"RIGHT"}}}
    """.trimIndent()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PebbleNtnDatabase::class.java,
        ).allowMainThreadQueries().build()
        userRepo = UserRuleRepository(db.userRuleDao())
        history = DebugHistoryRepository(db.debugEventDao())
        vm = RulesViewModel(
            userRuleRepository = userRepo,
            debugHistoryRepository = history,
            previewService = RulePreviewService(),
            catalog = NavigationAppCatalog(schemaVersion = 1, apps = emptyList()),
            officialRules = emptyList(),
        )
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun saveValidRulePersistsIt() = runTest {
        val result = vm.save(validRule)
        assertTrue(result is RuleValidationResult.Valid)
        assertEquals(listOf("turn-right"), userRepo.userRulesSnapshot().map { it.id })
    }

    @Test
    fun saveInvalidRuleReturnsErrorsAndDoesNotPersist() = runTest {
        val result = vm.save("{ not valid")
        assertTrue(result is RuleValidationResult.Invalid)
        assertTrue(userRepo.userRulesSnapshot().isEmpty())
    }

    @Test
    fun formatReturnsCanonicalOrNull() {
        assertTrue(vm.format(validRule)!!.contains("\"id\": \"turn-right\""))
        assertNull(vm.format("{ broken"))
    }

    @Test
    fun setEnabledAndDelete() = runTest {
        vm.save(validRule)
        vm.setEnabled("turn-right", false).join()
        assertEquals(false, userRepo.userRulesSnapshot().single().enabled)
        vm.delete("turn-right").join()
        assertTrue(userRepo.userRulesSnapshot().isEmpty())
    }

    @Test
    fun editorInitialJsonForNewRuleIsTemplate() = runTest {
        assertEquals(RulesViewModel.NEW_RULE_TEMPLATE, vm.editorInitialJson(null))
    }

    private suspend fun capture(title: String): Long = history.recordPosted(
        com.pebblentn.app.notification.PostedNotification(
            snapshot = com.pebblentn.app.notification.NotificationSnapshot(
                packageName = "com.google.android.apps.maps",
                notificationId = 1,
                title = title,
            ),
            notificationKey = title,
            tag = null,
            receivedAtMillis = 1,
        ),
    )

    @Test
    fun previewRunsAgainstTheChosenCapture() = runTest {
        val turnRight = capture("Turn right onto Main St")
        capture("Continue straight") // the latest, which the preview must NOT use
        val result = vm.previewAgainst(validRule, turnRight)
        val evaluated = result as com.pebblentn.app.rules.PreviewResult.Evaluated
        assertTrue("the chosen capture matches the rule", evaluated.evaluation.matched)
    }

    @Test
    fun createRuleFromCaptureGivesAValidRuleForThatApp() = runTest {
        val id = capture("Use the left lane to merge")
        val json = vm.editorJsonFromCapture(id)!!
        assertTrue(json, vm.validate(json) is RuleValidationResult.Valid)
        assertTrue(json.contains("com.google.android.apps.maps"))
        assertTrue(json.contains("Use the left lane to merge"))
    }

    @Test
    fun undoBringsBackTheDeletedRule() = runTest {
        vm.save(validRule)
        vm.delete("turn-right").join()
        assertTrue(userRepo.userRulesSnapshot().isEmpty())
        vm.undoDelete().join()
        assertEquals(listOf("turn-right"), userRepo.userRulesSnapshot().map { it.id })
    }
}
