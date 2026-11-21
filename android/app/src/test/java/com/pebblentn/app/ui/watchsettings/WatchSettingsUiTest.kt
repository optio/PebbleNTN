package com.pebblentn.app.ui.watchsettings

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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The Watch settings screen's rules (REQ-ANDROID-021, #102 FR5/FR6), without Compose. Robolectric
 * resolves the help texts (#139), as DisplayLabelsTest does for its labels.
 */
@RunWith(RobolectricTestRunner::class)
class WatchSettingsUiTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private val basalt = WatchCapabilities(Protocol.CapabilityMasks.COLOR_MASK)

    private fun rows(state: com.pebblentn.app.core.WatchSettingsModel) =
        WatchSettingsUi.sections(state).flatMap { it.settings }

    @Test
    fun waitingUntilTheWatchSendsItsSettingsAndOutdatedForAnOldWatchapp() {
        assertEquals(WatchSettingsStatus.WAITING, WatchSettingsUi.status(WatchSettingsSyncState()))
        assertEquals(WatchSettingsStatus.OUTDATED, WatchSettingsUi.status(WatchSettingsSyncState(watchappOutdated = true)))
        val ready = WatchSettingsSyncState(mirror = WatchMessages.settings(1), receivedFromWatch = true)
        assertEquals(WatchSettingsStatus.READY, WatchSettingsUi.status(ready))
    }

    @Test
    fun noBacklightColourWithoutRgbAndNoTouchRowsWithoutTouch() {
        val visible = rows(WatchMessages.settings(1, platform = WatchPlatform.BASALT, caps = basalt))
        assertFalse(WatchSetting.BACKLIGHT_COLOR in visible)
        assertFalse(WatchSetting.TOUCH in visible)
        assertFalse(WatchSetting.SHORTCUT_TAP in visible)
        assertTrue(WatchSetting.SHORTCUT_UP in visible)
        assertTrue(WatchSetting.BACKLIGHT in visible)
    }

    @Test
    fun theGesturesShowOnlyWhileTouchIsOn() {
        val off = rows(WatchMessages.settings(1, mapOf(WatchSetting.TOUCH to 0)))
        assertTrue(WatchSetting.TOUCH in off)
        assertTrue(WatchSetting.BACKLIGHT_COLOR in off)
        assertFalse(WatchSetting.SHORTCUT_SWIPE_LEFT in off)
        val on = rows(WatchMessages.settings(1, mapOf(WatchSetting.TOUCH to 1)))
        assertTrue(listOf(
            WatchSetting.SHORTCUT_SWIPE_LEFT, WatchSetting.SHORTCUT_SWIPE_RIGHT, WatchSetting.SHORTCUT_SWIPE_UP,
            WatchSetting.SHORTCUT_SWIPE_DOWN, WatchSetting.SHORTCUT_TAP,
        ).all { it in on })
    }

    @Test
    fun theEarlyWarningRowsFollowItsDistance() {
        val off = rows(WatchMessages.settings(1, mapOf(WatchSetting.EARLY_WARN_DISTANCE to 0)))
        assertTrue(WatchSetting.EARLY_WARN_DISTANCE in off)
        assertFalse(WatchSetting.EARLY_WARN_PATTERN in off)
        assertTrue(WatchSetting.EARLY_WARN_PATTERN in rows(WatchMessages.settings(1, mapOf(WatchSetting.EARLY_WARN_DISTANCE to 3))))
    }

    @Test
    fun blackAndWhiteWatchesListOnlyBlackAndWhiteAccents() {
        val bw = WatchMessages.settings(1, platform = WatchPlatform.DIORITE, caps = WatchCapabilities.NONE)
        assertEquals(listOf(14, 15), WatchSettingsUi.options(WatchSetting.ACCENT, bw))
    }

    @Test
    fun theQueuedNoticeShowsForPendingEditsWhileClosed() {
        val pending = WatchSettingsSyncState(mirror = WatchMessages.settings(1), pending = setOf(WatchSetting.UNITS))
        assertTrue(WatchSettingsUi.showsQueuedNotice(pending, watchappOpen = false))
        assertFalse(WatchSettingsUi.showsQueuedNotice(pending, watchappOpen = true))
        assertFalse(WatchSettingsUi.showsQueuedNotice(pending.copy(pending = emptySet()), watchappOpen = false))
    }

    @Test
    fun previewsOnlyForVisualSettingsAndGreyOnBlackAndWhite() {
        assertNull(WatchSettingsUi.preview(WatchSetting.VIBE_PATTERN, 1, WatchPlatform.BASALT))
        assertFalse(WatchSettingsUi.preview(WatchSetting.INVERT, 1, WatchPlatform.BASALT)!!.greyscale)
        assertTrue(WatchSettingsUi.preview(WatchSetting.INVERT, 1, WatchPlatform.APLITE)!!.greyscale)
        assertTrue(WatchSettingsUi.preview(WatchSetting.ACCENT, 15, WatchPlatform.DIORITE)!!.greyscale)
    }

    @Test
    fun everySettingHasATitleAndLabelsForEveryValue() {
        for (setting in WatchSetting.entries) {
            assertTrue(WatchSettingsUi.title(setting) != 0)
            assertTrue(WatchSettingsUi.valueLabels(setting, imperial = false) != 0)
        }
        assertEquals(16, WatchSettingsUi.ACCENT_COLOURS.size)
    }

    // --- Help texts (#139 FR1–FR3, FR5) ------------------------------------------------------------

    /** FR1.4, FR2.3: no setting lacks a short line or an explanation; FR1.1: the line stays short. */
    @Test
    fun everySettingHasAShortHelpLineAndAFullerExplanation() {
        for (setting in WatchSetting.entries) {
            val help = context.getString(WatchSettingsUi.help(setting))
            assertTrue("${setting.name} has help", help.isNotBlank())
            assertTrue("${setting.name} help is short: $help", help.length <= MAX_HELP_LENGTH)
            for (imperial in listOf(false, true)) {
                val details = context.getString(WatchSettingsUi.details(setting, imperial))
                assertTrue("${setting.name} details are fuller than the help", details.length > help.length)
            }
        }
        assertEquals(
            "the driving speeds share one line",
            WatchSettingsUi.help(WatchSetting.DRIVING_SPEED_METRIC),
            WatchSettingsUi.help(WatchSetting.DRIVING_SPEED_IMPERIAL),
        )
    }

    /** FR3.1, FR3.3: one description per shortcut action, for the seven slots only. */
    @Test
    fun onlyTheShortcutSlotsDescribeTheirValuesOnePerAction() {
        val withDescriptions = WatchSetting.entries.filter { WatchSettingsUi.valueDescriptions(it) != null }
        assertEquals(
            listOf(
                WatchSetting.SHORTCUT_UP, WatchSetting.SHORTCUT_DOWN, WatchSetting.SHORTCUT_SWIPE_LEFT,
                WatchSetting.SHORTCUT_SWIPE_RIGHT, WatchSetting.SHORTCUT_SWIPE_UP, WatchSetting.SHORTCUT_SWIPE_DOWN,
                WatchSetting.SHORTCUT_TAP,
            ),
            withDescriptions,
        )
        assertNull("Touch is On/Off, no actions", WatchSettingsUi.valueDescriptions(WatchSetting.TOUCH))
        assertNull(WatchSettingsUi.valueDescriptions(WatchSetting.BACKLIGHT))
        val actions = context.resources.getStringArray(R.array.watch_values_shortcut)
        val descriptions = context.resources.getStringArray(WatchSettingsUi.valueDescriptions(WatchSetting.SHORTCUT_UP)!!)
        assertEquals(15, actions.size)
        assertEquals(actions.size, descriptions.size)
        assertTrue(descriptions.all { it.isNotBlank() })
        assertEquals("descriptions are distinct", descriptions.size, descriptions.toSet().size)
    }

    /** FR5.3: the early-warning explanation names Off as its default (#141) and its example distance in the watch's units. */
    @Test
    fun theEarlyWarningExplanationFollowsTheUnits() {
        val metric = context.getString(WatchSettingsUi.details(WatchSetting.EARLY_WARN_DISTANCE, imperial = false))
        val imperial = context.getString(WatchSettingsUi.details(WatchSetting.EARLY_WARN_DISTANCE, imperial = true))
        assertNotEquals(metric, imperial)
        assertTrue(metric.startsWith("Off, the default") && metric.contains("25 m") && !metric.contains(" ft"))
        assertTrue(imperial.startsWith("Off, the default") && imperial.contains("80 ft") && !imperial.contains(" m,"))
        assertEquals(
            "a text without a distance is the same in both units",
            WatchSettingsUi.details(WatchSetting.BACKLIGHT, imperial = false),
            WatchSettingsUi.details(WatchSetting.BACKLIGHT, imperial = true),
        )
    }

    /**
     * FR5.2: a row every watch shows never talks about touch, swipes or taps; only the Touch and
     * gesture rows, which a touch watch alone shows, do.
     */
    @Test
    fun onlyTheTouchRowsMentionTouchGestures() {
        val touchRows = setOf(
            WatchSetting.TOUCH, WatchSetting.SHORTCUT_SWIPE_LEFT, WatchSetting.SHORTCUT_SWIPE_RIGHT,
            WatchSetting.SHORTCUT_SWIPE_UP, WatchSetting.SHORTCUT_SWIPE_DOWN, WatchSetting.SHORTCUT_TAP,
        )
        val gesture = Regex("\\b(swip\\w*|tap\\w*|touch\\w*)\\b", RegexOption.IGNORE_CASE)
        for (setting in WatchSetting.entries - touchRows) {
            val texts = listOf(context.getString(WatchSettingsUi.help(setting))) +
                listOf(false, true).map { context.getString(WatchSettingsUi.details(setting, it)) }
            for (text in texts) assertFalse("${setting.name}: $text", gesture.containsMatchIn(text))
        }
        val descriptions = context.resources.getStringArray(R.array.watch_values_shortcut_descriptions)
        assertTrue("action descriptions are shown on the button lists too", descriptions.none { gesture.containsMatchIn(it) })
    }

    // --- When driving (#132 FR3, FR4) ------------------------------------------------------------

    @Test
    fun theDrivingSectionNeedsAProtocol14Watchapp() {
        val mirror = WatchMessages.settings(1)
        assertFalse(WatchSettingsUi.showsDrivingSection(WatchSettingsSyncState(mirror, receivedFromWatch = true, watchProtocolMinor = 3)))
        assertFalse("never reported", WatchSettingsUi.showsDrivingSection(WatchSettingsSyncState(mirror, receivedFromWatch = true)))
        assertTrue(WatchSettingsUi.showsDrivingSection(WatchSettingsSyncState(mirror, receivedFromWatch = true, watchProtocolMinor = 4)))
    }

    /**
     * #134 FR7.2, FR7.4: the Directions row is there for every watchapp that sends its settings.
     * #139 FR4.1: its note moved into the row's help, so no section has a note any more.
     */
    @Test
    fun theDirectionsSectionIsOfferedOnEveryWatchWithoutANote() {
        for (caps in listOf(WatchCapabilities.NONE, basalt, WatchMessages.EMERY_CAPS)) {
            val sections = WatchSettingsUi.sections(WatchMessages.settings(1, caps = caps))
            val directions = sections.single { WatchSetting.DIRECTIONS_TEXT in it.settings }
            assertEquals(listOf(WatchSetting.DIRECTIONS_TEXT), directions.settings)
            assertNull(directions.note)
            assertTrue("no section keeps a note", sections.all { it.note == null })
        }
        assertTrue(context.getString(WatchSettingsUi.help(WatchSetting.DIRECTIONS_TEXT)).contains("Compact"))
        assertEquals(listOf(0, 1), WatchSettingsUi.options(WatchSetting.DIRECTIONS_TEXT, WatchMessages.settings(1)))
        assertNull(WatchSettingsUi.preview(WatchSetting.DIRECTIONS_TEXT, 1, WatchPlatform.BASALT))
    }

    @Test
    fun theDrivingSettingsAreNotListedAsOptionRows() {
        assertTrue(rows(WatchMessages.settings(1)).none { it.isDriving })
    }

    @Test
    fun theDrivingSpeedFollowsTheUnitsAndTheResetShowsOnlyWhenChanged() {
        val metric = WatchMessages.settings(1)
        assertEquals(25, WatchSettingsUi.drivingSpeedValue(metric))
        assertEquals(25, WatchSettingsUi.drivingDefaultValue(metric))
        assertFalse(WatchSettingsUi.showsDrivingReset(metric))
        val faster = metric.with(WatchSetting.DRIVING_SPEED_METRIC, 5)
        assertEquals(35, WatchSettingsUi.drivingSpeedValue(faster))
        assertTrue(WatchSettingsUi.showsDrivingReset(faster))

        val imperial = faster.with(WatchSetting.UNITS, 1)
        assertEquals("15 mph: the metric change does not count", 15, WatchSettingsUi.drivingSpeedValue(imperial))
        assertEquals(15, WatchSettingsUi.drivingDefaultValue(imperial))
        assertFalse(WatchSettingsUi.showsDrivingReset(imperial))
    }

    private companion object {
        /** "At most about 90 characters" (#139 FR1.1). */
        const val MAX_HELP_LENGTH = 90
    }
}
