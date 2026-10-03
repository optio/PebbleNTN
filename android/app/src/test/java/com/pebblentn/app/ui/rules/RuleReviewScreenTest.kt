package com.pebblentn.app.ui.rules

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.rules.ExternalResource
import org.robolectric.Shadows.shadowOf
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pebblentn.app.data.OverrideStatus
import com.pebblentn.app.data.RuleOverride
import com.pebblentn.app.data.RuleValidationStatus
import com.pebblentn.app.data.UserRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** REQ-ANDROID-016 (#58): reverting asks about sharing first, and only touches overlapping rules. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class RuleReviewScreenTest {

    val compose = createComposeRule()

    /**
     * The compose rule hosts its content in a ComponentActivity, which only debug builds declare
     * (ui-test-manifest is a debugImplementation). Register it first so release unit tests run too.
     */
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

    private fun user(id: String) = UserRule(id, null, "app.comaps.google", null, "{}", true, RuleValidationStatus.VALID, 0)

    private val users = listOf(user("comaps-navigation-step"), user("my-rule"), user("unrelated"))
    private val overrides = mapOf(
        "comaps-navigation-step" to RuleOverride("comaps-navigation-step", listOf("comaps-navigation-step"), OverrideStatus.OFFICIAL_UPDATED, "fp", needsAttention = true),
        "my-rule" to RuleOverride("my-rule", listOf("google-maps-turn-right-en"), OverrideStatus.TAKES_OVER, null, needsAttention = false),
    )

    private var reverted: List<String>? = null
    private var shared = 0
    private var kept: RuleOverride? = null

    private fun show() = compose.setContent {
        RuleReviewScreen(
            userRules = users,
            overrides = overrides,
            onRevert = { reverted = it },
            onKeep = { kept = it },
            onCompare = {},
            onShareRules = { shared++ },
            onBack = {},
        )
    }

    @Test
    fun revertAllAsksToShareFirstAndRemovesOnlyOverlappingRules() {
        show()
        compose.onNodeWithText("Revert all 2 overlapping rules").performClick()
        compose.onNodeWithText("Share your rules first?").assertExists()
        compose.onNodeWithText("Revert without sharing").performClick()
        assertEquals(listOf("comaps-navigation-step", "my-rule"), reverted)
    }

    @Test
    fun shareFirstOpensSharingWithoutReverting() {
        show()
        compose.onNodeWithText("Revert all 2 overlapping rules").performClick()
        compose.onNodeWithText("Share first").performClick()
        assertEquals(1, shared)
        assertNull(reverted)
    }

    @Test
    fun keepMineIsOfferedOnlyForANewerOfficialRule() {
        show()
        compose.onNodeWithText("Official rule is newer").assertExists()
        compose.onNodeWithText("Keep mine").performClick()
        assertEquals("comaps-navigation-step", kept?.userRuleId)
        compose.onNodeWithText("unrelated").assertDoesNotExist()
    }
}
