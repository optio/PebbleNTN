package com.pebblentn.app.ui

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pebblentn.app.data.DebugHistoryRepository
import com.pebblentn.app.data.UserRuleRepository
import com.pebblentn.app.data.db.PebbleNtnDatabase
import com.pebblentn.app.export.DiagnosticExporter
import com.pebblentn.app.export.ExportMode
import com.pebblentn.app.notification.NotificationSnapshot
import com.pebblentn.app.notification.PostedNotification
import com.pebblentn.app.ui.share.ShareDiagnosticsState
import com.pebblentn.app.ui.share.ShareDiagnosticsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * REQ-DEBUG-011: the share-to-help screen preselects the full dataset (with street names), and the
 * redacted dataset is one tap away.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ShareDiagnosticsViewModelTest {

    private lateinit var db: PebbleNtnDatabase
    private lateinit var exporter: DiagnosticExporter

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PebbleNtnDatabase::class.java,
        ).allowMainThreadQueries().build()
        val debugHistory = DebugHistoryRepository(db.debugEventDao())
        debugHistory.recordPosted(
            PostedNotification(
                snapshot = NotificationSnapshot(
                    packageName = "com.example.nav",
                    notificationId = 1,
                    title = "Turn right onto Elm Street",
                ),
                notificationKey = "k",
                tag = null,
                receivedAtMillis = 1,
            ),
        )
        exporter = DiagnosticExporter(
            debugHistory,
            UserRuleRepository(db.userRuleDao()),
            appVersion = "0.0.1",
            androidRelease = "14",
            now = { "2026-09-30T00:00:00Z" },
        )
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun ShareDiagnosticsViewModel.awaitLoaded(): ShareDiagnosticsState = runBlocking {
        withTimeout(5_000) {
            while (state.value.loading) delay(10)
        }
        state.value
    }

    @Test
    fun fullDatasetWithStreetNamesIsPreselected() {
        val state = ShareDiagnosticsViewModel(exporter).awaitLoaded()

        assertEquals(ExportMode.FULL, state.mode)
        assertTrue("the preview shows the raw text", state.previewText.contains("Elm Street"))
    }

    @Test
    fun switchingToRedactedRemovesStreetNames() {
        val vm = ShareDiagnosticsViewModel(exporter)
        vm.awaitLoaded()

        vm.setMode(ExportMode.PRIVACY_SAFE)
        val state = vm.awaitLoaded()

        assertEquals(ExportMode.PRIVACY_SAFE, vm.currentMode())
        assertFalse("redacted preview drops the road name", state.previewText.contains("Elm Street"))
    }

    @Test
    fun rulesOnlyHasContentWithoutNotificationText() {
        val vm = ShareDiagnosticsViewModel(exporter)
        vm.awaitLoaded()

        vm.setMode(ExportMode.RULES_ONLY)
        val state = vm.awaitLoaded()

        assertEquals("no events in a rules-only export", 0, state.includedEvents)
        assertTrue("the user's rules can always be shared", state.hasContent)
        assertFalse(state.previewText.contains("Elm Street"))
    }

    @Test
    fun aModeChosenRightAwayIsNotOverwrittenByTheFirstBuild() {
        // "Share your rules" opens the screen and selects rules only immediately; the initial full
        // build finishing later must not switch it back (seen on the emulator).
        val vm = ShareDiagnosticsViewModel(exporter)
        vm.setMode(ExportMode.RULES_ONLY)
        val state = vm.awaitLoaded()
        runBlocking { delay(300) } // give a stale build every chance to land

        assertEquals(ExportMode.RULES_ONLY, vm.state.value.mode)
        assertEquals(0, state.includedEvents)
    }
}
