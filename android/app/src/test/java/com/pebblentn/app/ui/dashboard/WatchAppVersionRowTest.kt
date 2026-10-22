package com.pebblentn.app.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

/** REQ-ANDROID-019 (FR1.2, FR1.3): the Status section's "Watchapp version" row text. */
class WatchAppVersionRowTest {

    private fun text(version: String?) = watchAppVersionText(version, lastSeen = { "$it (last seen)" }, none = NONE)

    @Test
    fun storedVersionIsShownAsLastSeen() {
        assertEquals("0.0.73 (last seen)", text("0.0.73"))
    }

    @Test
    fun noStoredVersionIsNotReceivedYet() {
        assertEquals(NONE, text(null))
        assertEquals(NONE, text(""))
        assertEquals(NONE, text("  "))
    }

    @Test
    fun versionOutsideTheVersionRuleIsShownAsStored() {
        assertEquals("0.0.72-debug (last seen)", text("0.0.72-debug"))
    }

    private companion object {
        const val NONE = "Not received yet"
    }
}
