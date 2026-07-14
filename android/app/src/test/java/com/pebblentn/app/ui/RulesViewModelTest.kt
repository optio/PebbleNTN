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
        vm = RulesViewModel(
            userRuleRepository = userRepo,
            debugHistoryRepository = DebugHistoryRepository(db.debugEventDao()),
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
}
