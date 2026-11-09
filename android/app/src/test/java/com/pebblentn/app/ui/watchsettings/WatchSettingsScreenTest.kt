package com.pebblentn.app.ui.watchsettings

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.pebblentn.app.core.WatchCapabilities
import com.pebblentn.app.core.WatchPlatform
import com.pebblentn.app.core.WatchSetting
import com.pebblentn.app.core.WatchSettingsSyncState
import com.pebblentn.app.protocol.Protocol
import com.pebblentn.app.protocol.WatchMessages
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

/** The Watch settings screens in Compose (REQ-ANDROID-021, #102 FR5/FR6). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class WatchSettingsScreenTest {

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

    private val basalt = WatchCapabilities(Protocol.CapabilityMasks.COLOR_MASK)

    @Test
    fun beforeAnySettingsTheScreenAsksToOpenTheWatchapp() {
        compose.setContent {
            WatchSettingsScreen(WatchSettingsSyncState(), watchappOpen = false, onOpenSetting = {}, onBack = {})
        }
        compose.onNodeWithText("Open PebbleNTN on your watch to load its settings").assertExists()
        compose.onNodeWithText("Colour").assertDoesNotExist()
    }

    @Test
    fun anOldWatchappAsksForAnUpdate() {
        compose.setContent {
            WatchSettingsScreen(WatchSettingsSyncState(watchappOutdated = true), watchappOpen = true, onOpenSetting = {}, onBack = {})
        }
        compose.onNodeWithText("Update the watchapp to change these here").assertExists()
    }

    @Test
    fun aBasaltShowsNoBacklightColourOrTouchRows() {
        val state = WatchSettingsSyncState(
            mirror = WatchMessages.settings(1, platform = WatchPlatform.BASALT, caps = basalt),
            receivedFromWatch = true,
        )
        var opened: WatchSetting? = null
        compose.setContent { WatchSettingsScreen(state, watchappOpen = true, onOpenSetting = { opened = it }, onBack = {}) }
        compose.onNodeWithText("Backlight colour").assertDoesNotExist()
        compose.onNodeWithText("Touch").assertDoesNotExist()
        compose.onNodeWithText("Up button").performScrollTo().assertExists()
        compose.onNodeWithText("Glyph pack").performScrollTo().performClick()
        assertEquals(WatchSetting.GLYPH_PACK, opened)
    }

    @Test
    fun anEmeryShowsBacklightColourAndTouch() {
        val state = WatchSettingsSyncState(mirror = WatchMessages.settings(1), receivedFromWatch = true)
        compose.setContent { WatchSettingsScreen(state, watchappOpen = false, onOpenSetting = {}, onBack = {}) }
        compose.onNodeWithText("Backlight colour").performScrollTo().assertExists()
        compose.onNodeWithText("Touch").performScrollTo().assertExists()
        compose.onNodeWithText("Will apply when the watchapp next opens").assertDoesNotExist()
    }

    @Test
    fun aBlackAndWhiteColourListOffersBlackAndWhiteWithAGreyPreview() {
        val bw = WatchMessages.settings(1, platform = WatchPlatform.APLITE, caps = WatchCapabilities.NONE)
        compose.setContent {
            OptionListScreen(WatchSetting.ACCENT, bw, queued = false, onSelect = {}, onBack = {})
        }
        compose.onNodeWithText("Black").assertIsSelected()
        compose.onNodeWithText("White").assertExists()
        compose.onNodeWithText("Green").assertDoesNotExist()
        compose.onNodeWithContentDescription("Preview: Black").assertExists()
    }

    @Test
    fun thePreviewNamesTheSelectedValueAndPickingOneReportsIt() {
        val model = WatchMessages.settings(1, mapOf(WatchSetting.INVERT to 1), platform = WatchPlatform.CHALK)
        var picked: Int? = null
        compose.setContent {
            OptionListScreen(WatchSetting.INVERT, model, queued = true, onSelect = { picked = it }, onBack = {})
        }
        compose.onNodeWithContentDescription("Preview: Light on dark").assertExists()
        compose.onNodeWithText("Will apply when the watchapp next opens").assertExists()
        compose.onNodeWithText("Dark on light").performClick()
        assertEquals(0, picked)
    }

    @Test
    fun aNonVisualSettingHasNoPreview() {
        compose.setContent {
            OptionListScreen(WatchSetting.VIBE_PATTERN, WatchMessages.settings(1), queued = false, onSelect = {}, onBack = {})
        }
        compose.onNodeWithText("Off").assertIsSelected()
        compose.onNodeWithContentDescription("Preview: Off").assertDoesNotExist()
    }

    // --- When driving (#132 FR3, FR4) ------------------------------------------------------------

    private fun drivingState(minor: Int?, overrides: Map<WatchSetting, Int> = emptyMap()) = WatchSettingsSyncState(
        mirror = WatchMessages.settings(1, overrides),
        receivedFromWatch = true,
        watchProtocolMinor = minor,
    )

    @Test
    fun theDrivingSectionIsHiddenForAProtocol13Watchapp() {
        compose.setContent { WatchSettingsScreen(drivingState(minor = 3), watchappOpen = true, onOpenSetting = {}, onBack = {}) }
        compose.onNodeWithTag(DrivingSectionTags.SECTION).assertDoesNotExist()
        compose.onNodeWithText("Hide directions when driving").assertDoesNotExist()
    }

    @Test
    fun theDrivingSectionShowsForAProtocol14WatchappAndTheSwitchHidesDirections() {
        val changes = mutableListOf<Pair<WatchSetting, Int>>()
        compose.setContent {
            WatchSettingsScreen(
                drivingState(minor = 4),
                watchappOpen = true,
                onOpenSetting = {},
                onBack = {},
                onChangeSetting = { setting, value -> changes += setting to value },
            )
        }
        compose.onNodeWithTag(DrivingSectionTags.SWITCH).performScrollTo().assertIsOff().performClick()
        assertEquals(listOf(WatchSetting.DRIVING_MODE to 1), changes)
        compose.onNodeWithText("Above 25 km/h").performScrollTo().assertExists()
        compose.onNodeWithText(
            "Navigation apps don't tell PebbleNTN whether you're driving, walking or cycling",
            substring = true,
        ).performScrollTo().assertExists()
    }

    @Test
    fun theSpeedSliderIsGreyedOutWhileTheSwitchIsOff() {
        compose.setContent { WatchSettingsScreen(drivingState(minor = 4), watchappOpen = true, onOpenSetting = {}, onBack = {}) }
        compose.onNodeWithTag(DrivingSectionTags.SLIDER).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun theSpeedSliderIsEnabledWhileHiding() {
        compose.setContent {
            WatchSettingsScreen(
                drivingState(minor = 4, mapOf(WatchSetting.DRIVING_MODE to 1)),
                watchappOpen = true,
                onOpenSetting = {},
                onBack = {},
            )
        }
        compose.onNodeWithTag(DrivingSectionTags.SWITCH).performScrollTo().assertIsOn()
        compose.onNodeWithTag(DrivingSectionTags.SLIDER).performScrollTo().assertIsEnabled()
    }

    @Test
    fun theResetButtonShowsOnlyWhenTheSpeedIsNotTheDefault() {
        var state by mutableStateOf(drivingState(minor = 4, mapOf(WatchSetting.DRIVING_MODE to 1)))
        val changes = mutableListOf<Pair<WatchSetting, Int>>()
        compose.setContent {
            WatchSettingsScreen(state, watchappOpen = true, onOpenSetting = {}, onBack = {}, onChangeSetting = { s, v -> changes += s to v })
        }
        compose.onNodeWithTag(DrivingSectionTags.RESET).assertDoesNotExist()

        state = drivingState(minor = 4, mapOf(WatchSetting.DRIVING_MODE to 1, WatchSetting.DRIVING_SPEED_METRIC to 7))
        compose.onNodeWithText("Above 45 km/h").performScrollTo().assertExists()
        compose.onNodeWithText("Reset to default (25 km/h)").performScrollTo().performClick()
        assertEquals(listOf(WatchSetting.DRIVING_SPEED_METRIC to 3), changes)
    }

    @Test
    fun withImperialUnitsTheSpeedIsInMph() {
        compose.setContent {
            WatchSettingsScreen(
                drivingState(minor = 4, mapOf(WatchSetting.DRIVING_MODE to 1, WatchSetting.UNITS to 1, WatchSetting.DRIVING_SPEED_IMPERIAL to 4)),
                watchappOpen = true,
                onOpenSetting = {},
                onBack = {},
            )
        }
        compose.onNodeWithText("Above 25 mph").performScrollTo().assertExists()
        compose.onNodeWithText("Reset to default (15 mph)").performScrollTo().assertExists()
    }
}
