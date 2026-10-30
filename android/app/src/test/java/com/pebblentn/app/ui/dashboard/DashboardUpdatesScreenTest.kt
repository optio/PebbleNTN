package com.pebblentn.app.ui.dashboard

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.pebblentn.app.update.InstallKind
import com.pebblentn.app.update.InstallRecord
import com.pebblentn.app.update.InstallSource
import org.junit.Assert.assertEquals
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
 * REQ-ANDROID-022 (FR2a, FR2c): a Google Play install shows no GitHub update control, and its update
 * card points to Google Play only; a GitHub install keeps v0.0.77's controls. REQ-ANDROID-020 (#119): the
 * "Phone app version" row is shown for every source, also while the stale card is at the top.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class DashboardUpdatesScreenTest {

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

    private fun show(
        source: InstallSource,
        record: InstallRecord = InstallRecord.UNKNOWN,
        prompt: UpdatePrompt = UpdatePrompt.NONE,
        updateAvailable: Boolean = false,
        onOpenSource: (InstallSource) -> Unit = {},
    ) {
        compose.setContent {
            DashboardScreen(
                accessGranted = true,
                lastEligibleAtMillis = 1L,
                installSource = source,
                installRecord = record,
                updatePrompt = prompt,
                updateAvailable = updateAvailable,
                latestVersion = "0.0.78",
                appVersion = "0.0.77",
                onOpenUpdateSource = onOpenSource,
            )
        }
    }

    @Test
    fun playShowsNoGitHubControlsEvenWhenAskedForTheStaleItem() {
        show(InstallSource.PLAY, record = playUpdate, prompt = UpdatePrompt.STALE_CHECK)
        compose.onAllNodesWithText("Check for updates").assertCountEquals(0)
        compose.onAllNodesWithText("Check for updates now").assertCountEquals(0)
        compose.onAllNodesWithText("Check for updates weekly").assertCountEquals(0)
        compose.onAllNodesWithText("GitHub", substring = true).assertCountEquals(0)
        compose.onNodeWithText("0.0.77, updated by Google Play").assertExists()
    }

    @Test
    fun playShowsNoWeeklyQuestion() {
        show(InstallSource.PLAY, prompt = UpdatePrompt.WEEKLY_QUESTION)
        compose.onAllNodesWithText("Check for updates automatically every week?").assertCountEquals(0)
    }

    @Test
    fun gitHubKeepsTheCheckRowTheToggleAndTheStaleItem() {
        show(InstallSource.GITHUB, record = manualInstall, prompt = UpdatePrompt.STALE_CHECK)
        // The stale card's title and button.
        compose.onAllNodesWithText("Check for updates").assertCountEquals(2)
        compose.onNodeWithText("Check for updates weekly").assertExists()
        compose.onAllNodesWithText("0.0.77, updated by Google Play").assertCountEquals(0)
        // The version row stays while the check row is replaced by the stale card.
        compose.onAllNodesWithText("Check for updates now").assertCountEquals(0)
        compose.onNodeWithText("Phone app version").assertExists()
        compose.onNodeWithText("0.0.77, installed manually").assertExists()
    }

    @Test
    fun gitHubWithoutAStaleCheckShowsTheCheckRow() {
        show(InstallSource.GITHUB, record = manualInstall)
        compose.onNodeWithText("Phone app version").assertExists()
        compose.onNodeWithText("0.0.77, installed manually").assertExists()
        compose.onNodeWithText("Check for updates now").assertExists()
        compose.onNodeWithText("Compare with the latest GitHub release").assertExists()
        compose.onNodeWithText("Check for updates weekly").assertExists()
        // The version is on its own row, not repeated under the check row.
        compose.onAllNodesWithText("Version 0.0.77").assertCountEquals(0)
    }

    @Test
    fun anObtainiumUpdateNamesObtainium() {
        show(InstallSource.OBTAINIUM, record = InstallRecord(InstallKind.OBTAINIUM, "dev.imranr.obtainium", firstInstallTime = 1L, lastUpdateTime = 2L))
        compose.onNodeWithText("0.0.77, updated by Obtainium").assertExists()
    }

    @Test
    fun aFailedLookupShowsOnlyTheVersion() {
        show(InstallSource.GITHUB, record = InstallRecord.UNKNOWN)
        compose.onNodeWithText("0.0.77").assertExists()
    }

    @Test
    fun playUpdateCardOpensGooglePlayAndOffersNoOtherSource() {
        var opened: InstallSource? = null
        show(InstallSource.PLAY, updateAvailable = true, onOpenSource = { opened = it })
        compose.onNodeWithText("A new version can take a day or two to reach Google Play.", substring = true).assertExists()
        compose.onAllNodesWithText("uninstall", substring = true).assertCountEquals(0)
        compose.onAllNodesWithText("Other sources").assertCountEquals(0)
        compose.onNodeWithText("Update from Google Play").performClick()
        assertEquals(InstallSource.PLAY, opened)
    }

    @Test
    fun gitHubUpdateCardKeepsOtherSourcesAndTheUninstallHint() {
        show(InstallSource.GITHUB, updateAvailable = true)
        compose.onNodeWithText("Update from GitHub").assertExists()
        compose.onNodeWithText("Other sources").assertExists()
        compose.onNodeWithText("uninstall PebbleNTN first", substring = true).assertExists()
        compose.onAllNodesWithText("Google Play", substring = true).assertCountEquals(0)
    }

    private val playUpdate = InstallRecord(InstallKind.PLAY, "com.android.vending", firstInstallTime = 1L, lastUpdateTime = 2L)
    private val manualInstall = InstallRecord(InstallKind.MANUAL, null, firstInstallTime = 1L, lastUpdateTime = 1L)
}
