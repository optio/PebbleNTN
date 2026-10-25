// GENERATED FILE — DO NOT EDIT.
// Source: protocol/protocol-definition.json
// Regenerate: scripts/generate-protocol.sh (protocol/generate.py)
package com.pebblentn.app.protocol

/** AppMessage protocol constants shared with the Pebble watchapp. */
object Protocol {
    const val MAJOR = 1
    const val MINOR = 3

    /** AppMessage dictionary keys. */
    object Keys {
        const val EVENT = 0
        const val PROTOCOL_MAJOR = 1
        const val PROTOCOL_MINOR = 2
        const val MANEUVER = 3
        const val DISTANCE_METERS = 4
        const val PRIMARY_TEXT = 5
        const val SECONDARY_TEXT = 6
        const val ETA_EPOCH_SECONDS = 7
        const val STATE_TIMESTAMP_SECONDS = 8
        const val SESSION_ID = 9
        const val FLAGS = 10
        const val APP_VERSION = 11
        const val ERROR_CODE = 12
        const val STOPS_REMAINING = 13
        const val LATEST_PHONE_VERSION = 14
        const val SETTINGS_REVISION = 15
        const val WATCH_PLATFORM = 16
        const val WATCH_CAPABILITIES = 17
        const val SETTING_ACCENT = 18
        const val SETTING_UNITS = 19
        const val SETTING_INVERT = 20
        const val SETTING_GLYPH_PACK = 21
        const val SETTING_ARROW_LEFT = 22
        const val SETTING_ETA_MODE = 23
        const val SETTING_BACKLIGHT = 24
        const val SETTING_VIBE_PATTERN = 25
        const val SETTING_VIBE_INTENSITY = 26
        const val SETTING_BACKLIGHT_COLOR = 27
        const val SETTING_ETA_LABEL = 28
        const val SETTING_EARLY_WARN_DISTANCE = 29
        const val SETTING_EARLY_WARN_REPEAT = 30
        const val SETTING_EARLY_WARN_PATTERN = 31
        const val SETTING_EARLY_WARN_INTENSITY = 32
        const val SETTING_TOUCH = 33
        const val SETTING_SHORTCUT_UP = 34
        const val SETTING_SHORTCUT_DOWN = 35
        const val SETTING_SHORTCUT_SWIPE_LEFT = 36
        const val SETTING_SHORTCUT_SWIPE_RIGHT = 37
        const val SETTING_SHORTCUT_SWIPE_UP = 38
        const val SETTING_SHORTCUT_SWIPE_DOWN = 39
        const val SETTING_SHORTCUT_TAP = 40
    }

    /** Values for the [Keys.EVENT] field. */
    object Events {
        const val NAVIGATION_UPDATE = 1
        const val NAVIGATION_STOPPED = 2
        const val NO_ACTIVE_NAVIGATION = 3
        const val SETTINGS = 4
        const val WATCH_READY = 10
        const val WATCH_REQUEST_STATE = 11
        const val PHONE_COMPATIBILITY_ERROR = 12
    }

    /** Bit positions for the [Keys.FLAGS] field. */
    object FlagBits {
        const val EXIT_TO_WATCHFACE_ON_STOP = 0
        const val VIBRATE_ON_MANEUVER_CHANGE = 1
        const val ACTIVATE_BACKLIGHT = 2
        const val STATE_IS_STALE = 3
        const val PHONE_UPDATE_AVAILABLE = 4
    }

    /** Precomputed masks for the [Keys.FLAGS] field. */
    object FlagMasks {
        const val EXIT_TO_WATCHFACE_ON_STOP_MASK = 1 shl 0
        const val VIBRATE_ON_MANEUVER_CHANGE_MASK = 1 shl 1
        const val ACTIVATE_BACKLIGHT_MASK = 1 shl 2
        const val STATE_IS_STALE_MASK = 1 shl 3
        const val PHONE_UPDATE_AVAILABLE_MASK = 1 shl 4
    }

    /** Values for the [Keys.MANEUVER] field. */
    object ManeuverCodes {
        const val UNKNOWN = 0
        const val STRAIGHT = 1
        const val SLIGHT_LEFT = 2
        const val LEFT = 3
        const val SHARP_LEFT = 4
        const val SLIGHT_RIGHT = 5
        const val RIGHT = 6
        const val SHARP_RIGHT = 7
        const val UTURN_LEFT = 8
        const val UTURN_RIGHT = 9
        const val ROUNDABOUT = 10
        const val ARRIVE = 11
        const val TRANSIT = 12
    }

    /** Values for the [Keys.ERROR_CODE] field. */
    object ErrorCodes {
        const val NONE = 0
        const val INCOMPATIBLE_PROTOCOL_MAJOR = 1
    }

    /** Bit positions for the [Keys.WATCH_CAPABILITIES] field. */
    object CapabilityBits {
        const val COLOR = 0
        const val ROUND = 1
        const val RGB_BACKLIGHT = 2
        const val TOUCH = 3
    }

    /** Precomputed masks for the [Keys.WATCH_CAPABILITIES] field. */
    object CapabilityMasks {
        const val COLOR_MASK = 1 shl 0
        const val ROUND_MASK = 1 shl 1
        const val RGB_BACKLIGHT_MASK = 1 shl 2
        const val TOUCH_MASK = 1 shl 3
    }

    /** Values for the [Keys.WATCH_PLATFORM] field. */
    object Platforms {
        const val UNKNOWN = 0
        const val APLITE = 1
        const val BASALT = 2
        const val CHALK = 3
        const val DIORITE = 4
        const val EMERY = 5
    }
}
