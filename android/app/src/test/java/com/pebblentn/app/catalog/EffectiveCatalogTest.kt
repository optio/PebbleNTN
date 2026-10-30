package com.pebblentn.app.catalog

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pebblentn.app.core.EpochClock
import com.pebblentn.app.data.EnabledAppRepository
import com.pebblentn.app.data.db.PebbleNtnDatabase
import com.pebblentn.app.notification.NotificationDispatcher
import com.pebblentn.app.notification.NotificationProcessor
import com.pebblentn.app.notification.NotificationSnapshot
import com.pebblentn.app.notification.PostedNotification
import com.pebblentn.app.notification.SerialProcessingQueue
import com.pebblentn.app.notification.icon.resourcePackagesToTry
import com.pebblentn.app.rules.LayeredRules
import com.pebblentn.app.rules.RuleEngine
import com.pebblentn.app.rules.RuleEvaluation
import com.pebblentn.app.rules.RulesetCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * FR6: a user-added package works like its app's built-in ones — same family and rules, same switch,
 * the FR2 icon fallback to the app's first package — and stops working once removed, without a
 * restart. Uses the real bundled catalog and Google Maps rules.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class EffectiveCatalogTest {

    private val google = "com.google.android.apps.maps"
    private val microg = "app.morphe.android.apps.maps"
    private val ungoogled = "org.ungoogled.android.apps.maps.microg"
    private val custom = "app.revanced.android.apps.maps"

    private class FakeStore(initial: Map<String, List<String>> = emptyMap()) : UserPackageStore {
        private val state = MutableStateFlow(initial)
        override val packages: StateFlow<Map<String, List<String>>> = state
        override fun add(appId: String, packageName: String) {
            state.value = state.value + (appId to state.value[appId].orEmpty() + packageName)
        }
        override fun remove(appId: String, packageName: String) {
            val left = state.value[appId].orEmpty() - packageName
            state.value = if (left.isEmpty()) state.value - appId else state.value + (appId to left)
        }
    }

    private fun resource(path: String): String =
        javaClass.getResourceAsStream(path)?.bufferedReader()?.use { it.readText() } ?: error("resource not found: $path")

    private val bundled = NavigationAppCatalog.parse(resource("/catalog/navigation-apps.json"))

    private lateinit var db: PebbleNtnDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), PebbleNtnDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun effective(googlePlay: Boolean = false, store: UserPackageStore = FakeStore()) =
        EffectiveCatalog(bundled, googlePlay, store)

    @Test
    fun aUserAddedPackageJoinsItsAppAfterTheBuiltInOnes() {
        val catalog = effective()
        assertEquals(PackageCheck.Valid(custom), catalog.add("google-maps", " $custom "))

        val entry = catalog.current.entryForPackage(custom)!!
        assertEquals("google-maps", entry.appId)
        assertEquals(listOf(google, custom), entry.packageNames)
        assertTrue(catalog.current.isUserAdded(custom))
        assertFalse(catalog.current.isUserAdded(google))
        assertEquals(mapOf("google-maps" to listOf(custom)), catalog.current.userAddedByApp())
        assertEquals("debug labels name the app", "Google Maps", com.pebblentn.app.ui.format.DisplayLabels.appName(catalog.current, custom))
        assertEquals(listOf(google), catalog.builtInPackagesOf("google-maps"))
        assertEquals(listOf(custom), catalog.userAddedPackagesOf("google-maps"))
    }

    @Test
    fun theLiveFamiliesFollowAddAndRemoveWithTheIconFallbackToTheFirstBuiltInPackage() {
        val catalog = effective()
        val families = catalog.families
        assertFalse(families.applies(custom, listOf(google)))

        catalog.add("google-maps", custom)
        assertTrue(families.applies(custom, listOf(google)))
        assertEquals(listOf(custom, google), resourcePackagesToTry(custom, families))
        // Built-in packages keep exactly their FR1/FR2 behaviour.
        assertNull(families.resourceFallbackFor(google))
        assertEquals(google, families.resourceFallbackFor(microg))

        catalog.remove("google-maps", custom)
        assertFalse(families.applies(custom, listOf(google)))
        assertEquals(listOf(custom), resourcePackagesToTry(custom, families))
    }

    @Test
    fun aMicrogPackageFallsBackToGoogleMapsAndOtherAppsToTheirOwnFirstPackage() {
        val catalog = effective()
        catalog.add("microg-maps", custom)
        catalog.add("comaps", "app.comaps.beta")

        assertEquals(google, catalog.families.resourceFallbackFor(custom))
        assertEquals("app.comaps.google", catalog.families.resourceFallbackFor("app.comaps.beta"))
        assertNull("built-in CoMaps variants still have none", catalog.families.resourceFallbackFor("app.comaps.fdroid"))
    }

    @Test
    fun aUserAddedGoogleMapsPackageMatchesLikeGoogleMaps() = runTest {
        val catalog = effective()
        catalog.add("google-maps", custom)
        val repo = enablement(catalog)
        repo.syncInstalledApps(emptySet())

        assertTrue("the user said it is installed, so it gets its row and the default switch", repo.isEnabled(custom))
        val handled = dispatch(repo, catalog.families, custom)
        assertEquals("google-maps-turn-right-fr", handled.single().matchedRuleId)
    }

    @Test
    fun aUserAddedPackageIsIgnoredWhenItsAppIsOff() = runTest {
        val catalog = effective()
        catalog.add("google-maps", custom)
        val repo = enablement(catalog)
        repo.syncInstalledApps(emptySet())
        repo.setEnabled("google-maps", false)

        assertFalse(repo.isEnabled(custom))
        assertTrue(dispatch(repo, catalog.families, custom).isEmpty())
    }

    @Test
    fun aRemovedPackageNoLongerMatches() = runTest {
        val catalog = effective()
        catalog.add("google-maps", custom)
        val repo = enablement(catalog)
        repo.syncInstalledApps(setOf(google))
        assertEquals(1, dispatch(repo, catalog.families, custom).size)

        catalog.remove("google-maps", custom)
        // As AppContainer does on every catalog change.
        repo.syncInstalledApps(setOf(google))

        assertFalse(repo.isEnabled(custom))
        assertTrue(repo.isEnabled(google))
        assertTrue(dispatch(repo, catalog.families, custom).isEmpty())
    }

    @Test
    fun anAppWithAUserAddedPackageIsListedAsInstalled() = runTest {
        val catalog = effective()
        catalog.add("microg-maps", custom)
        val repo = enablement(catalog)
        repo.syncInstalledApps(emptySet())

        val row = repo.observeEnablement().first().single()
        assertEquals("microg-maps", row.appId)
        assertTrue(row.installed)
        assertEquals(listOf(microg, ungoogled, custom), row.packageNames)
    }

    @Test
    fun onPlayAPackageAddedToGoogleMapsWorksAndMicrogsPackageIsGoogleMaps() = runTest {
        val catalog = effective(googlePlay = true)
        assertEquals(listOf(google, microg, ungoogled), catalog.builtInPackagesOf("google-maps"))
        assertTrue(catalog.builtInPackagesOf("microg-maps").isEmpty())
        // The joined MicroG package is a Google Maps built-in here: no app named MicroG Maps.
        assertEquals(PackageCheck.Invalid(PackageCheck.Reason.BUILT_IN_HERE), catalog.add("google-maps", microg))
        assertEquals(PackageCheck.Invalid(PackageCheck.Reason.BUILT_IN_ELSEWHERE, "google-maps"), catalog.add("osmand", microg))

        assertEquals(PackageCheck.Valid(custom), catalog.add("google-maps", custom))
        // User-added packages join before the Play view, so the joined MicroG package comes after them.
        assertEquals(listOf(google, custom, microg, ungoogled), catalog.current.entryForPackage(custom)!!.packageNames)
        assertEquals(google, catalog.families.resourceFallbackFor(custom))
        val repo = enablement(catalog)
        repo.syncInstalledApps(emptySet())
        assertEquals("google-maps-turn-right-fr", dispatch(repo, catalog.families, custom).single().matchedRuleId)
    }

    @Test
    fun theSecondMicrogPackageIsBuiltInAndCannotBeAddedAgain() {
        // #113: org.ungoogled.android.apps.maps.microg is a locked built-in package of MicroG Maps.
        val catalog = effective()
        assertEquals(listOf(microg, ungoogled), catalog.builtInPackagesOf("microg-maps"))
        assertEquals(PackageCheck.Invalid(PackageCheck.Reason.BUILT_IN_HERE), catalog.add("microg-maps", ungoogled))
        assertEquals(PackageCheck.Invalid(PackageCheck.Reason.BUILT_IN_ELSEWHERE, "microg-maps"), catalog.add("google-maps", ungoogled))
        assertTrue(catalog.userAddedPackagesOf("microg-maps").isEmpty())
        assertEquals(google, catalog.families.resourceFallbackFor(ungoogled))
        // Hidden on Google Play: it is a Google Maps package there, never a MicroG Maps entry.
        val play = effective(googlePlay = true)
        assertEquals("google-maps", play.current.entryForPackage(ungoogled)!!.appId)
        assertTrue(play.current.apps.none { it.appId == "microg-maps" })
    }

    @Test
    fun aNameWithCapitalsIsStoredExactlyMatchesItsAppAndItsLowerCaseFormIsADuplicate() = runTest {
        // Revision 2: package names are case-sensitive. A catalog without the Baidu Maps entry, so
        // that its real, capitalised package can be added by the user (to Google Maps here).
        val baidu = "com.baidu.BaiduMap"
        val withoutBaidu = bundled.copy(apps = bundled.apps.filterNot { it.appId == "baidu-maps" })
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val catalog = EffectiveCatalog(withoutBaidu, googlePlay = false, store = com.pebblentn.app.data.CustomPackagesRepository(context))

        assertEquals(PackageCheck.Valid(baidu), catalog.add("google-maps", "  $baidu "))
        assertEquals(listOf(baidu), catalog.userAddedPackagesOf("google-maps"))
        assertEquals("kept with its capitals after a restart", mapOf("google-maps" to listOf(baidu)), com.pebblentn.app.data.CustomPackagesRepository(context).packages.value)
        assertTrue(catalog.current.isUserAdded(baidu))
        assertNull("the lower-case form is another package", catalog.current.entryForPackage("com.baidu.baidumap"))

        val repo = enablement(catalog)
        repo.syncInstalledApps(emptySet())
        assertTrue(repo.isEnabled(baidu))
        assertEquals("google-maps-turn-right-fr", dispatch(repo, catalog.families, baidu).single().matchedRuleId)

        assertEquals(PackageCheck.Invalid(PackageCheck.Reason.ADDED_HERE), catalog.add("google-maps", "com.baidu.baidumap"))
        assertEquals(PackageCheck.Invalid(PackageCheck.Reason.ADDED_ELSEWHERE, "google-maps"), catalog.add("osmand", "COM.BAIDU.BAIDUMAP"))
        assertEquals(listOf(baidu), catalog.userAddedPackagesOf("google-maps"))
    }

    @Test
    fun theLowerCaseFormOfABuiltInPackageWithCapitalsIsADuplicate() {
        assertEquals(
            PackageCheck.Invalid(PackageCheck.Reason.BUILT_IN_ELSEWHERE, "baidu-maps"),
            effective().add("google-maps", "com.baidu.baidumap"),
        )
    }

    @Test
    fun aRejectedNameIsNotStored() {
        val store = FakeStore()
        val catalog = effective(store = store)
        assertEquals(PackageCheck.Invalid(PackageCheck.Reason.FORMAT), catalog.add("google-maps", "not a package"))
        assertEquals(PackageCheck.Invalid(PackageCheck.Reason.BUILT_IN_ELSEWHERE, "google-maps"), catalog.add("microg-maps", google))
        catalog.add("osmand", custom)
        assertEquals(PackageCheck.Invalid(PackageCheck.Reason.ADDED_ELSEWHERE, "osmand"), catalog.add("google-maps", custom))
        assertEquals(mapOf("osmand" to listOf(custom)), store.packages.value)
    }

    @Test
    fun storedPackagesThatClashWithTheCatalogAreSkippedNotFatal() {
        // As after a catalog update made a user-added package built in, or a hand-edited store.
        val store = FakeStore(
            mapOf(
                "osmand" to listOf(google, "net.osmand.patched"),
                "comaps" to listOf("net.osmand.patched", "Bad Name"),
                "no-such-app" to listOf("com.example.maps"),
            ),
        )
        val catalog = effective(store = store).current

        assertEquals("google-maps", catalog.entryForPackage(google)!!.appId)
        assertEquals("osmand", catalog.entryForPackage("net.osmand.patched")!!.appId)
        assertEquals(mapOf("osmand" to listOf("net.osmand.patched")), catalog.userAddedByApp())
        assertNull(catalog.entryForPackage("com.example.maps"))
        catalog.ruleFamilies()
    }

    @Test
    fun withoutUserAddedPackagesTheCatalogIsTheBuiltInOne() {
        assertEquals(bundled, effective().current)
        assertEquals(bundled.forGooglePlay(), effective(googlePlay = true).current)
    }

    private fun enablement(catalog: EffectiveCatalog) =
        EnabledAppRepository(db.appEnablementDao(), catalog.catalog, EpochClock { 1_000L })

    /** Posts one French turn from [pkg] through the privacy gate; returns what the rules made of it. */
    private fun TestScope.dispatch(repo: EnabledAppRepository, families: RuleFamilies, pkg: String): List<RuleEvaluation> {
        val engine = RuleEngine(families = families)
        val rules = LayeredRules(bundled = RulesetCodec.parse(resource("/rules/bundled/google-maps/fr.json")).rules)
        val handled = mutableListOf<RuleEvaluation>()
        val processor = object : NotificationProcessor {
            override suspend fun onPosted(event: PostedNotification) {
                handled += engine.evaluate(event.snapshot, rules, "fr")
            }
            override suspend fun onRemoved(packageName: String) = Unit
        }
        val dispatcher = NotificationDispatcher(repo, SerialProcessingQueue(CoroutineScope(UnconfinedTestDispatcher(testScheduler))), processor)
        dispatcher.onPosted(pkg) {
            PostedNotification(
                snapshot = NotificationSnapshot(packageName = pkg, notificationId = 1, title = "Tournez à droite", text = "200 m"),
                notificationKey = "key",
                tag = null,
                receivedAtMillis = 20,
            )
        }
        testScheduler.advanceUntilIdle()
        return handled
    }
}
