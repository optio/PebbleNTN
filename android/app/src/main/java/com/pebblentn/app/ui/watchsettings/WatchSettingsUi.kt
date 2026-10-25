package com.pebblentn.app.ui.watchsettings

import androidx.annotation.ArrayRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.pebblentn.app.R
import com.pebblentn.app.core.PreviewCatalog
import com.pebblentn.app.core.WatchPlatform
import com.pebblentn.app.core.WatchSetting
import com.pebblentn.app.core.WatchSettingsModel
import com.pebblentn.app.core.WatchSettingsRules
import com.pebblentn.app.core.WatchSettingsSyncState

/**
 * What the Watch settings screen shows (REQ-ANDROID-021, #102), as pure functions so the rules are
 * unit-tested without Compose: which state it is in, which rows a watch offers, the labels and the
 * previews.
 */
enum class WatchSettingsStatus {
    /** No watchapp has sent its settings yet: open it on the watch. */
    WAITING,

    /** The watchapp heard from is too old to send its settings: update it. */
    OUTDATED,

    /** The settings are known and can be changed. */
    READY,
}

/** One group of rows, as on the watch's own menu. */
data class WatchSettingsSection(@param:StringRes val title: Int, val settings: List<WatchSetting>)

/** A preview image and whether it is shown in greyscale (black-and-white watches). */
data class WatchPreview(@param:DrawableRes val drawable: Int, val greyscale: Boolean)

object WatchSettingsUi {

    fun status(state: WatchSettingsSyncState): WatchSettingsStatus = when {
        state.watchappOutdated -> WatchSettingsStatus.OUTDATED
        state.mirror != null && state.receivedFromWatch -> WatchSettingsStatus.READY
        else -> WatchSettingsStatus.WAITING
    }

    /** Whether to say that the phone's edits wait for the watchapp: some are pending and it is closed. */
    fun showsQueuedNotice(state: WatchSettingsSyncState, watchappOpen: Boolean): Boolean =
        state.pending.isNotEmpty() && !watchappOpen

    /**
     * The groups and rows for [model]'s watch: only what its capabilities allow (Backlight colour on
     * an RGB backlight, the Touch rows on a touch watch, the gestures only while Touch is on) and,
     * as on the watch, the early warning's other rows only once a distance is chosen.
     */
    fun sections(model: WatchSettingsModel): List<WatchSettingsSection> {
        val caps = model.capabilities
        fun offered(vararg settings: WatchSetting) = settings.filter { WatchSettingsRules.isOffered(it, caps) }
        val warning = if (model.earlyWarnDistance == 0) {
            listOf(WatchSetting.EARLY_WARN_DISTANCE)
        } else {
            listOf(
                WatchSetting.EARLY_WARN_DISTANCE,
                WatchSetting.EARLY_WARN_REPEAT,
                WatchSetting.EARLY_WARN_PATTERN,
                WatchSetting.EARLY_WARN_INTENSITY,
            )
        }
        val gestures = if (model.touchEnabled) {
            offered(
                WatchSetting.SHORTCUT_SWIPE_LEFT,
                WatchSetting.SHORTCUT_SWIPE_RIGHT,
                WatchSetting.SHORTCUT_SWIPE_UP,
                WatchSetting.SHORTCUT_SWIPE_DOWN,
                WatchSetting.SHORTCUT_TAP,
            )
        } else {
            emptyList()
        }
        return listOf(
            WatchSettingsSection(
                R.string.watch_settings_section_appearance,
                listOf(WatchSetting.ACCENT, WatchSetting.INVERT, WatchSetting.GLYPH_PACK, WatchSetting.ARROW_LEFT),
            ),
            WatchSettingsSection(R.string.watch_settings_section_eta, listOf(WatchSetting.ETA_MODE, WatchSetting.ETA_LABEL)),
            WatchSettingsSection(R.string.watch_settings_section_units, listOf(WatchSetting.UNITS)),
            WatchSettingsSection(
                R.string.watch_settings_section_backlight,
                offered(WatchSetting.BACKLIGHT, WatchSetting.BACKLIGHT_COLOR),
            ),
            WatchSettingsSection(
                R.string.watch_settings_section_vibration,
                listOf(WatchSetting.VIBE_PATTERN, WatchSetting.VIBE_INTENSITY),
            ),
            WatchSettingsSection(R.string.watch_settings_section_early_warning, warning),
            WatchSettingsSection(
                R.string.watch_settings_section_shortcuts,
                listOf(WatchSetting.SHORTCUT_UP, WatchSetting.SHORTCUT_DOWN) +
                    offered(WatchSetting.TOUCH) + gestures,
            ),
        )
    }

    /** The values the option list offers, in the watch menu's order. */
    fun options(setting: WatchSetting, model: WatchSettingsModel): List<Int> =
        WatchSettingsRules.options(setting, model.capabilities)

    /** The preview of [value], or null when [setting] is not a visual one. */
    fun preview(setting: WatchSetting, value: Int, platform: WatchPlatform): WatchPreview? =
        PreviewCatalog.drawable(setting, value, platform)?.let { WatchPreview(it, PreviewCatalog.isGreyscale(platform)) }

    @StringRes
    fun title(setting: WatchSetting): Int = when (setting) {
        WatchSetting.ACCENT -> R.string.watch_setting_accent
        WatchSetting.UNITS -> R.string.watch_setting_units
        WatchSetting.INVERT -> R.string.watch_setting_invert
        WatchSetting.GLYPH_PACK -> R.string.watch_setting_glyph_pack
        WatchSetting.ARROW_LEFT -> R.string.watch_setting_arrow_left
        WatchSetting.ETA_MODE -> R.string.watch_setting_eta_mode
        WatchSetting.BACKLIGHT -> R.string.watch_setting_backlight
        WatchSetting.VIBE_PATTERN -> R.string.watch_setting_vibe_pattern
        WatchSetting.VIBE_INTENSITY -> R.string.watch_setting_vibe_intensity
        WatchSetting.BACKLIGHT_COLOR -> R.string.watch_setting_backlight_color
        WatchSetting.ETA_LABEL -> R.string.watch_setting_eta_label
        WatchSetting.EARLY_WARN_DISTANCE -> R.string.watch_setting_early_warn_distance
        WatchSetting.EARLY_WARN_REPEAT -> R.string.watch_setting_early_warn_repeat
        WatchSetting.EARLY_WARN_PATTERN -> R.string.watch_setting_early_warn_pattern
        WatchSetting.EARLY_WARN_INTENSITY -> R.string.watch_setting_early_warn_intensity
        WatchSetting.TOUCH -> R.string.watch_setting_touch
        WatchSetting.SHORTCUT_UP -> R.string.watch_setting_shortcut_up
        WatchSetting.SHORTCUT_DOWN -> R.string.watch_setting_shortcut_down
        WatchSetting.SHORTCUT_SWIPE_LEFT -> R.string.watch_setting_shortcut_swipe_left
        WatchSetting.SHORTCUT_SWIPE_RIGHT -> R.string.watch_setting_shortcut_swipe_right
        WatchSetting.SHORTCUT_SWIPE_UP -> R.string.watch_setting_shortcut_swipe_up
        WatchSetting.SHORTCUT_SWIPE_DOWN -> R.string.watch_setting_shortcut_swipe_down
        WatchSetting.SHORTCUT_TAP -> R.string.watch_setting_shortcut_tap
    }

    /** The labels of [setting]'s values, indexed by value; distances follow the watch's units. */
    @ArrayRes
    fun valueLabels(setting: WatchSetting, imperial: Boolean): Int = when (setting) {
        WatchSetting.ACCENT -> R.array.watch_values_accent
        WatchSetting.UNITS -> R.array.watch_values_units
        WatchSetting.INVERT -> R.array.watch_values_invert
        WatchSetting.GLYPH_PACK -> R.array.watch_values_glyph_pack
        WatchSetting.ARROW_LEFT -> R.array.watch_values_arrow_left
        WatchSetting.ETA_MODE -> R.array.watch_values_eta_mode
        WatchSetting.BACKLIGHT -> R.array.watch_values_backlight
        WatchSetting.VIBE_PATTERN, WatchSetting.EARLY_WARN_PATTERN -> R.array.watch_values_vibe_pattern
        WatchSetting.VIBE_INTENSITY, WatchSetting.EARLY_WARN_INTENSITY -> R.array.watch_values_vibe_intensity
        WatchSetting.BACKLIGHT_COLOR -> R.array.watch_values_backlight_color
        WatchSetting.ETA_LABEL -> R.array.watch_values_eta_label
        WatchSetting.EARLY_WARN_DISTANCE ->
            if (imperial) R.array.watch_values_early_warn_distance_imperial else R.array.watch_values_early_warn_distance_metric
        WatchSetting.EARLY_WARN_REPEAT -> R.array.watch_values_early_warn_repeat
        WatchSetting.TOUCH -> R.array.watch_values_on_off
        WatchSetting.SHORTCUT_UP, WatchSetting.SHORTCUT_DOWN, WatchSetting.SHORTCUT_SWIPE_LEFT,
        WatchSetting.SHORTCUT_SWIPE_RIGHT, WatchSetting.SHORTCUT_SWIPE_UP, WatchSetting.SHORTCUT_SWIPE_DOWN,
        WatchSetting.SHORTCUT_TAP -> R.array.watch_values_shortcut
    }

    /**
     * The colour each accent has on a colour watch (theme.c accent_color, as 0xAARRGGBB), for the
     * thumbnails of the colour list.
     */
    val ACCENT_COLOURS: List<Long> = listOf(
        0xFF00AA00, // Green: GColorIslamicGreen
        0xFF0055AA, // Blue: GColorCobaltBlue
        0xFFAA0000, // Red: GColorDarkCandyAppleRed
        0xFFFF5500, // Orange: GColorOrange
        0xFF550055, // Purple: GColorImperialPurple
        0xFF00AAAA, // Teal: GColorTiffanyBlue
        0xFFFF55FF, // Pink: GColorShockingPink
        0xFFFFFF00, // Yellow: GColorYellow
        0xFF00FFFF, // Cyan: GColorCyan
        0xFF00FF00, // Lime: GColorGreen
        0xFFAA5500, // Tan: GColorWindsorTan
        0xFF000055, // Navy: GColorOxfordBlue
        0xFFFFAA00, // Amber: GColorChromeYellow
        0xFFAAAAAA, // Grey: GColorLightGray
        0xFF000000, // Black
        0xFFFFFFFF, // White
    )
}
