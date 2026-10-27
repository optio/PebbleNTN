package com.pebblentn.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pebblentn.app.catalog.NavigationAppCatalog
import com.pebblentn.app.core.EpochClock
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
 * MicroG Maps' switch (#113): its own row on GitHub, F-Droid and Obtainium installs; on a Google
 * Play install no row of its own, and its notifications follow the Google Maps switch and get the
 * Google Maps rules. Uses the real bundled catalog.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MicrogMapsEnablementTest {

    private val google = "com.google.android.apps.maps"
    private val microg = "app.morphe.android.apps.maps"

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

    // --- GitHub, F-Droid, Obtainium --------------------------------------------------------------

    @Test
    fun onOtherInstallsMicrogMapsHasItsOwnRowAndSwitch() = runTest {
        val repo = repo(bundled)
        repo.syncInstalledApps(setOf(microg))

        assertTrue(repo.isEnabled(microg))
        assertFalse("Google Maps isn't installed, so it has no row", repo.isEnabled(google))
        val row = repo.observeEnablement().first().single()
        assertEquals("microg-maps", row.appId)
        assertEquals("MicroG Maps", row.displayName)
        assertEquals(listOf(microg), row.packageNames)
        assertEquals("its rules are the Google Maps rules", "google-maps", row.rulesAppId)
        assertFalse(row.captureOnly)

        repo.setEnabled("microg-maps", false)
        assertFalse(repo.isEnabled(microg))
    }

    @Test
    fun onOtherInstallsTheTwoSwitchesAreIndependent() = runTest {
        val repo = repo(bundled)
        repo.syncInstalledApps(setOf(google, microg))
        assertEquals(listOf("google-maps", "microg-maps"), repo.observeEnablement().first().map { it.appId })

        repo.setEnabled("google-maps", false)
        assertFalse(repo.isEnabled(google))
        assertTrue(repo.isEnabled(microg))

        repo.setEnabled("google-maps", true)
        repo.setEnabled("microg-maps", false)
        assertTrue(repo.isEnabled(google))
        assertFalse(repo.isEnabled(microg))
    }

    // --- Google Play ---------------------------------------------------------------------------

    @Test
    fun onPlayMicrogMapsIsNotListedAndShowsAsGoogleMaps() = runTest {
        val repo = repo(bundled.forGooglePlay())
        repo.syncInstalledApps(setOf(microg))

        val rows = repo.observeEnablement().first()
        assertEquals(listOf("google-maps"), rows.map { it.appId })
        assertEquals("Google Maps", rows.single().displayName)
        assertTrue("installed through its MicroG package", rows.single().installed)
        assertTrue(rows.none { "MicroG" in it.displayName })
    }

    @Test
    fun onPlayGoogleMapsEnabledMeansMicrogNotificationsAreHandledWithTheGoogleMapsRules() = runTest {
        val play = bundled.forGooglePlay()
        val repo = repo(play)
        repo.syncInstalledApps(setOf(google, microg))
        val handled = dispatch(repo, play)

        assertEquals(1, handled.size)
        assertEquals("google-maps-turn-right-fr", handled.single().matchedRuleId)
    }

    @Test
    fun onPlayGoogleMapsDisabledMeansMicrogNotificationsAreIgnored() = runTest {
        val play = bundled.forGooglePlay()
        val repo = repo(play)
        repo.syncInstalledApps(setOf(microg))
        repo.setEnabled("google-maps", false)

        assertFalse(repo.isEnabled(microg))
        assertTrue(dispatch(repo, play).isEmpty())
    }

    /** Posts one French MicroG Maps turn through the privacy gate; returns what the rules made of it. */
    private fun TestScope.dispatch(repo: EnabledAppRepository, catalog: NavigationAppCatalog): List<RuleEvaluation> {
        val engine = RuleEngine(families = catalog.ruleFamilies())
        val rules = LayeredRules(bundled = RulesetCodec.parse(resource("/rules/bundled/google-maps/fr.json")).rules)
        val handled = mutableListOf<RuleEvaluation>()
        val processor = object : NotificationProcessor {
            override suspend fun onPosted(event: PostedNotification) {
                handled += engine.evaluate(event.snapshot, rules, "fr")
            }
            override suspend fun onRemoved(packageName: String) = Unit
        }
        val dispatcher = NotificationDispatcher(repo, SerialProcessingQueue(CoroutineScope(UnconfinedTestDispatcher(testScheduler))), processor)
        dispatcher.onPosted(microg) {
            PostedNotification(
                snapshot = NotificationSnapshot(packageName = microg, notificationId = 1, title = "Tournez à droite", text = "200 m"),
                notificationKey = "key",
                tag = null,
                receivedAtMillis = 20,
            )
        }
        testScheduler.advanceUntilIdle()
        return handled
    }
}
