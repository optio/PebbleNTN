package com.pebblentn.app.update

import org.junit.Assert.assertEquals
import org.junit.Test

/** REQ-ANDROID-020 (FR4.1): the installer package name decides where the update card sends the user. */
class InstallSourceTest {

    @Test
    fun fdroidClientsMapToFdroid() {
        for (pkg in listOf("org.fdroid.fdroid", "org.fdroid.basic", "com.looker.droidify", "com.machiav3lli.fdroid")) {
            assertEquals(pkg, InstallSource.FDROID, InstallSources.fromInstaller(pkg))
        }
    }

    @Test
    fun obtainiumAndItsVariantsMapToObtainium() {
        assertEquals(InstallSource.OBTAINIUM, InstallSources.fromInstaller("dev.imranr.obtainium"))
        assertEquals(InstallSource.OBTAINIUM, InstallSources.fromInstaller("dev.imranr.obtainium.fdroid"))
        // A package that merely starts with the same letters is not Obtainium.
        assertEquals(InstallSource.GITHUB, InstallSources.fromInstaller("dev.imranr.obtainiumx"))
    }

    @Test
    fun everythingElseMapsToGitHub() {
        for (pkg in listOf(null, "com.android.packageinstaller", "com.google.android.packageinstaller", "com.android.vending", "com.android.chrome", "")) {
            assertEquals("$pkg", InstallSource.GITHUB, InstallSources.fromInstaller(pkg))
        }
    }
}
