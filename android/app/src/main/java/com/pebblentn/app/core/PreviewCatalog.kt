package com.pebblentn.app.core

import androidx.annotation.DrawableRes
import com.pebblentn.app.R

/**
 * The preview images of the visual watch settings (REQ-ANDROID-021, #102 FR6/FR7): one emulator
 * screenshot per value of each setting, every other setting at its default, made by
 * `watchapp/tools/capture_screenshots.py --previews` for basalt (144×168), chalk (180×180) and
 * emery (200×228). Black-and-white watches (aplite, diorite) use the basalt set in greyscale, and
 * so does a watch whose platform is not known yet.
 *
 * Every drawable is referenced here by its `R.drawable` constant, never looked up by name, so
 * release builds (resource shrinking) keep them. PreviewCatalogTest checks that every visual value
 * has an image on every set and that no `watch_preview_*` image is left without a value.
 */
object PreviewCatalog {

    /** The image sets the capture tool makes, one per screen shape. */
    enum class ImageSet { BASALT, CHALK, EMERY }

    /** The settings that change how the navigation screen looks, in the watch menu's order. */
    val VISUAL_SETTINGS: List<WatchSetting> = listOf(
        WatchSetting.ACCENT,
        WatchSetting.INVERT,
        WatchSetting.GLYPH_PACK,
        WatchSetting.ARROW_LEFT,
        WatchSetting.ETA_MODE,
        WatchSetting.ETA_LABEL,
        WatchSetting.UNITS,
    )

    /**
     * The name each value has in the image file names (`watch_preview_<setting>_<value>_<set>`), in
     * value order; the capture tool uses the same names.
     */
    val VALUE_NAMES: Map<WatchSetting, List<String>> = mapOf(
        WatchSetting.ACCENT to listOf(
            "green", "blue", "red", "orange", "purple", "teal", "pink", "yellow",
            "cyan", "lime", "tan", "navy", "amber", "grey", "black", "white",
        ),
        WatchSetting.INVERT to listOf("off", "on"),
        WatchSetting.GLYPH_PACK to listOf("classic", "bold", "outline"),
        WatchSetting.ARROW_LEFT to listOf("right", "left"),
        WatchSetting.ETA_MODE to listOf("arrival", "remaining"),
        WatchSetting.ETA_LABEL to listOf("labelled", "compact"),
        WatchSetting.UNITS to listOf("metric", "imperial"),
    )

    /** The setting's part of the file names. */
    val SETTING_NAMES: Map<WatchSetting, String> = mapOf(
        WatchSetting.ACCENT to "accent",
        WatchSetting.INVERT to "invert",
        WatchSetting.GLYPH_PACK to "glyph",
        WatchSetting.ARROW_LEFT to "arrow",
        WatchSetting.ETA_MODE to "eta",
        WatchSetting.ETA_LABEL to "label",
        WatchSetting.UNITS to "units",
    )

    fun isVisual(setting: WatchSetting): Boolean = setting in VISUAL_SETTINGS

    /** The set the images for [platform] come from: basalt unless it is chalk or emery. */
    fun imageSet(platform: WatchPlatform): ImageSet = when (platform) {
        WatchPlatform.CHALK -> ImageSet.CHALK
        WatchPlatform.EMERY -> ImageSet.EMERY
        else -> ImageSet.BASALT
    }

    /** Whether the preview is shown in greyscale: on a black-and-white watch (aplite, diorite). */
    fun isGreyscale(platform: WatchPlatform): Boolean =
        platform == WatchPlatform.APLITE || platform == WatchPlatform.DIORITE

    /** The preview of [value] of [setting] on [platform], or null for a setting with no preview. */
    @DrawableRes
    fun drawable(setting: WatchSetting, value: Int, platform: WatchPlatform): Int? =
        IMAGES[imageSet(platform)]?.get(setting)?.getOrNull(value)

    /** The resource name the capture tool writes for one image, for the completeness check. */
    fun resourceName(setting: WatchSetting, value: Int, set: ImageSet): String? {
        val settingName = SETTING_NAMES[setting] ?: return null
        val valueName = VALUE_NAMES[setting]?.getOrNull(value) ?: return null
        return "watch_preview_${settingName}_${valueName}_${set.name.lowercase()}"
    }

    /** Every image, by set and setting, in value order. */
    val IMAGES: Map<ImageSet, Map<WatchSetting, List<Int>>> = mapOf(
        ImageSet.BASALT to mapOf(
            WatchSetting.ACCENT to listOf(
                R.drawable.watch_preview_accent_green_basalt,
                R.drawable.watch_preview_accent_blue_basalt,
                R.drawable.watch_preview_accent_red_basalt,
                R.drawable.watch_preview_accent_orange_basalt,
                R.drawable.watch_preview_accent_purple_basalt,
                R.drawable.watch_preview_accent_teal_basalt,
                R.drawable.watch_preview_accent_pink_basalt,
                R.drawable.watch_preview_accent_yellow_basalt,
                R.drawable.watch_preview_accent_cyan_basalt,
                R.drawable.watch_preview_accent_lime_basalt,
                R.drawable.watch_preview_accent_tan_basalt,
                R.drawable.watch_preview_accent_navy_basalt,
                R.drawable.watch_preview_accent_amber_basalt,
                R.drawable.watch_preview_accent_grey_basalt,
                R.drawable.watch_preview_accent_black_basalt,
                R.drawable.watch_preview_accent_white_basalt,
            ),
            WatchSetting.INVERT to listOf(
                R.drawable.watch_preview_invert_off_basalt,
                R.drawable.watch_preview_invert_on_basalt,
            ),
            WatchSetting.GLYPH_PACK to listOf(
                R.drawable.watch_preview_glyph_classic_basalt,
                R.drawable.watch_preview_glyph_bold_basalt,
                R.drawable.watch_preview_glyph_outline_basalt,
            ),
            WatchSetting.ARROW_LEFT to listOf(
                R.drawable.watch_preview_arrow_right_basalt,
                R.drawable.watch_preview_arrow_left_basalt,
            ),
            WatchSetting.ETA_MODE to listOf(
                R.drawable.watch_preview_eta_arrival_basalt,
                R.drawable.watch_preview_eta_remaining_basalt,
            ),
            WatchSetting.ETA_LABEL to listOf(
                R.drawable.watch_preview_label_labelled_basalt,
                R.drawable.watch_preview_label_compact_basalt,
            ),
            WatchSetting.UNITS to listOf(
                R.drawable.watch_preview_units_metric_basalt,
                R.drawable.watch_preview_units_imperial_basalt,
            ),
        ),
        ImageSet.CHALK to mapOf(
            WatchSetting.ACCENT to listOf(
                R.drawable.watch_preview_accent_green_chalk,
                R.drawable.watch_preview_accent_blue_chalk,
                R.drawable.watch_preview_accent_red_chalk,
                R.drawable.watch_preview_accent_orange_chalk,
                R.drawable.watch_preview_accent_purple_chalk,
                R.drawable.watch_preview_accent_teal_chalk,
                R.drawable.watch_preview_accent_pink_chalk,
                R.drawable.watch_preview_accent_yellow_chalk,
                R.drawable.watch_preview_accent_cyan_chalk,
                R.drawable.watch_preview_accent_lime_chalk,
                R.drawable.watch_preview_accent_tan_chalk,
                R.drawable.watch_preview_accent_navy_chalk,
                R.drawable.watch_preview_accent_amber_chalk,
                R.drawable.watch_preview_accent_grey_chalk,
                R.drawable.watch_preview_accent_black_chalk,
                R.drawable.watch_preview_accent_white_chalk,
            ),
            WatchSetting.INVERT to listOf(
                R.drawable.watch_preview_invert_off_chalk,
                R.drawable.watch_preview_invert_on_chalk,
            ),
            WatchSetting.GLYPH_PACK to listOf(
                R.drawable.watch_preview_glyph_classic_chalk,
                R.drawable.watch_preview_glyph_bold_chalk,
                R.drawable.watch_preview_glyph_outline_chalk,
            ),
            WatchSetting.ARROW_LEFT to listOf(
                R.drawable.watch_preview_arrow_right_chalk,
                R.drawable.watch_preview_arrow_left_chalk,
            ),
            WatchSetting.ETA_MODE to listOf(
                R.drawable.watch_preview_eta_arrival_chalk,
                R.drawable.watch_preview_eta_remaining_chalk,
            ),
            WatchSetting.ETA_LABEL to listOf(
                R.drawable.watch_preview_label_labelled_chalk,
                R.drawable.watch_preview_label_compact_chalk,
            ),
            WatchSetting.UNITS to listOf(
                R.drawable.watch_preview_units_metric_chalk,
                R.drawable.watch_preview_units_imperial_chalk,
            ),
        ),
        ImageSet.EMERY to mapOf(
            WatchSetting.ACCENT to listOf(
                R.drawable.watch_preview_accent_green_emery,
                R.drawable.watch_preview_accent_blue_emery,
                R.drawable.watch_preview_accent_red_emery,
                R.drawable.watch_preview_accent_orange_emery,
                R.drawable.watch_preview_accent_purple_emery,
                R.drawable.watch_preview_accent_teal_emery,
                R.drawable.watch_preview_accent_pink_emery,
                R.drawable.watch_preview_accent_yellow_emery,
                R.drawable.watch_preview_accent_cyan_emery,
                R.drawable.watch_preview_accent_lime_emery,
                R.drawable.watch_preview_accent_tan_emery,
                R.drawable.watch_preview_accent_navy_emery,
                R.drawable.watch_preview_accent_amber_emery,
                R.drawable.watch_preview_accent_grey_emery,
                R.drawable.watch_preview_accent_black_emery,
                R.drawable.watch_preview_accent_white_emery,
            ),
            WatchSetting.INVERT to listOf(
                R.drawable.watch_preview_invert_off_emery,
                R.drawable.watch_preview_invert_on_emery,
            ),
            WatchSetting.GLYPH_PACK to listOf(
                R.drawable.watch_preview_glyph_classic_emery,
                R.drawable.watch_preview_glyph_bold_emery,
                R.drawable.watch_preview_glyph_outline_emery,
            ),
            WatchSetting.ARROW_LEFT to listOf(
                R.drawable.watch_preview_arrow_right_emery,
                R.drawable.watch_preview_arrow_left_emery,
            ),
            WatchSetting.ETA_MODE to listOf(
                R.drawable.watch_preview_eta_arrival_emery,
                R.drawable.watch_preview_eta_remaining_emery,
            ),
            WatchSetting.ETA_LABEL to listOf(
                R.drawable.watch_preview_label_labelled_emery,
                R.drawable.watch_preview_label_compact_emery,
            ),
            WatchSetting.UNITS to listOf(
                R.drawable.watch_preview_units_metric_emery,
                R.drawable.watch_preview_units_imperial_emery,
            ),
        ),
    )
}
