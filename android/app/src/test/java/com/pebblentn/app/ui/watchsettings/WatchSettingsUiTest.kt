package com.pebblentn.app.ui.watchsettings

import com.pebblentn.app.core.WatchCapabilities
import com.pebblentn.app.core.WatchPlatform
import com.pebblentn.app.core.WatchSetting
import com.pebblentn.app.core.WatchSettingsSyncState
import com.pebblentn.app.protocol.Protocol
import com.pebblentn.app.protocol.WatchMessages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Watch settings screen's rules (REQ-ANDROID-021, #102 FR5/FR6), without Compose. */
class WatchSettingsUiTest {

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
}
