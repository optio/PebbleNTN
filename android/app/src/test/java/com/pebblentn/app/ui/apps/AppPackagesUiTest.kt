package com.pebblentn.app.ui.apps

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.pebblentn.app.catalog.EffectiveCatalog
import com.pebblentn.app.catalog.NavigationAppCatalog
import com.pebblentn.app.catalog.supportedNotInstalled
import com.pebblentn.app.data.AppEnablement
import com.pebblentn.app.data.CustomPackagesRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * FR4 and FR5 in the UI: the "Packages" link on every app with rules, installed or under "Also
 * supported" (never MicroG Maps on Google Play), and the Packages screen's list, add, remove and
 * refusal messages. Built from the real bundled catalog the way MainActivity builds it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class AppPackagesUiTest {

    val compose = createComposeRule()

    /** See RuleReviewScreenTest: register the host activity so release unit tests run too. */
    @get:Rule
    val rules: TestRule = RuleChain
        .outerRule(object : ExternalResource() {
            override fun before() {
                val context = ApplicationProvider.getApplicationContext<android.content.Context>()
                shadowOf(context.packageManager)
                    .addActivityIfNotPresent(ComponentName(context, ComponentActivity::class.java))
            }
        })
        .around(compose)

    private val bundled: NavigationAppCatalog = NavigationAppCatalog.parse(
        javaClass.getResourceAsStream("/catalog/navigation-apps.json")!!.bufferedReader().use { it.readText() },
    )

    private val googleMaps = AppEnablement(
        appId = "google-maps",
        displayName = "Google Maps",
        packageNames = listOf("com.google.android.apps.maps"),
        enabled = true,
        captureOnly = false,
    )

    private val opened = mutableListOf<String>()

    private fun showApps(catalog: NavigationAppCatalog, installed: List<AppEnablement>) {
        compose.setContent {
            NavigationAppsScreen(
                apps = installed,
                onToggle = { _, _ -> },
                onBack = {},
                notInstalled = supportedNotInstalled(catalog, installed.map { it.appId }.toSet()),
                onOpenPackages = { opened += it },
            )
        }
    }

    private fun link(app: String) = "Package names for $app"

    private fun scrollTo(description: String) {
        compose.onNode(hasScrollAction()).performScrollToNode(hasContentDescription(description))
    }

    // --- FR4: the link ----------------------------------------------------------------------------

    @Test
    fun anInstalledAppHasAPackagesLinkNextToItsRules() {
        showApps(bundled, listOf(googleMaps))
        compose.onNodeWithText("View its rules").assertExists()
        assertTrue(compose.onAllNodesWithText("Packages", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
        compose.onNodeWithContentDescription(link("Google Maps")).performClick()
        assertEquals(listOf("google-maps"), opened)
    }

    @Test
    fun everyAlsoSupportedAppWithRulesHasTheLinkButNotACaptureOnlyOne() {
        showApps(bundled, listOf(googleMaps))
        for (app in listOf("MicroG Maps", "OsmAnd", "Organic Maps", "CoMaps")) {
            scrollTo(link(app))
            compose.onNodeWithContentDescription(link(app)).assertExists()
        }
        compose.onNodeWithContentDescription(link("OsmAnd")).performClick()
        assertEquals(listOf("osmand"), opened)
        compose.onAllNodesWithContentDescription(link("Waze")).assertCountEquals(0)
    }

    @Test
    fun onPlayMicrogMapsHasNoLinkAndGoogleMapsHasOne() {
        showApps(bundled.forGooglePlay(), installed = emptyList())
        compose.onAllNodesWithContentDescription(link("MicroG Maps")).assertCountEquals(0)
        compose.onAllNodesWithText("MicroG", substring = true).assertCountEquals(0)
        scrollTo(link("Google Maps"))
        compose.onNodeWithContentDescription(link("Google Maps")).performClick()
        assertEquals(listOf("google-maps"), opened)
    }

    // --- FR5: the screen --------------------------------------------------------------------------

    private fun showPackages(effective: EffectiveCatalog, appId: String) {
        compose.setContent {
            val catalog by effective.catalog.collectAsState()
            AppPackagesScreen(
                appName = effective.displayNameOf(appId),
                builtIn = effective.builtInPackagesOf(appId),
                userAdded = catalog.userAddedByApp()[appId].orEmpty(),
                onAdd = { effective.add(appId, it) },
                onRemove = { effective.remove(appId, it) },
                onBack = {},
                ownerName = effective::displayNameOf,
            )
        }
    }

    private fun effective(googlePlay: Boolean = false) = EffectiveCatalog(
        bundled,
        googlePlay,
        CustomPackagesRepository(ApplicationProvider.getApplicationContext<android.content.Context>()),
    )

    private fun type(text: String) {
        compose.onNode(hasSetTextAction()).performTextInput(text)
        compose.onNodeWithText("Add").performClick()
    }

    @Test
    fun theScreenListsTheBuiltInPackagesLocked() {
        showPackages(effective(), "osmand")
        compose.onNodeWithText("OsmAnd packages").assertExists()
        for (pkg in listOf("net.osmand", "net.osmand.plus", "net.osmand.dev", "net.osmand.huawei")) {
            compose.onNodeWithText(pkg).assertExists()
        }
        compose.onAllNodesWithText("Built in, can't be removed").assertCountEquals(4)
        compose.onAllNodesWithContentDescription("Remove", substring = true).assertCountEquals(0)
        compose.onNodeWithText("No package names added yet.").assertExists()
    }

    @Test
    fun addingAndRemovingAPackage() {
        val effective = effective()
        showPackages(effective, "microg-maps")
        type(" App.ReVanced.android.apps.maps ")

        // Trimmed, its case kept (package names are case-sensitive).
        compose.onNodeWithText("App.ReVanced.android.apps.maps").assertExists()
        compose.onNodeWithText("Yours, can be removed").assertExists()
        assertEquals(listOf("App.ReVanced.android.apps.maps"), effective.userAddedPackagesOf("microg-maps"))
        // The field is cleared for the next one.
        compose.onNode(hasSetTextAction() and hasText("App.ReVanced", substring = true)).assertDoesNotExist()

        // The same name in another case is a duplicate.
        type("app.revanced.android.apps.maps")
        compose.onNodeWithText("This package name is already added.").assertExists()
        compose.onNode(hasSetTextAction()).performTextClearance()

        compose.onNodeWithContentDescription("Remove App.ReVanced.android.apps.maps").performClick()
        compose.onNodeWithText("App.ReVanced.android.apps.maps").assertDoesNotExist()
        assertTrue(effective.userAddedPackagesOf("microg-maps").isEmpty())
        compose.onNodeWithText("No package names added yet.").assertExists()
    }

    @Test
    fun aDuplicateIsRefusedNamingTheAppThatUsesIt() {
        val effective = effective()
        effective.add("osmand", "net.osmand.patched")
        showPackages(effective, "microg-maps")

        type("com.google.android.apps.maps")
        compose.onNodeWithText("This is a default package name of Google Maps. A package name can belong to only one app.").assertExists()
        assertTrue(effective.userAddedPackagesOf("microg-maps").isEmpty())
    }

    @Test
    fun aPackageAddedToAnotherAppOrMalformedIsRefused() {
        val effective = effective()
        effective.add("osmand", "net.osmand.patched")
        showPackages(effective, "comaps")

        type("net.osmand.patched")
        compose.onNodeWithText("This package name is already added to OsmAnd. A package name can belong to only one app.").assertExists()

        compose.onNode(hasSetTextAction()).performTextInput("x")
        compose.onAllNodesWithText("already added to OsmAnd", substring = true).assertCountEquals(0)

        compose.onNode(hasSetTextAction()).performTextInput(" maps")
        compose.onNodeWithText("Add").performClick()
        compose.onNodeWithText("This isn't a package name", substring = true).assertExists()
        assertTrue(effective.userAddedPackagesOf("comaps").isEmpty())
    }

    @Test
    fun onPlayGoogleMapsListsTheJoinedPackageAsBuiltInWithoutNamingMicrog() {
        showPackages(effective(googlePlay = true), "google-maps")
        compose.onNodeWithText("com.google.android.apps.maps").assertExists()
        compose.onNodeWithText("app.morphe.android.apps.maps").assertExists()
        type("app.morphe.android.apps.maps")
        compose.onNodeWithText("This is already one of the default package names.").assertExists()
        compose.onAllNodesWithText("MicroG", substring = true).assertCountEquals(0)
    }
}
