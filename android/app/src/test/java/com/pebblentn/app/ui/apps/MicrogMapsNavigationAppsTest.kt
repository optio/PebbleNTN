package com.pebblentn.app.ui.apps

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasScrollAction
import androidx.test.core.app.ApplicationProvider
import com.pebblentn.app.catalog.NavigationAppCatalog
import com.pebblentn.app.catalog.supportedNotInstalled
import com.pebblentn.app.data.AppEnablement
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
 * MicroG Maps in Navigation apps (#113): on GitHub, F-Droid and Obtainium installs it is its own row,
 * linked to the patch project's website; on a Google Play install it appears nowhere. The lists are
 * built from the real bundled catalog the way MainActivity builds them.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class MicrogMapsNavigationAppsTest {

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

    private val website = "https://github.com/fangkampanat/gmaps-patches"

    private val opened = mutableListOf<String>()
    private val stores = mutableListOf<String>()
    private val viewedRules = mutableListOf<String>()

    private fun show(catalog: NavigationAppCatalog, installed: List<AppEnablement>) {
        compose.setContent {
            NavigationAppsScreen(
                apps = installed,
                onToggle = { _, _ -> },
                onBack = {},
                notInstalled = supportedNotInstalled(catalog, installed.map { it.appId }.toSet()),
                onGetApp = { stores += it },
                onOpenWebsite = { opened += it },
                onViewRules = { viewedRules += it },
                languagesByApp = mapOf("google-maps" to listOf("en", "fr")),
            )
        }
    }

    private fun clickLabel(label: String) =
        SemanticsMatcher("click label '$label'") { it.config.getOrNull(SemanticsActions.OnClick)?.label == label }

    private fun scrollTo(text: String) {
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(text))
    }

    @Test
    fun onGithubAnUninstalledMicrogMapsLinksToItsWebsite() {
        show(bundled, installed = emptyList())
        scrollTo("MicroG Maps")
        compose.onNode(clickLabel("Open the MicroG Maps website")).assertExists()
        compose.onNodeWithText("MicroG Maps").performClick()
        assertEquals(listOf(website), opened)
        assertTrue("never Google Play for MicroG Maps", stores.isEmpty())
        // Its directions line comes from the Google Maps rules.
        compose.onAllNodesWithText("Directions in", substring = true).fetchSemanticsNodes().let { assertTrue(it.isNotEmpty()) }
    }

    @Test
    fun onGithubOtherAppsStillLinkToGooglePlay() {
        show(bundled, installed = emptyList())
        scrollTo("Waze")
        compose.onNode(clickLabel("Get Waze")).assertExists()
        compose.onNodeWithText("Waze").performClick()
        assertEquals(listOf("com.waze"), stores)
        assertTrue(opened.isEmpty())
    }

    @Test
    fun onGithubAnInstalledMicrogMapsIsItsOwnRowShowingTheGoogleMapsRules() {
        val microg = AppEnablement(
            appId = "microg-maps",
            displayName = "MicroG Maps",
            packageNames = listOf("app.morphe.android.apps.maps"),
            enabled = true,
            captureOnly = false,
            rulesAppId = "google-maps",
        )
        show(bundled, installed = listOf(microg))
        compose.onNode(hasText("MicroG Maps") and hasText("Directions in", substring = true)).assertExists()
        compose.onNodeWithText("View its rules").performClick()
        assertEquals(listOf("google-maps"), viewedRules)
    }

    @Test
    fun debugHistoryLabelsMicrogEventsMicrogMapsExceptOnPlay() {
        val pkg = "app.morphe.android.apps.maps"
        assertEquals("MicroG Maps", com.pebblentn.app.ui.format.DisplayLabels.appName(bundled, pkg))
        assertEquals("Google Maps", com.pebblentn.app.ui.format.DisplayLabels.appName(bundled.forGooglePlay(), pkg))
    }

    @Test
    fun onPlayMicrogMapsAndItsLinkAppearNowhere() {
        val play = bundled.forGooglePlay()
        assertTrue(supportedNotInstalled(play, emptySet()).none { it.appId == "microg-maps" || it.website != null })
        show(play, installed = emptyList())
        compose.onAllNodesWithText("MicroG", substring = true).assertCountEquals(0)
        compose.onAllNodes(clickLabel("Open the MicroG Maps website")).assertCountEquals(0)
        scrollTo("Google Maps")
        compose.onNodeWithText("Google Maps").performClick()
        assertEquals(listOf("com.google.android.apps.maps"), stores)
        assertTrue(opened.isEmpty())
    }
}
