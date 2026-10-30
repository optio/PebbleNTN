package com.pebblentn.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pebblentn.app.catalog.NavigationAppCatalog
import com.pebblentn.app.core.EpochClock
import com.pebblentn.app.core.Maneuver
import com.pebblentn.app.data.db.PebbleNtnDatabase
import com.pebblentn.app.notification.NotificationDispatcher
import com.pebblentn.app.notification.NotificationProcessor
import com.pebblentn.app.notification.NotificationSnapshot
import com.pebblentn.app.notification.PostedNotification
import com.pebblentn.app.notification.SerialProcessingQueue
import com.pebblentn.app.rules.LayeredRules
import com.pebblentn.app.rules.RuleEngine
import com.pebblentn.app.rules.RuleEvaluation
import com.pebblentn.app.rules.RulesetCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Vela Maps' switch (#120): its own row on GitHub, F-Droid and Obtainium installs; on a Google Play
 * install a row that is never shown, enabled by default, so its notifications still reach its own
 * rules. MicroG Maps' Play behaviour is covered by [MicrogMapsEnablementTest]. Uses the real bundled
 * catalog and the bundled Vela rules.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class VelaMapsEnablementTest {

    private val vela = "app.vela"
    private val google = "com.google.android.apps.maps"

    private lateinit var db: PebbleNtnDatabase

    private fun resource(path: String): String =
        javaClass.getResourceAsStream(path)?.bufferedReader()?.use { it.readText() }
            ?: error("resource not found: $path")

    private val bundled = NavigationAppCatalog.parse(resource("/catalog/navigation-apps.json"))

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), PebbleNtnDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun repo(catalog: NavigationAppCatalog) = EnabledAppRepository(db.appEnablementDao(), catalog, EpochClock { 1_000L })

    @Test
    fun onOtherInstallsVelaMapsHasItsOwnVisibleRowAndSwitch() = runTest {
        val repo = repo(bundled)
        repo.syncInstalledApps(setOf(vela))

        assertTrue("on by default", repo.isEnabled(vela))
        val row = repo.observeEnablement().first().single()
        assertEquals("vela-maps", row.appId)
        assertEquals("Vela Maps", row.displayName)
        assertEquals("its own rules", "vela-maps", row.rulesAppId)
        assertFalse(row.captureOnly)

        repo.setEnabled("vela-maps", false)
        assertFalse(repo.isEnabled(vela))
    }

    @Test
    fun onPlayVelaMapsIsAllowlistedButNeverShown() = runTest {
        val repo = repo(bundled.forGooglePlay())
        repo.syncInstalledApps(setOf(vela, google))

        assertTrue("allowlisted, on by default", repo.isEnabled(vela))
        assertTrue(vela in repo.enabledPackageSnapshot)
        val rows = repo.observeEnablement().first()
        assertEquals("only Google Maps is shown", listOf("google-maps"), rows.map { it.appId })
        assertTrue(rows.none { "Vela" in it.displayName })
    }

    @Test
    fun onPlayAVelaNotificationIsHandledWithTheVelaRules() = runTest {
        val play = bundled.forGooglePlay()
        val repo = repo(play)
        repo.syncInstalledApps(setOf(vela))
        val handled = dispatch(repo, play)

        assertEquals(1, handled.size)
        assertEquals("vela-in-distance-turn-right-en", handled.single().matchedRuleId)
        assertEquals(Maneuver.RIGHT, handled.single().instruction?.maneuver)
    }

    @Test
    fun onPlayVelaIsNotDiscoveredWhenNotInstalled() = runTest {
        val repo = repo(bundled.forGooglePlay())
        repo.syncInstalledApps(setOf(google))
        assertFalse(repo.isEnabled(vela))
        assertTrue(dispatch(repo, bundled.forGooglePlay()).isEmpty())
    }

    /** Posts one English Vela turn through the privacy gate; returns what the rules made of it. */
    private fun TestScope.dispatch(repo: EnabledAppRepository, catalog: NavigationAppCatalog): List<RuleEvaluation> {
        val engine = RuleEngine(families = catalog.ruleFamilies())
        val rules = LayeredRules(bundled = RulesetCodec.parse(resource("/rules/bundled/vela-maps/en.json")).rules)
        val handled = mutableListOf<RuleEvaluation>()
        val processor = object : NotificationProcessor {
            override suspend fun onPosted(event: PostedNotification) {
                handled += engine.evaluate(event.snapshot, rules, "en")
            }
            override suspend fun onRemoved(packageName: String) = Unit
        }
        val dispatcher = NotificationDispatcher(repo, SerialProcessingQueue(CoroutineScope(UnconfinedTestDispatcher(testScheduler))), processor)
        dispatcher.onPosted(vela) {
            PostedNotification(
                snapshot = NotificationSnapshot(
                    packageName = vela,
                    notificationId = 1,
                    title = "In 500 ft · Turn right onto Main St",
                    text = "12 min · 3.4 mi · Arrive 12:45",
                ),
                notificationKey = "key",
                tag = null,
                receivedAtMillis = 20,
            )
        }
        testScheduler.advanceUntilIdle()
        return handled
    }
}
