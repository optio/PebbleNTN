package com.pebblentn.app.update

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * REQ-ANDROID-020 (FR4.1) and REQ-ANDROID-022: the installer package name decides where the update
 * card sends the user.
 */
class InstallSourceTest {

    @Test
    fun playStoreMapsToPlay() {
        assertEquals(InstallSource.PLAY, InstallSources.fromInstaller("com.android.vending"))
    }

    @Test
    fun onlyThePlayStorePackageItselfIsPlay() {
        // Neighbouring Google packages and look-alikes are not the Play Store.
        for (pkg in listOf("com.android.vending.billing", "com.google.android.vending", "com.android.vendingx")) {
            assertEquals(pkg, InstallSource.GITHUB, InstallSources.fromInstaller(pkg))
        }
    }

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
        for (pkg in listOf(null, "com.android.packageinstaller", "com.google.android.packageinstaller", "com.android.chrome", "")) {
            assertEquals("$pkg", InstallSource.GITHUB, InstallSources.fromInstaller(pkg))
        }
    }
}
