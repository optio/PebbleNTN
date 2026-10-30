package com.pebblentn.app.ui.apps

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
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
 * Vela Maps in Navigation apps (#120): on GitHub, F-Droid and Obtainium installs it is listed with
 * its rules, Packages link and website; on a Google Play install it appears nowhere. The lists are
 * built from the real bundled catalog the way MainActivity builds them.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class VelaMapsNavigationAppsTest {

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

    private val website = "https://github.com/PimpinPumpkin/Vela"

    private val opened = mutableListOf<String>()
    private val stores = mutableListOf<String>()
    private val packages = mutableListOf<String>()

    private fun show(catalog: NavigationAppCatalog, installed: List<AppEnablement>) {
        compose.setContent {
            NavigationAppsScreen(
                apps = installed,
                onToggle = { _, _ -> },
                onBack = {},
                notInstalled = supportedNotInstalled(catalog, installed.map { it.appId }.toSet()),
                onGetApp = { stores += it },
                onOpenWebsite = { opened += it },
                onViewRules = {},
                languagesByApp = mapOf("vela-maps" to listOf("en")),
                onOpenPackages = { packages += it },
            )
        }
    }

    private fun clickLabel(label: String) =
        SemanticsMatcher("click label '$label'") { it.config.getOrNull(SemanticsActions.OnClick)?.label == label }

    private fun scrollTo(text: String) {
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(text))
    }

    @Test
    fun onGithubAnUninstalledVelaMapsLinksToItsWebsiteAndPackages() {
        show(bundled, installed = emptyList())
        scrollTo("Vela Maps")
        compose.onNode(clickLabel("Open the Vela Maps website")).assertExists()
        compose.onNodeWithText("Vela Maps").performClick()
        assertEquals(listOf(website), opened)
        assertTrue("never Google Play for Vela Maps", stores.isEmpty())
        compose.onAllNodesWithContentDescription("Package names for Vela Maps").fetchSemanticsNodes()
            .let { assertEquals(1, it.size) }
    }

    @Test
    fun onPlayVelaMapsAndItsLinksAppearNowhere() {
        val play = bundled.forGooglePlay()
        assertTrue(supportedNotInstalled(play, emptySet()).none { it.appId == "vela-maps" || it.website != null })
        show(play, installed = emptyList())
        compose.onAllNodesWithText("Vela", substring = true).assertCountEquals(0)
        compose.onAllNodes(clickLabel("Open the Vela Maps website")).assertCountEquals(0)
        compose.onAllNodesWithContentDescription("Package names for Vela Maps").assertCountEquals(0)
        // MicroG Maps stays hidden too, Google Maps still links to Google Play.
        compose.onAllNodesWithText("MicroG", substring = true).assertCountEquals(0)
        scrollTo("Google Maps")
        compose.onNodeWithText("Google Maps").performClick()
        assertEquals(listOf("com.google.android.apps.maps"), stores)
        assertTrue(opened.isEmpty())
    }
}
