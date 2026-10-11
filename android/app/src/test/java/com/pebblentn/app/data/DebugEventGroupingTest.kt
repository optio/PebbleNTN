package com.pebblentn.app.data

import com.pebblentn.app.notification.NotificationSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** When two consecutive debug events count as the same card (#86). */
class DebugEventGroupingTest {

    // The German classic card from #83's diagnostics, redacted.
    private val card = NotificationSnapshot(
        packageName = "com.google.android.apps.maps",
        notificationId = 1,
        channelId = "1_2",
        category = "navigation",
        postTimeMillis = 1_000,
        title = "100 m",
        text = "▮ ▮ / ▮",
        subText = "7 min · 6,1 km · ▮ ▮. 14:21",
        iconMatch = "no match: closest maneuver_turn_normal_left 31.2 is too far",
    )
    private val versions = EventVersions("0.0.61", 61, "26.40.01", 1_234_567, "google-maps-any-2026.10.11")

    private fun key(
        snapshot: NotificationSnapshot = card,
        disposition: String = DebugDisposition.CAPTURED_UNMATCHED,
        rule: String? = null,
        v: EventVersions = versions,
    ) = DebugEventGrouping.key(snapshot, disposition, rule, v)

    @Test
    fun theSameCardWithOtherNumbersAndAnotherPostTimeGroups() {
        val later = card.copy(
            postTimeMillis = 9_000,
            notificationId = 2,
            title = "90 m",
            subText = "12 min · 8,6 km · ▮ ▮. 14:22",
            iconMatch = "no match: closest maneuver_turn_normal_left 28.9 is too far",
        )
        assertEquals(key(), key(later))
    }

    @Test
    fun aChangeOfUnitStartsANewEvent() {
        // "2,6 km" counting down to "900 m" reads "N km" then "N m": two events, which is fine.
        assertNotEquals(key(card.copy(title = "2,6\u00a0km")), key(card.copy(title = "900\u00a0m")))
    }

    @Test
    fun anotherRoadRuleIconResultOrVersionStartsANewEvent() {
        assertNotEquals(key(), key(card.copy(text = "▮-▮-▮")))
        assertNotEquals(key(), key(disposition = DebugDisposition.MATCHED, rule = "google-maps-icon-step"))
        assertNotEquals(key(), key(card.copy(iconDrawable = "maneuver_keep_right", iconMatch = "maneuver_keep_right 3.1")))
        assertNotEquals(key(), key(v = versions.copy(sourceAppVersionCode = 1_234_568)))
        assertNotEquals(key(), key(v = versions.copy(appVersionCode = 62)))
        assertNotEquals(key(), key(card.copy(packageName = "app.comaps.google")))
        assertNotEquals(key(), key(card.copy(template = "android.app.Notification\$ProgressStyle")))
    }

    @Test
    fun theKeyIsAHashWithoutNotificationText() {
        val k = key()
        assertEquals(64, k.length)
        assertFalse(k.contains("▮"))
    }

    @Test
    fun numberlessReplacesEveryNumber() {
        assertEquals("N min · N km · N:N", DebugEventGrouping.numberless("7 min · 6,1 km · 14:21"))
        assertEquals("N m", DebugEventGrouping.numberless("1.234,5 m"))
        assertEquals("", DebugEventGrouping.numberless(null))
    }
}
