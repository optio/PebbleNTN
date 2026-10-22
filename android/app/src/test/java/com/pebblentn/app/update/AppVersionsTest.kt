package com.pebblentn.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionsTest {

    @Test
    fun newerWhenLatestIsHigher() {
        assertTrue(AppVersions.isNewer(current = "0.0.13", latest = "0.0.14"))
        assertTrue(AppVersions.isNewer(current = "0.0.13", latest = "0.1.0"))
        assertTrue(AppVersions.isNewer(current = "0.9.9", latest = "1.0.0"))
    }

    @Test
    fun notNewerWhenEqualOrLower() {
        assertFalse(AppVersions.isNewer(current = "0.0.13", latest = "0.0.13"))
        assertFalse(AppVersions.isNewer(current = "1.2.0", latest = "1.1.9"))
        assertFalse(AppVersions.isNewer(current = "1.0.0", latest = "0.9.9"))
    }

    @Test
    fun toleratesLeadingVOnEitherSide() {
        assertTrue(AppVersions.isNewer(current = "0.0.13", latest = "v0.0.14"))
        assertFalse(AppVersions.isNewer(current = "v0.0.14", latest = "0.0.14"))
    }

    @Test
    fun toleratesDifferingComponentCounts() {
        assertFalse(AppVersions.isNewer(current = "1.2", latest = "1.2.0"))
        assertTrue(AppVersions.isNewer(current = "1.2", latest = "1.2.1"))
        assertFalse(AppVersions.isNewer(current = "1.2.0", latest = "1.2"))
    }

    @Test
    fun ignoresPreReleaseSuffix() {
        // A pre-release suffix is dropped, so 0.0.14-rc1 compares equal to 0.0.14 here.
        assertFalse(AppVersions.isNewer(current = "0.0.14", latest = "0.0.14-rc1"))
        assertTrue(AppVersions.isNewer(current = "0.0.13", latest = "0.0.14-rc1"))
    }

    @Test
    fun unparseableLatestNeverReportsAnUpdate() {
        // Junk from a bad tag must not make the app think it is out of date.
        assertFalse(AppVersions.isNewer(current = "0.0.13", latest = "garbage"))
        assertFalse(AppVersions.isNewer(current = "0.0.13", latest = ""))
    }

    // The strict rule (REQ-ANDROID-019), the same cases as watchapp/tests/test_version_compare.c.

    @Test
    fun strictComparesNumericallyWithMissingPartsAsZero() {
        assertTrue(AppVersions.isNewerStrict(current = "0.0.9", candidate = "0.0.10"))
        assertFalse(AppVersions.isNewerStrict(current = "0.0.10", candidate = "0.0.9"))
        assertFalse("1.2 equals 1.2.0", AppVersions.isNewerStrict(current = "1.2.0", candidate = "1.2"))
        assertFalse("1.2.0 equals 1.2", AppVersions.isNewerStrict(current = "1.2", candidate = "1.2.0"))
        assertTrue(AppVersions.isNewerStrict(current = "0.0.71", candidate = "v0.0.72"))
        assertFalse("equal is not newer", AppVersions.isNewerStrict(current = "0.0.71", candidate = "0.0.71"))
    }

    @Test
    fun strictParseAcceptsOnlyPlainDottedDigits() {
        assertEquals(listOf(0, 0, 71), AppVersions.parseStrict("v0.0.71"))
        assertEquals(listOf(1), AppVersions.parseStrict("V1"))
        assertEquals(listOf(0, 0, 999_999_999), AppVersions.parseStrict("0.0.999999999"))
        for (bad in listOf("0.0.71-debug", "0.0.x", "0..1", "0.0.", "0.0 .1", "+0.0.1", "", "v", "0.0.1234567890", " 0.0.1")) {
            assertNull("\"$bad\" is unparsable", AppVersions.parseStrict(bad))
        }
        assertNull(AppVersions.parseStrict(null))
    }

    @Test
    fun strictUnparsableOrMissingIsNeverNewer() {
        assertFalse(AppVersions.isNewerStrict(current = "0.0.70", candidate = "0.0.71-debug"))
        assertFalse(AppVersions.isNewerStrict(current = "0.0.x", candidate = "0.0.72"))
        assertFalse(AppVersions.isNewerStrict(current = "0.0.70", candidate = ""))
        assertFalse(AppVersions.isNewerStrict(current = "0.0.70", candidate = null))
        assertFalse(AppVersions.isNewerStrict(current = null, candidate = "0.0.72"))
    }
}
