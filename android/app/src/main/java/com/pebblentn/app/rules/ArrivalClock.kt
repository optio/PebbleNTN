package com.pebblentn.app.rules

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Reads an arrival clock from notification text and turns it into the next moment it shows (#134
 * FR3). Navigation apps print the arrival as a wall-clock time — Google Maps `Arrive 12:12 PM`,
 * OsmAnd `2,79 km • 42 min • 11:00 • 12 km/h` — never as an epoch, so the phone has to place it on
 * the calendar. Pure: the caller passes now and the time zone.
 *
 * Accepted forms: 24-hour `11:00`, `9:05`, `23:59`; 12-hour `12:12 PM`, `9:05 am`, `3:05 p. m.`;
 * and the Chinese day-period prefixes `上午3:05` (AM) and `下午3:05` (PM) that some Android locales
 * use. The minutes are always two digits, and a clock glued to other digits (`123:45`) is not one.
 */
object ArrivalClock {

    /** A minute's grace: "11:00" seen at 11:00:30 is still today's 11:00, not tomorrow's. */
    const val GRACE_SECONDS = 60L

    private val CLOCK = Regex("""(?:(上午|下午)\s*)?(?<!\d)(\d{1,2}):(\d{2})(?!\d)(?:\s*([AaPp])\.?\s*[Mm]\.?)?""")

    /** The first clock in [text], or null when it has none that is valid. */
    fun parse(text: String?): LocalTime? = text?.let { CLOCK.find(it) }?.let(::toTime)

    /** The first clock in [text] as written (`12:12 PM`), or null when it is not a valid clock. */
    fun findText(text: String?): String? = text?.let { CLOCK.find(it) }?.takeIf { toTime(it) != null }?.value

    /** The last clock in [text] (Google Maps' subText may carry more than one number), or null. */
    fun parseLast(text: String?): LocalTime? = text?.let { CLOCK.findAll(it).lastOrNull() }?.let(::toTime)

    /**
     * The next time [time] shows on the clock in [zone] at or after [now] less [GRACE_SECONDS]:
     * today, or tomorrow when it has already passed (a drive past midnight). Whole minutes.
     * [ZonedDateTime] keeps it right on a daylight-saving change day.
     */
    fun next(time: LocalTime, now: Instant, zone: ZoneId): ZonedDateTime {
        val nowZoned = now.atZone(zone)
        val today = nowZoned.toLocalDate().atTime(time).atZone(zone)
        return if (today.isBefore(nowZoned.minusSeconds(GRACE_SECONDS))) {
            nowZoned.toLocalDate().plusDays(1).atTime(time).atZone(zone)
        } else {
            today
        }
    }

    /** [next] as Unix epoch seconds (seconds always 0), for `etaEpochSeconds`. */
    fun nextEpochSeconds(time: LocalTime, nowEpochSeconds: Long, zone: ZoneId): Long =
        next(time, Instant.ofEpochSecond(nowEpochSeconds), zone).toEpochSecond()

    private fun toTime(match: MatchResult): LocalTime? {
        val (prefix, hourText, minuteText, meridiem) = match.destructured
        var hour = hourText.toInt()
        val minute = minuteText.toInt()
        if (minute > 59) return null
        val pm = when {
            meridiem.isNotEmpty() -> meridiem.equals("p", ignoreCase = true)
            prefix.isNotEmpty() -> prefix == "下午"
            else -> null
        }
        if (pm != null) {
            if (hour !in 1..12) return null
            hour = hour % 12 + if (pm) 12 else 0
        } else if (hour > 23) {
            return null
        }
        return LocalTime.of(hour, minute)
    }
}
