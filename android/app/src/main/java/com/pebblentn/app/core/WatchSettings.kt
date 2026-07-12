package com.pebblentn.app.core

/**
 * User-configurable behavior that influences what the phone tells the watch. Immutable; supplied
 * to the reducer as configuration (REQ-ANDROID-009, REQ-WATCH-005/007/009).
 *
 * @property autoLaunchOnSessionStart launch the watchapp once when a new session starts.
 * @property exitToWatchfaceOnStop ask the watch to return to the watchface when navigation ends.
 * @property vibrateOnManeuverChange allow a single vibration when the maneuver changes.
 * @property activateBacklight briefly activate the backlight on updates.
 */
data class WatchSettings(
    val autoLaunchOnSessionStart: Boolean = true,
    val exitToWatchfaceOnStop: Boolean = true,
    val vibrateOnManeuverChange: Boolean = true,
    val activateBacklight: Boolean = false,
) {
    companion object {
        /** Product defaults (spec/000-overview.md, REQ-ANDROID-009). */
        val DEFAULT = WatchSettings()
    }
}
