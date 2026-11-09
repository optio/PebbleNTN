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
    fun twentySixSettingsInProtocolKeyOrder() {
        assertEquals(26, WatchSetting.entries.size)
        assertEquals(Protocol.Keys.SETTING_ACCENT, WatchSetting.ACCENT.key)
        assertEquals(Protocol.Keys.SETTING_SHORTCUT_TAP, WatchSetting.SHORTCUT_TAP.key)
        assertEquals(Protocol.Keys.SETTING_DRIVING_MODE, WatchSetting.DRIVING_MODE.key)
        assertEquals(Protocol.Keys.SETTING_DRIVING_SPEED_METRIC, WatchSetting.DRIVING_SPEED_METRIC.key)
        assertEquals(Protocol.Keys.SETTING_DRIVING_SPEED_IMPERIAL, WatchSetting.DRIVING_SPEED_IMPERIAL.key)
        assertEquals(4, Protocol.MINOR)
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
        assertEquals(26, model.values.size)
        assertEquals(WatchPlatform.BASALT, model.platform)
    }

    // --- When driving (#132 FR1, FR4) ------------------------------------------------------------

    @Test
    fun drivingSettingsDefaultToShowDirections25KmhAnd15Mph() {
        val model = WatchSettingsModel.defaults(WatchPlatform.EMERY, emery)
        assertFalse("Show directions (FR1.1, FR1.5)", model.drivingHide)
        assertEquals("25 km/h", 3, model.drivingSpeedMetric)
        assertEquals("15 mph", 2, model.drivingSpeedImperial)
        assertEquals(25, DrivingSpeeds.metricKmh(model.drivingSpeedMetric))
        assertEquals(15, DrivingSpeeds.imperialMph(model.drivingSpeedImperial))
    }

    @Test
    fun drivingSpeedRangesAre10To60KmhAnd5To40Mph() {
        assertEquals(2, WatchSetting.DRIVING_MODE.valueCount)
        assertEquals((0..10).toList(), WatchSettingsRules.options(WatchSetting.DRIVING_SPEED_METRIC, bw))
        assertEquals((0..7).toList(), WatchSettingsRules.options(WatchSetting.DRIVING_SPEED_IMPERIAL, bw))
        assertEquals(10, DrivingSpeeds.metricKmh(0))
        assertEquals(60, DrivingSpeeds.metricKmh(10))
        assertEquals(5, DrivingSpeeds.imperialMph(0))
        assertEquals(40, DrivingSpeeds.imperialMph(7))
        assertFalse(WatchSettingsRules.isValid(WatchSetting.DRIVING_SPEED_METRIC, 11, emery))
        assertFalse(WatchSettingsRules.isValid(WatchSetting.DRIVING_SPEED_IMPERIAL, 8, emery))
        assertFalse(WatchSettingsRules.isValid(WatchSetting.DRIVING_MODE, 2, emery))
    }

    @Test
    fun drivingThresholdFollowsTheUnitsSetting() {
        val metric = WatchSettingsModel.defaults(WatchPlatform.BASALT, basalt)
        assertEquals(25.0, metric.drivingThresholdKmh(), 1e-9)
        val imperial = metric.with(WatchSetting.UNITS, DrivingSpeeds.UNITS_IMPERIAL)
        assertEquals("15 mph", 24.14, imperial.drivingThresholdKmh(), 0.01)
        assertEquals(WatchSetting.DRIVING_SPEED_IMPERIAL, imperial.drivingSpeedSetting)
        assertEquals(WatchSetting.DRIVING_SPEED_METRIC, metric.drivingSpeedSetting)
        assertEquals("60 km/h", 60.0, metric.with(WatchSetting.DRIVING_SPEED_METRIC, 10).drivingThresholdKmh(), 1e-9)
    }

    @Test
    fun aUnitsChangeKeepsBothStoredSpeeds() {
        val edited = WatchSettingsModel.defaults(WatchPlatform.BASALT, basalt)
            .with(WatchSetting.DRIVING_SPEED_METRIC, 6) // 40 km/h
            .with(WatchSetting.DRIVING_SPEED_IMPERIAL, 5) // 30 mph
        val imperial = edited.with(WatchSetting.UNITS, DrivingSpeeds.UNITS_IMPERIAL)
        assertEquals("neither value converted (FR1.4)", 6, imperial.drivingSpeedMetric)
        assertEquals(5, imperial.drivingSpeedImperial)
        assertEquals(30 * DrivingSpeeds.KMH_PER_MPH, imperial.drivingThresholdKmh(), 1e-9)
        assertEquals(40.0, imperial.with(WatchSetting.UNITS, 0).drivingThresholdKmh(), 1e-9)
    }

    @Test
    fun anOlderWatchappWithoutTheDrivingKeysReadsAsTheDefaults() {
        val carried = WatchSetting.entries.filterNot { it.isDriving }.associateWith { 0 }
        val model = WatchSettingsModel.fromWatch(carried, revision = 1, platform = WatchPlatform.BASALT, capabilities = basalt)
        assertFalse(model.drivingHide)
        assertEquals(3, model.drivingSpeedMetric)
        assertEquals(2, model.drivingSpeedImperial)
    }

    @Test
    fun theDrivingSettingsAreNotShortcuts() {
        assertFalse(WatchSetting.DRIVING_MODE.isShortcut)
        assertFalse(WatchSetting.DRIVING_SPEED_METRIC.isShortcut)
        assertFalse(WatchSetting.DRIVING_SPEED_IMPERIAL.isShortcut)
        assertTrue(WatchSetting.SHORTCUT_TAP.isShortcut)
        assertTrue(WatchSetting.SHORTCUT_UP.isShortcut)
        assertFalse(WatchSetting.TOUCH.isShortcut)
        assertEquals(3, WatchSetting.entries.count { it.isDriving })
        // Value 12 is the shortcut "Next backlight colour"; a driving speed index 12 is out of range anyway.
        assertTrue(WatchSettingsRules.isValid(WatchSetting.DRIVING_SPEED_METRIC, 10, basalt))
    }
}
