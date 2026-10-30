package com.pebblentn.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * REQ-ANDROID-020 (#119, FR1): the install record classifies the installing package, keeps the raw
 * package names and install times, and a failed lookup is unknown with no other values.
 */
class InstallRecordTest {

    @Test
    fun playFdroidAndObtainiumAreRecognisedByPackage() {
        assertEquals(InstallKind.PLAY, InstallRecords.classify("com.android.vending"))
        for (pkg in listOf("org.fdroid.fdroid", "org.fdroid.basic", "com.looker.droidify", "com.machiav3lli.fdroid")) {
            assertEquals(pkg, InstallKind.FDROID, InstallRecords.classify(pkg))
        }
        assertEquals(InstallKind.OBTAINIUM, InstallRecords.classify("dev.imranr.obtainium"))
        assertEquals(InstallKind.OBTAINIUM, InstallRecords.classify("dev.imranr.obtainium.fdroid"))
    }

    @Test
    fun lookAlikesOfKnownStoresAreAnotherInstaller() {
        for (pkg in listOf("com.android.vending.billing", "com.google.android.vending", "dev.imranr.obtainiumx")) {
            assertEquals(pkg, InstallKind.OTHER, InstallRecords.classify(pkg))
        }
    }

    @Test
    fun noInstallerThePackageInstallersAndTheShellAreManual() {
        for (pkg in listOf(null, "", "com.google.android.packageinstaller", "com.android.packageinstaller", "com.android.shell")) {
            assertEquals("$pkg", InstallKind.MANUAL, InstallRecords.classify(pkg))
        }
    }

    @Test
    fun anotherStoreIsOtherAndKeepsItsPackageName() {
        val record = InstallRecords.from("com.aurora.store", "com.aurora.store", null, 1_000L, 1_000L)
        assertEquals(InstallKind.OTHER, record.kind)
        assertEquals("com.aurora.store", record.installingPackage)
        assertEquals("com.aurora.store", record.initiatingPackage)
        assertNull(record.originatingPackage)
    }

    @Test
    fun updatedSinceInstallComparesTheTwoTimes() {
        assertEquals(false, InstallRecords.from("com.android.vending", null, null, 1_000L, 1_000L).updatedSinceInstall)
        assertEquals(true, InstallRecords.from("com.android.vending", null, null, 1_000L, 2_000L).updatedSinceInstall)
        assertNull(InstallRecords.from("com.android.vending", null, null, null, 2_000L).updatedSinceInstall)
    }

    @Test
    fun aFailedLookupIsUnknownWithNoValues() {
        val record = InstallRecords.detectWith { throw SecurityException("no access") }
        assertEquals(InstallKind.UNKNOWN, record.kind)
        assertNull(record.installingPackage)
        assertNull(record.initiatingPackage)
        assertNull(record.originatingPackage)
        assertNull(record.firstInstallTime)
        assertNull(record.lastUpdateTime)
        assertNull(record.updatedSinceInstall)
    }

    @Test
    fun aSuccessfulLookupIsReturnedAsIs() {
        val expected = InstallRecords.from("org.fdroid.fdroid", "org.fdroid.fdroid", null, 1L, 2L)
        assertEquals(expected, InstallRecords.detectWith { expected })
    }
}
