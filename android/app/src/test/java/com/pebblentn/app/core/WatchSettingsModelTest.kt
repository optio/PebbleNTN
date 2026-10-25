package com.pebblentn.app.core

import com.pebblentn.app.protocol.Protocol
import com.pebblentn.app.protocol.WatchMessages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The mirrored watch settings: defaults and validation match the watch (REQ-ANDROID-021, #102). */
class WatchSettingsModelTest {

    private val basalt = WatchCapabilities(Protocol.CapabilityMasks.COLOR_MASK)
    private val bw = WatchCapabilities.NONE
    private val emery = WatchMessages.EMERY_CAPS

    @Test
    fun twentyThreeSettingsInProtocolKeyOrder() {
        assertEquals(23, WatchSetting.entries.size)
        assertEquals(Protocol.Keys.SETTING_ACCENT, WatchSetting.ACCENT.key)
        assertEquals(Protocol.Keys.SETTING_SHORTCUT_TAP, WatchSetting.SHORTCUT_TAP.key)
        assertEquals(WatchSetting.EARLY_WARN_DISTANCE, WatchSetting.forKey(Protocol.Keys.SETTING_EARLY_WARN_DISTANCE))
        assertEquals(null, WatchSetting.forKey(Protocol.Keys.FLAGS))
    }

    @Test
    fun defaultsMatchTheWatch() {
        val colour = WatchSettingsModel.defaults(WatchPlatform.EMERY, emery)
        assertEquals("Green", 0, colour.accent)
        assertEquals("Off", WatchValues.VIBE_OFF, colour.vibePattern)
        assertEquals("Medium", 1, colour.vibeIntensity)
        assertEquals("Double", 2, colour.earlyWarnPattern)
        assertEquals("ETA toggle", ShortcutActions.ETA_MODE, colour.shortcutSwipeLeft)
        assertEquals("ETA toggle", ShortcutActions.ETA_MODE, colour.shortcutSwipeRight)
        assertEquals("25 m", 2, colour.earlyWarnDistance)
        assertEquals("every update", 1, colour.earlyWarnRepeat)
        assertEquals("Medium", 1, colour.earlyWarnIntensity)
        assertEquals("ETA toggle", ShortcutActions.ETA_MODE, colour.shortcutUp)
        assertEquals("Backlight now", ShortcutActions.BACKLIGHT_NOW, colour.shortcutDown)
        assertEquals("Backlight now", ShortcutActions.BACKLIGHT_NOW, colour.shortcutTap)
        assertEquals(ShortcutActions.NONE, colour.shortcutSwipeUp)
        assertTrue(colour.touchEnabled)
        assertFalse(colour.inverted)
        assertEquals("Black on black-and-white", WatchValues.ACCENT_BLACK, WatchSettingsModel.defaults(WatchPlatform.APLITE, bw).accent)
        assertTrue("every default is valid", WatchSetting.entries.all { WatchSettingsRules.isValid(it, colour[it], emery) })
    }

    @Test
    fun blackAndWhiteWatchesTakeOnlyBlackAndWhiteAccents() {
        assertFalse(WatchSettingsRules.isValid(WatchSetting.ACCENT, 1, bw))
        assertTrue(WatchSettingsRules.isValid(WatchSetting.ACCENT, WatchValues.ACCENT_WHITE, bw))
        assertEquals(listOf(WatchValues.ACCENT_BLACK, WatchValues.ACCENT_WHITE), WatchSettingsRules.options(WatchSetting.ACCENT, bw))
        assertEquals(16, WatchSettingsRules.options(WatchSetting.ACCENT, basalt).size)
    }

    @Test
    fun rgbOnlyValuesNeedAnRgbBacklight() {
        assertFalse(WatchSettingsRules.isOffered(WatchSetting.BACKLIGHT_COLOR, basalt))
        assertFalse(WatchSettingsRules.isValid(WatchSetting.BACKLIGHT_COLOR, 2, basalt))
        assertTrue(WatchSettingsRules.isValid(WatchSetting.BACKLIGHT_COLOR, 2, emery))
        assertFalse(WatchSettingsRules.isValid(WatchSetting.SHORTCUT_UP, ShortcutActions.NEXT_BACKLIGHT_COLOUR, basalt))
        assertFalse(ShortcutActions.NEXT_BACKLIGHT_COLOUR in WatchSettingsRules.options(WatchSetting.SHORTCUT_UP, basalt))
        assertTrue(ShortcutActions.NEXT_BACKLIGHT_COLOUR in WatchSettingsRules.options(WatchSetting.SHORTCUT_UP, emery))
    }

    @Test
    fun touchAndTheGesturesNeedTouch() {
        assertFalse(WatchSettingsRules.isOffered(WatchSetting.TOUCH, basalt))
        assertFalse(WatchSettingsRules.isValid(WatchSetting.SHORTCUT_TAP, 5, basalt))
        assertTrue(WatchSettingsRules.isValid(WatchSetting.SHORTCUT_TAP, 5, emery))
        assertTrue("buttons need no touch", WatchSettingsRules.isValid(WatchSetting.SHORTCUT_DOWN, 5, bw))
    }

    @Test
    fun rangesAndTheVibrationOrder() {
        assertFalse(WatchSettingsRules.isValid(WatchSetting.UNITS, 2, emery))
        assertFalse(WatchSettingsRules.isValid(WatchSetting.UNITS, -1, emery))
        assertFalse("no Off early warning pattern", WatchSettingsRules.isValid(WatchSetting.EARLY_WARN_PATTERN, 0, emery))
        assertEquals("Off listed first", listOf(5, 0, 1, 2, 3, 4), WatchSettingsRules.options(WatchSetting.VIBE_PATTERN, emery))
        assertEquals(listOf(1, 2, 3, 4), WatchSettingsRules.options(WatchSetting.EARLY_WARN_PATTERN, emery))
    }

    @Test
    fun fromWatchFillsGapsWithDefaults() {
        val model = WatchSettingsModel.fromWatch(
            carried = mapOf(WatchSetting.UNITS to 1, WatchSetting.GLYPH_PACK to 9),
            revision = 3,
            platform = WatchPlatform.BASALT,
            capabilities = basalt,
        )
        assertEquals(1, model.units)
        assertEquals("out of range reads as the default", 0, model.glyphPack)
        assertEquals(23, model.values.size)
        assertEquals(WatchPlatform.BASALT, model.platform)
    }
}
