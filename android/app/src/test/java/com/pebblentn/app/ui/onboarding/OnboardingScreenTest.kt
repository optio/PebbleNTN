package com.pebblentn.app.ui.onboarding

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.pebblentn.app.R
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

/** REQ-SEC-004 and REQ-ANDROID-022 (FR2d): the Play disclosure has no GitHub-check sentence. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class OnboardingScreenTest {

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

    @Test
    fun eachSourcePicksItsDisclosure() {
        assertEquals(R.string.onboarding_disclosure_play, onboardingDisclosure(InstallSource.PLAY))
        for (source in listOf(InstallSource.GITHUB, InstallSource.FDROID, InstallSource.OBTAINIUM)) {
            assertEquals("$source", R.string.onboarding_disclosure, onboardingDisclosure(source))
        }
    }

    @Test
    fun playDisclosureNamesGooglePlayAndNotGitHub() {
        compose.setContent { OnboardingScreen(accessGranted = false, onOpenSettings = {}, installSource = InstallSource.PLAY) }
        compose.onNodeWithText("Google Play delivers the updates", substring = true).assertExists()
        compose.onNodeWithText("makes no network connections of its own", substring = true).assertExists()
        compose.onNodeWithText("Processing occurs on this device", substring = true).assertExists()
        compose.onAllNodesWithText("GitHub", substring = true).assertCountEquals(0)
    }

    @Test
    fun gitHubDisclosureKeepsTheGitHubSentence() {
        compose.setContent { OnboardingScreen(accessGranted = false, onOpenSettings = {}) }
        compose.onNodeWithText("It uses the network only for the optional GitHub check", substring = true).assertExists()
        compose.onAllNodesWithText("Google Play", substring = true).assertCountEquals(0)
    }
}
