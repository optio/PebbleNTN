package com.pebblentn.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pebblentn.app.catalog.NavigationAppCatalog
import com.pebblentn.app.core.EpochClock
import com.pebblentn.app.data.db.PebbleNtnDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EnabledAppRepositoryTest {

    private lateinit var db: PebbleNtnDatabase
    private lateinit var repo: EnabledAppRepository

    private val catalog = NavigationAppCatalog.parse(
        """
        {"schemaVersion":1,"apps":[
          {"appId":"google-maps","displayName":"Google Maps","packageNames":["com.google.android.apps.maps"],"hasOfficialRules":true,"captureAvailable":true,"defaultEnabled":true},
          {"appId":"waze","displayName":"Waze","packageNames":["com.waze"],"hasOfficialRules":false,"captureAvailable":true,"defaultEnabled":true},
          {"appId":"osmand","displayName":"OsmAnd","packageNames":["net.osmand","net.osmand.plus"],"hasOfficialRules":false,"captureAvailable":true,"defaultEnabled":true}
        ]}
        """.trimIndent(),
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PebbleNtnDatabase::class.java,
        ).allowMainThreadQueries().build()
        repo = EnabledAppRepository(db.appEnablementDao(), catalog, EpochClock { 1_000L })
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun installedAppsAreEnabledByDefaultAndUninstalledAreNot() = runTest {
        repo.syncInstalledApps(setOf("com.google.android.apps.maps", "com.waze"))

        assertTrue(repo.isEnabled("com.google.android.apps.maps"))
        assertTrue(repo.isEnabled("com.waze"))
        // OsmAnd is not installed -> no row -> not enabled.
        assertFalse(repo.isEnabled("net.osmand"))
        // Unknown package is never enabled.
        assertFalse(repo.isEnabled("com.example.unknown"))
    }

    @Test
    fun enablingAppCoversAllItsPackages() = runTest {
        repo.syncInstalledApps(setOf("net.osmand"))
        assertTrue(repo.isEnabled("net.osmand"))
        // Both packages of the same catalog app are allowlisted.
        assertTrue(repo.isEnabled("net.osmand.plus"))
    }

    @Test
    fun disablingAppRemovesItFromAllowlist() = runTest {
        repo.syncInstalledApps(setOf("com.google.android.apps.maps"))
        assertTrue(repo.isEnabled("com.google.android.apps.maps"))

        repo.setEnabled("google-maps", false)
        assertFalse(repo.isEnabled("com.google.android.apps.maps"))
    }

    @Test
    fun syncDoesNotOverwriteExistingUserChoice() = runTest {
        repo.syncInstalledApps(setOf("com.google.android.apps.maps"))
        repo.setEnabled("google-maps", false)
        // Re-discovering an already-known app must not re-enable it.
        repo.syncInstalledApps(setOf("com.google.android.apps.maps"))
        assertFalse(repo.isEnabled("com.google.android.apps.maps"))
    }

    @Test
    fun isEnabledBeforeAnySyncIsFalse() {
        // Cache starts empty; gate is closed until a sync/refresh runs.
        assertFalse(repo.isEnabled("com.google.android.apps.maps"))
    }

    @Test
    fun observeEnablementReturnsDiscoveredAppsWithCaptureFlag() = runTest {
        repo.syncInstalledApps(setOf("com.google.android.apps.maps", "com.waze"))
        val enablement = repo.observeEnablement().first().associateBy { it.appId }

        assertEquals(2, enablement.size)
        assertFalse(enablement.getValue("google-maps").captureOnly)
        assertTrue(enablement.getValue("waze").captureOnly)
    }
}
