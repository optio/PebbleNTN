package com.pebblentn.app.core

/**
 * A single normalized turn instruction produced by the rule engine from one notification.
 *
 * All fields except [maneuver] are optional because a given notification/rule may not supply them.
 * This is the immutable domain output of parsing; it carries no session or transport concerns.
 *
 * @property maneuver the turn maneuver; [Maneuver.UNKNOWN] when it could not be determined.
 * @property distanceMeters distance to the maneuver in meters, or null if absent. Never negative.
 * @property primaryText main line (typically the road/step text).
 * @property secondaryText optional secondary line.
 * @property etaEpochSeconds estimated arrival time as a Unix epoch second, or null.
 */
data class NavigationInstruction(
    val maneuver: Maneuver = Maneuver.UNKNOWN,
    val distanceMeters: Int? = null,
    val primaryText: String? = null,
    val secondaryText: String? = null,
    val etaEpochSeconds: Long? = null,
) {
    init {
        require(distanceMeters == null || distanceMeters >= 0) {
            "distanceMeters must be null or non-negative, was $distanceMeters"
        }
    }
}
