package com.pebblentn.app.ui.apps

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.pebblentn.app.catalog.NavigationAppEntry
import com.pebblentn.app.data.AppEnablement
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** An uninstalled app is listed under "Also supported" like any other, not under Installed (#88). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class NavigationAppsScreenTest {

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

    private fun entry(id: String, name: String) = NavigationAppEntry(id, name, listOf("pkg.$id"), false, true, true)

    @Test
    fun anUninstalledAppIsListedUnderAlsoSupportedWithoutALabel() {
        compose.setContent {
            NavigationAppsScreen(
                apps = listOf(AppEnablement("osmand", "OsmAnd", listOf("net.osmand"), enabled = true, captureOnly = true)),
                onToggle = { _, _ -> },
                onBack = {},
                notInstalled = listOf(entry("amap", "Amap (Gaode)"), entry("baidu", "Baidu Maps")),
            )
        }
        compose.onNodeWithText("Also supported").assertExists()
        compose.onNodeWithText("Amap (Gaode)").assertExists()
        compose.onAllNodesWithText("No longer installed").assertCountEquals(0)
    }
}
