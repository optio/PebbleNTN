package com.pebblentn.app.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/** The arrival-clock parser and calendar placement behind the `arrivalClock` extractor (#134 FR3, FR4.3). */
class ArrivalClockTest {

    private val utc = ZoneOffset.UTC
    private val brussels = ZoneId.of("Europe/Brussels")

    private fun epoch(dateTime: String, zone: ZoneId = utc): Long =
        LocalDateTime.parse(dateTime).atZone(zone).toEpochSecond()

    @Test
    fun readsTwentyFourHourClocks() {
        assertEquals(LocalTime.of(11, 0), ArrivalClock.parse("2,79 km • 42 min • 11:00 • 12 km/h"))
        assertEquals(LocalTime.of(9, 5), ArrivalClock.parse("9:05"))
        assertEquals(LocalTime.of(23, 59), ArrivalClock.parse("Arrive 23:59"))
    }

    @Test
    fun readsTwelveHourClocks() {
        assertEquals(LocalTime.of(12, 12), ArrivalClock.parse("12:12 PM"))
        assertEquals(LocalTime.of(0, 5), ArrivalClock.parse("12:05 AM"))
        assertEquals(LocalTime.of(21, 5), ArrivalClock.parse("9:05 pm"))
        assertEquals(LocalTime.of(15, 5), ArrivalClock.parse("3:05 p. m."))
        assertEquals(LocalTime.of(15, 5), ArrivalClock.parse("下午3:05"))
        assertEquals(LocalTime.of(0, 30), ArrivalClock.parse("上午12:30"))
    }

    @Test
    fun rejectsTextThatIsNoClock() {
        assertNull(ArrivalClock.parse("25:00"))
        assertNull(ArrivalClock.parse("9:7"))
        assertNull(ArrivalClock.parse("13:05 PM"))
        assertNull(ArrivalClock.parse("12:60"))
        assertNull(ArrivalClock.parse("123:45"))
        assertNull(ArrivalClock.parse("42 min"))
        assertNull(ArrivalClock.parse(null))
    }

    @Test
    fun findTextKeepsTheClockAsWritten() {
        assertEquals("12:12 PM", ArrivalClock.findText("Arrive 12:12 PM · Home"))
        assertEquals("下午3:05", ArrivalClock.findText("下午3:05"))
        assertNull(ArrivalClock.findText("Arrive soon"))
    }

    @Test
    fun parseLastTakesTheLastClock() {
        assertEquals(LocalTime.of(20, 25), ArrivalClock.parseLast("Leave 19:50 · Arrive 20:25"))
    }

    @Test
    fun laterTodayIsToday() {
        val now = epoch("2026-11-10T10:20:00")
        assertEquals(epoch("2026-11-10T11:00:00"), ArrivalClock.nextEpochSeconds(LocalTime.of(11, 0), now, utc))
    }

    @Test
    fun earlierThanNowIsTomorrow() {
        val now = epoch("2026-11-10T23:40:00")
        assertEquals(epoch("2026-11-11T00:15:00"), ArrivalClock.nextEpochSeconds(LocalTime.of(0, 15), now, utc))
    }

    @Test
    fun withinTheGraceMinuteIsStillToday() {
        val now = epoch("2026-11-10T11:00:30")
        assertEquals(epoch("2026-11-10T11:00:00"), ArrivalClock.nextEpochSeconds(LocalTime.of(11, 0), now, utc))
    }

    @Test
    fun wholeMinutesWhateverTheSecondsOfNow() {
        val first = ArrivalClock.nextEpochSeconds(LocalTime.of(11, 0), epoch("2026-11-10T10:20:07"), utc)
        val second = ArrivalClock.nextEpochSeconds(LocalTime.of(11, 0), epoch("2026-11-10T10:20:53"), utc)
        assertEquals(first, second)
        assertEquals(0L, first % 60)
    }

    @Test
    fun readsTheClockInTheGivenZone() {
        // 09:20 UTC is 10:20 in Brussels (CET): 11:00 Brussels is 10:00 UTC the same day.
        val now = epoch("2026-11-10T09:20:00")
        assertEquals(epoch("2026-11-10T10:00:00"), ArrivalClock.nextEpochSeconds(LocalTime.of(11, 0), now, brussels))
    }

    @Test
    fun daylightSavingChangeDay() {
        // 2026-03-29: Brussels moves from UTC+1 to UTC+2 at 02:00. At 01:30 local (00:30 UTC), an
        // arrival of 03:30 local is 01:30 UTC: one hour of wall clock is skipped, so 1 h real time.
        val now = epoch("2026-03-29T00:30:00")
        assertEquals(epoch("2026-03-29T01:30:00"), ArrivalClock.nextEpochSeconds(LocalTime.of(3, 30), now, brussels))
    }
}
