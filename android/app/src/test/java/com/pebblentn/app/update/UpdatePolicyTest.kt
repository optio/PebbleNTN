package com.pebblentn.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** REQ-ANDROID-022 (FR2): a Google Play install gets its updates from Play only; other sources keep v0.0.77. */
class UpdatePolicyTest {

    @Test
    fun playNeverAsksGitHubAndOffersNoOtherSource() {
        val policy = UpdatePolicy.forSource(InstallSource.PLAY)
        assertFalse(policy.allowsGithubCheck)
        assertTrue(policy.ignoresStoredGithubVersion)
        assertFalse(policy.offersOtherSources)
        assertTrue("Play review can take a day or two", policy.showsInstallDelayNote)
    }

    @Test
    fun fdroidKeepsTheGitHubCheckAndItsDelayLine() {
        val policy = UpdatePolicy.forSource(InstallSource.FDROID)
        assertTrue(policy.allowsGithubCheck)
        assertFalse(policy.ignoresStoredGithubVersion)
        assertTrue(policy.offersOtherSources)
        assertTrue(policy.showsInstallDelayNote)
    }

    @Test
    fun gitHubAndObtainiumAreUnchanged() {
        val expected = UpdatePolicy(
            allowsGithubCheck = true,
            ignoresStoredGithubVersion = false,
            offersOtherSources = true,
            showsInstallDelayNote = false,
        )
        assertEquals(expected, UpdatePolicy.forSource(InstallSource.GITHUB))
        assertEquals(expected, UpdatePolicy.forSource(InstallSource.OBTAINIUM))
    }
}
