package com.pebblentn.app.core

import com.pebblentn.app.protocol.Protocol

/**
 * The watchapp's own settings as the phone mirrors them (REQ-ANDROID-021, #102). These live on the
 * watch and change how it renders and buzzes; the phone only shows and edits them. Not to be
 * confused with [WatchSettings], the phone's fixed flags.
 *
 * One entry per synced setting, in the order of the protocol's SETTING_* keys, which is also the
 * watch's persist order (watchapp/src/c/settings_sync.c keeps the same table). Values are the
 * watch's enum indices; bools are 0/1.
 *
 * @property valueCount how many values the setting has (0 until [valueCount] − 1).
 * @property firstValue the lowest valid value: the early warning has no Off or Short pulse pattern.
 * @property needs the capability bits ([Protocol.CapabilityMasks]) the setting needs.
 * @property defaultValue the value before anyone picks one (the accent's is in [WatchSettingsRules]).
 */
enum class WatchSetting(
    val valueCount: Int,
    val firstValue: Int = 0,
    val needs: Int = 0,
    val defaultValue: Int = 0,
) {
    ACCENT(16),
    UNITS(2),
    INVERT(2),
    GLYPH_PACK(3),
    ARROW_LEFT(2),
    ETA_MODE(2),
    BACKLIGHT(4),
    VIBE_PATTERN(6, defaultValue = WatchValues.VIBE_OFF),
    VIBE_INTENSITY(3, defaultValue = 1),
    BACKLIGHT_COLOR(11, needs = Protocol.CapabilityMasks.RGB_BACKLIGHT_MASK),
    ETA_LABEL(2),
    EARLY_WARN_DISTANCE(6, defaultValue = 2), // 25 m
    EARLY_WARN_REPEAT(2, defaultValue = 1), // every update
    EARLY_WARN_PATTERN(5, firstValue = 1, defaultValue = 2),
    EARLY_WARN_INTENSITY(3, defaultValue = 1),
    TOUCH(2, needs = Protocol.CapabilityMasks.TOUCH_MASK, defaultValue = 1),
    SHORTCUT_UP(ShortcutActions.COUNT, defaultValue = ShortcutActions.ETA_MODE),
    SHORTCUT_DOWN(ShortcutActions.COUNT, defaultValue = ShortcutActions.BACKLIGHT_NOW),
    SHORTCUT_SWIPE_LEFT(ShortcutActions.COUNT, needs = Protocol.CapabilityMasks.TOUCH_MASK, defaultValue = ShortcutActions.ETA_MODE),
    SHORTCUT_SWIPE_RIGHT(ShortcutActions.COUNT, needs = Protocol.CapabilityMasks.TOUCH_MASK, defaultValue = ShortcutActions.ETA_MODE),
    SHORTCUT_SWIPE_UP(ShortcutActions.COUNT, needs = Protocol.CapabilityMasks.TOUCH_MASK),
    SHORTCUT_SWIPE_DOWN(ShortcutActions.COUNT, needs = Protocol.CapabilityMasks.TOUCH_MASK),
    SHORTCUT_TAP(ShortcutActions.COUNT, needs = Protocol.CapabilityMasks.TOUCH_MASK, defaultValue = ShortcutActions.BACKLIGHT_NOW),
    ;

    /** The AppMessage key that carries this setting (protocol 1.3). */
    val key: Int get() = Protocol.Keys.SETTING_ACCENT + ordinal

    val isShortcut: Boolean get() = ordinal >= SHORTCUT_UP.ordinal

    companion object {
        /** The setting a protocol key carries, or null for any other key. */
        fun forKey(key: Int): WatchSetting? = entries.getOrNull(key - Protocol.Keys.SETTING_ACCENT)
    }
}

/** Accent values the watch can show on a black-and-white screen, and the vibration values (theme.h). */
object WatchValues {
    const val ACCENT_GREEN = 0
    const val ACCENT_BLACK = 14
    const val ACCENT_WHITE = 15
    const val VIBE_PULSE = 0
    const val VIBE_OFF = 5
}

/** The watch's shortcut actions (shortcuts.h); values are fixed once released. */
object ShortcutActions {
    const val NONE = 0
    const val ETA_MODE = 1
    const val BACKLIGHT_NOW = 11
    const val NEXT_BACKLIGHT_COLOUR = 12
    const val COUNT = 15
}

/** Which watch model reported the settings (protocol 1.3 WATCH_PLATFORM). */
enum class WatchPlatform(val code: Int) {
    UNKNOWN(Protocol.Platforms.UNKNOWN),
    APLITE(Protocol.Platforms.APLITE),
    BASALT(Protocol.Platforms.BASALT),
    CHALK(Protocol.Platforms.CHALK),
    DIORITE(Protocol.Platforms.DIORITE),
    EMERY(Protocol.Platforms.EMERY),
    ;

    companion object {
        fun fromCode(code: Int?): WatchPlatform = entries.firstOrNull { it.code == code } ?: UNKNOWN
    }
}

/** What the watch can do (protocol 1.3 WATCH_CAPABILITIES): colour, round, RGB backlight, touch. */
@JvmInline
value class WatchCapabilities(val mask: Int) {
    val color: Boolean get() = has(Protocol.CapabilityMasks.COLOR_MASK)
    val round: Boolean get() = has(Protocol.CapabilityMasks.ROUND_MASK)
    val rgbBacklight: Boolean get() = has(Protocol.CapabilityMasks.RGB_BACKLIGHT_MASK)
    val touch: Boolean get() = has(Protocol.CapabilityMasks.TOUCH_MASK)

    fun has(bits: Int): Boolean = mask and bits == bits

    companion object {
        val NONE = WatchCapabilities(0)
    }
}

/** Range and capability rules, the same as the watch's sync_valid / sync_default. */
object WatchSettingsRules {

    /** Whether [value] is one [setting] can take on a watch with [caps]. */
    fun isValid(setting: WatchSetting, value: Int, caps: WatchCapabilities): Boolean {
        if (value < setting.firstValue || value >= setting.valueCount) return false
        if (!caps.has(setting.needs)) return false
        if (setting == WatchSetting.ACCENT && !caps.color) {
            return value == WatchValues.ACCENT_BLACK || value == WatchValues.ACCENT_WHITE
        }
        if (setting.isShortcut && value == ShortcutActions.NEXT_BACKLIGHT_COLOUR) return caps.rgbBacklight
        return true
    }

    /** Whether the watch offers [setting] at all with [caps] (Backlight colour, Touch, the gestures). */
    fun isOffered(setting: WatchSetting, caps: WatchCapabilities): Boolean = caps.has(setting.needs)

    fun defaultValue(setting: WatchSetting, caps: WatchCapabilities): Int =
        if (setting == WatchSetting.ACCENT) {
            if (caps.color) WatchValues.ACCENT_GREEN else WatchValues.ACCENT_BLACK
        } else {
            setting.defaultValue
        }

    /**
     * The values a list offers for [setting], in the watch menu's order: Off first for the New
     * instruction pattern (#102 FR8), Black and White only for a black-and-white accent, no Next
     * backlight colour action without an RGB backlight.
     */
    fun options(setting: WatchSetting, caps: WatchCapabilities): List<Int> = when (setting) {
        WatchSetting.VIBE_PATTERN -> listOf(WatchValues.VIBE_OFF) + (0 until WatchValues.VIBE_OFF)
        else -> (setting.firstValue until setting.valueCount).filter { isValid(setting, it, caps) }
    }
}

/**
 * One full set of watch settings with the revision it belongs to and the watch that sent it.
 * [values] holds every setting; [WatchSettingsModel.fromWatch] fills in the defaults for any key a
 * message left out.
 */
data class WatchSettingsModel(
    val values: Map<WatchSetting, Int>,
    val revision: Int,
    val platform: WatchPlatform = WatchPlatform.UNKNOWN,
    val capabilities: WatchCapabilities = WatchCapabilities.NONE,
) {
    operator fun get(setting: WatchSetting): Int =
        values[setting] ?: WatchSettingsRules.defaultValue(setting, capabilities)

    fun with(setting: WatchSetting, value: Int): WatchSettingsModel = copy(values = values + (setting to value))

    val accent: Int get() = this[WatchSetting.ACCENT]
    val units: Int get() = this[WatchSetting.UNITS]
    val inverted: Boolean get() = this[WatchSetting.INVERT] == 1
    val glyphPack: Int get() = this[WatchSetting.GLYPH_PACK]
    val arrowLeft: Boolean get() = this[WatchSetting.ARROW_LEFT] == 1
    val etaMode: Int get() = this[WatchSetting.ETA_MODE]
    val backlight: Int get() = this[WatchSetting.BACKLIGHT]
    val vibePattern: Int get() = this[WatchSetting.VIBE_PATTERN]
    val vibeIntensity: Int get() = this[WatchSetting.VIBE_INTENSITY]
    val backlightColor: Int get() = this[WatchSetting.BACKLIGHT_COLOR]
    val etaLabel: Int get() = this[WatchSetting.ETA_LABEL]
    val earlyWarnDistance: Int get() = this[WatchSetting.EARLY_WARN_DISTANCE]
    val earlyWarnRepeat: Int get() = this[WatchSetting.EARLY_WARN_REPEAT]
    val earlyWarnPattern: Int get() = this[WatchSetting.EARLY_WARN_PATTERN]
    val earlyWarnIntensity: Int get() = this[WatchSetting.EARLY_WARN_INTENSITY]
    val touchEnabled: Boolean get() = this[WatchSetting.TOUCH] == 1
    val shortcutUp: Int get() = this[WatchSetting.SHORTCUT_UP]
    val shortcutDown: Int get() = this[WatchSetting.SHORTCUT_DOWN]
    val shortcutSwipeLeft: Int get() = this[WatchSetting.SHORTCUT_SWIPE_LEFT]
    val shortcutSwipeRight: Int get() = this[WatchSetting.SHORTCUT_SWIPE_RIGHT]
    val shortcutSwipeUp: Int get() = this[WatchSetting.SHORTCUT_SWIPE_UP]
    val shortcutSwipeDown: Int get() = this[WatchSetting.SHORTCUT_SWIPE_DOWN]
    val shortcutTap: Int get() = this[WatchSetting.SHORTCUT_TAP]

    companion object {
        /** The settings a new watch has. */
        fun defaults(
            platform: WatchPlatform = WatchPlatform.UNKNOWN,
            capabilities: WatchCapabilities = WatchCapabilities.NONE,
            revision: Int = 0,
        ): WatchSettingsModel = WatchSettingsModel(
            values = WatchSetting.entries.associateWith { WatchSettingsRules.defaultValue(it, capabilities) },
            revision = revision,
            platform = platform,
            capabilities = capabilities,
        )

        /**
         * A set as the watch reported it: values in range are kept, anything missing or out of range
         * reads as its default. The capability check is left to the watch, whose own settings these are.
         */
        fun fromWatch(
            carried: Map<WatchSetting, Int>,
            revision: Int,
            platform: WatchPlatform,
            capabilities: WatchCapabilities,
        ): WatchSettingsModel {
            val defaults = defaults(platform, capabilities, revision)
            val kept = carried.filter { (setting, value) -> value >= setting.firstValue && value < setting.valueCount }
            return defaults.copy(values = defaults.values + kept)
        }
    }
}
