package com.pebblentn.app.ui.watchsettings

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.pebblentn.app.R
import com.pebblentn.app.core.WatchCapabilities
import com.pebblentn.app.core.WatchPlatform
import com.pebblentn.app.core.WatchSetting
import com.pebblentn.app.core.WatchSettingsSyncState
import com.pebblentn.app.protocol.Protocol
import com.pebblentn.app.protocol.WatchMessages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
import org.robolectric.annotation.GraphicsMode

/** The Watch settings screens in Compose (REQ-ANDROID-021, #102 FR5/FR6; help texts #139). */
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

    // --- Directions text (#134 FR7.2, FR7.4) -----------------------------------------------------

    @Test
    fun directionsTextShowsForA13WatchappWithItsValueAndExplanation() {
        val state = WatchSettingsSyncState(
            mirror = WatchMessages.settings(1, mapOf(WatchSetting.DIRECTIONS_TEXT to 1)),
            receivedFromWatch = true,
            watchProtocolMinor = 3,
        )
        val opened = mutableListOf<WatchSetting>()
        compose.setContent { WatchSettingsScreen(state, watchappOpen = true, onOpenSetting = { opened += it }, onBack = {}) }
        compose.onNodeWithText("Compact").performScrollTo().assertExists()
        // #139 FR4.1: the old note is gone; the row's own help says what Full and Compact show.
        compose.onNodeWithText("Compact shows only the street name when the app gives one.").assertDoesNotExist()
        compose.onNodeWithTag(WatchSettingsTags.help(WatchSetting.DIRECTIONS_TEXT), useUnmergedTree = true)
            .performScrollTo()
            .assertTextEquals(string(R.string.watch_setting_directions_text_help))
        compose.onNodeWithText("Directions text").performScrollTo().performClick()
        assertEquals(listOf(WatchSetting.DIRECTIONS_TEXT), opened)
    }

    // --- Help texts (#139) ------------------------------------------------------------------------

    private fun string(@StringRes id: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(id, *args)

    private fun stringArray(@ArrayRes id: Int): Array<String> =
        ApplicationProvider.getApplicationContext<android.content.Context>().resources.getStringArray(id)

    private val ready = WatchSettingsSyncState(mirror = WatchMessages.settings(1), receivedFromWatch = true)

    /** FR1.1–FR1.3: every visible row shows its value and, under it, its help line, without a tap. */
    @Test
    fun everyRowShowsItsValueAndItsHelpLine() {
        compose.setContent { WatchSettingsScreen(ready, watchappOpen = true, onOpenSetting = {}, onBack = {}) }
        val mirror = ready.mirror!!
        for (setting in WatchSettingsUi.sections(mirror).flatMap { it.settings }) {
            compose.onNodeWithTag(WatchSettingsTags.help(setting), useUnmergedTree = true)
                .performScrollTo()
                .assertIsDisplayed()
                .assertTextEquals(string(WatchSettingsUi.help(setting)))
        }
        // A representative row per section, its value together with its help.
        for ((setting, value) in listOf(
            WatchSetting.ACCENT to "Green",
            WatchSetting.DIRECTIONS_TEXT to "Full",
            WatchSetting.ETA_MODE to "Arrival time",
            WatchSetting.BACKLIGHT to "Watch default",
        )) {
            compose.onNodeWithTag(WatchSettingsTags.row(setting))
                .performScrollTo()
                .assert(hasText(value))
                .assert(hasText(string(WatchSettingsUi.help(setting))))
        }
    }

    /** FR5.1, FR5.2: a watch without touch or an RGB backlight shows no help about either. */
    @Test
    fun aWatchWithoutTouchOrAnRgbBacklightShowsNoHelpAboutThem() {
        val state = WatchSettingsSyncState(
            mirror = WatchMessages.settings(1, platform = WatchPlatform.BASALT, caps = basalt),
            receivedFromWatch = true,
        )
        compose.setContent { WatchSettingsScreen(state, watchappOpen = true, onOpenSetting = {}, onBack = {}) }
        for (hidden in listOf(WatchSetting.TOUCH, WatchSetting.SHORTCUT_TAP, WatchSetting.SHORTCUT_SWIPE_LEFT, WatchSetting.BACKLIGHT_COLOR)) {
            compose.onNodeWithTag(WatchSettingsTags.help(hidden), useUnmergedTree = true).assertDoesNotExist()
            compose.onNodeWithText(string(WatchSettingsUi.help(hidden))).assertDoesNotExist()
        }
        val gesture = Regex("\\b(swip\\w*|tap\\w*|touch\\w*)\\b", RegexOption.IGNORE_CASE)
        val shown = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .flatMap { it.config.getOrElse(SemanticsProperties.Text) { emptyList() } }
            .map { it.text }
        assertTrue("the screen shows help lines", shown.contains(string(R.string.watch_setting_shortcut_up_help)))
        assertTrue(shown.filter { gesture.containsMatchIn(it) }.toString(), shown.none { gesture.containsMatchIn(it) })
    }

    /** FR4.2–FR4.4: the When driving paragraph and the intro stay; the switch and the slider get help. */
    @Test
    fun theDrivingRowsHaveHelpAndTheDrivingParagraphStays() {
        compose.setContent { WatchSettingsScreen(drivingState(minor = 4), watchappOpen = true, onOpenSetting = {}, onBack = {}) }
        compose.onNodeWithText(string(R.string.watch_settings_explanation)).assertExists()
        compose.onNodeWithTag(DrivingSectionTags.SWITCH_HELP, useUnmergedTree = true)
            .performScrollTo()
            .assertTextEquals(string(R.string.watch_setting_driving_mode_help))
        compose.onNodeWithTag(DrivingSectionTags.SPEED_HELP, useUnmergedTree = true)
            .performScrollTo()
            .assertTextEquals(string(R.string.watch_setting_driving_speed_help))
        compose.onNodeWithText(string(R.string.watch_driving_explanation)).performScrollTo().assertExists()
        assertTrue(string(R.string.watch_setting_driving_speed_help).contains("Distance units"))
    }

    /** FR2.1, FR2.2: the value list starts with the explanation; Backlight duration names the battery. */
    @Test
    fun theBacklightListExplainsTheBatteryCostAboveTheValues() {
        compose.setContent {
            OptionListScreen(WatchSetting.BACKLIGHT, WatchMessages.settings(1), queued = false, onSelect = {}, onBack = {})
        }
        val details = string(R.string.watch_setting_backlight_details)
        assertTrue(details.contains("battery"))
        compose.onNodeWithTag(WatchSettingsTags.DETAILS).assertIsDisplayed().assertTextEquals(details)
        val detailsBottom = compose.onNodeWithTag(WatchSettingsTags.DETAILS).getUnclippedBoundsInRoot().bottom
        assertTrue(detailsBottom <= compose.onNodeWithText("Watch default").getUnclippedBoundsInRoot().top)
    }

    /** FR2.1: a visual setting shows its explanation above the preview. */
    @Test
    fun theColourListExplainsAboveThePreview() {
        compose.setContent {
            OptionListScreen(WatchSetting.ACCENT, WatchMessages.settings(1), queued = false, onSelect = {}, onBack = {})
        }
        compose.onNodeWithTag(WatchSettingsTags.DETAILS).assertTextEquals(string(R.string.watch_setting_accent_details))
        val preview = compose.onNodeWithContentDescription("Preview: Green").assertExists()
        assertTrue(
            compose.onNodeWithTag(WatchSettingsTags.DETAILS).getUnclippedBoundsInRoot().bottom <=
                preview.getUnclippedBoundsInRoot().top,
        )
    }

    /** FR5.3: with miles, the early-warning explanation gives its distance in feet. */
    @Test
    fun withMilesTheEarlyWarningExplanationIsInFeet() {
        compose.setContent {
            OptionListScreen(
                WatchSetting.EARLY_WARN_DISTANCE,
                WatchMessages.settings(1, mapOf(WatchSetting.UNITS to 1)),
                queued = false,
                onSelect = {},
                onBack = {},
            )
        }
        compose.onNodeWithTag(WatchSettingsTags.DETAILS)
            .assertTextEquals(string(R.string.watch_setting_early_warn_distance_details_imperial))
            .assert(hasText("80 ft", substring = true))
    }

    /** FR3.1: each shortcut action says what it does under its name. */
    @Test
    fun theUpButtonListDescribesEveryAction() {
        compose.setContent {
            OptionListScreen(WatchSetting.SHORTCUT_UP, WatchMessages.settings(1), queued = false, onSelect = {}, onBack = {})
        }
        val actions = stringArray(R.array.watch_values_shortcut)
        val descriptions = stringArray(R.array.watch_values_shortcut_descriptions)
        val list = compose.onNode(hasScrollToNodeAction())
        for ((action, description) in actions.zip(descriptions)) {
            list.performScrollToNode(hasText(action))
            compose.onNode(hasText(action) and hasText(description)).assertIsDisplayed()
        }
    }

    /** FR3.2: without an RGB backlight, Next backlight colour and its description are not offered. */
    @Test
    fun withoutAnRgbBacklightTheBacklightColourActionAndItsDescriptionAreAbsent() {
        val bw = WatchMessages.settings(1, platform = WatchPlatform.BASALT, caps = basalt)
        compose.setContent { OptionListScreen(WatchSetting.SHORTCUT_DOWN, bw, queued = false, onSelect = {}, onBack = {}) }
        val descriptions = stringArray(R.array.watch_values_shortcut_descriptions)
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Units"))
        compose.onNode(hasText("Units") and hasText(descriptions[13])).assertIsDisplayed()
        compose.onNodeWithText("Next backlight colour").assertDoesNotExist()
        compose.onNodeWithText(descriptions[12]).assertDoesNotExist()
    }

    /** FR3.3: other lists keep their plain names. */
    @Test
    fun aNonShortcutListHasNoValueDescriptions() {
        compose.setContent {
            OptionListScreen(WatchSetting.BACKLIGHT, WatchMessages.settings(1), queued = false, onSelect = {}, onBack = {})
        }
        for (description in stringArray(R.array.watch_values_shortcut_descriptions)) {
            compose.onNodeWithText(description).assertDoesNotExist()
        }
        val row = compose.onNodeWithText("High - until app closes").fetchSemanticsNode()
        assertEquals(listOf("High - until app closes"), row.config[SemanticsProperties.Text].map { it.text })
    }

    /** NFR2: at twice the font size on a 360 dp screen the help wraps in full, inside the screen. */
    @Test
    @Config(qualifiers = "w360dp-h640dp")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun atTwiceTheFontSizeTheHelpWrapsInFull() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)) {
                WatchSettingsScreen(ready, watchappOpen = true, onOpenSetting = {}, onBack = {})
            }
        }
        val help = string(R.string.watch_setting_arrow_left_help)
        val node = compose.onNodeWithTag(WatchSettingsTags.help(WatchSetting.ARROW_LEFT), useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextEquals(help)
        val layouts = mutableListOf<TextLayoutResult>()
        node.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        val layout = layouts.single()
        assertFalse("no ellipsis or clipping", layout.hasVisualOverflow)
        assertTrue("wraps over several lines", layout.lineCount > 1)
        assertEquals("the whole text is laid out", help.length, layout.getLineEnd(layout.lineCount - 1))
        val rootRight = compose.onRoot().getUnclippedBoundsInRoot().right
        assertTrue("inside the screen", node.getUnclippedBoundsInRoot().right <= rootRight)
    }

    /** NFR1: TalkBack reads a row as one item with title, value and help, and can still change it. */
    @Test
    fun aRowIsOneAccessibleItemWithTitleValueAndHelp() {
        compose.setContent { WatchSettingsScreen(ready, watchappOpen = true, onOpenSetting = {}, onBack = {}) }
        val row = compose.onNodeWithTag(WatchSettingsTags.row(WatchSetting.BACKLIGHT)).performScrollTo()
        val texts = row.fetchSemanticsNode().config[SemanticsProperties.Text].map { it.text }
        assertEquals(listOf("Backlight duration", "Watch default", string(R.string.watch_setting_backlight_help)), texts)
        row.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        assertEquals(
            "Change Backlight duration",
            row.fetchSemanticsNode().config[SemanticsActions.OnClick].label,
        )
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
