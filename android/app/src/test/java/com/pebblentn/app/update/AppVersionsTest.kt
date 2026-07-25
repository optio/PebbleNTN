package com.pebblentn.app.update

import org.junit.Assert.assertFalse
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
}
