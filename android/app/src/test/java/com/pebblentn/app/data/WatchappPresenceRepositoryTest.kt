package com.pebblentn.app.data

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** #28: the setup checklist's watchapp step, from proof or a manual confirmation. */
@RunWith(RobolectricTestRunner::class)
class WatchappPresenceRepositoryTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun unknownUntilThereIsProof() {
        assertFalse(WatchappPresenceRepository(context).detected.value)
    }

    @Test
    fun proofIsRememberedAcrossRestarts() {
        WatchappPresenceRepository(context).markSeen(1_000)
        assertTrue("a new instance reads the stored proof", WatchappPresenceRepository(context).detected.value)
    }

    @Test
    fun aManualConfirmationCountsAndIsRemembered() {
        val repo = WatchappPresenceRepository(context)
        repo.confirmManually()
        assertTrue(repo.detected.value)
        assertTrue(WatchappPresenceRepository(context).detected.value)
    }
}
