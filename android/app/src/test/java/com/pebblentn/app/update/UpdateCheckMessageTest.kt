package com.pebblentn.app.update

import org.junit.Assert.assertEquals
import org.junit.Test

/** The manual check's message combines GitHub and the watchapp version (REQ-ANDROID-013, REQ-ANDROID-019). */
class UpdateCheckMessageTest {

    @Test
    fun githubUpToDateButNewerWatchappReportsTheWatchVersion() {
        val state = UpdateState(currentVersion = "0.0.74", latestVersion = "0.0.74", watchAppVersion = "0.0.75")

        assertEquals(
            UpdateCheckMessage.Available("0.0.75"),
            updateCheckMessage(UpdateCheckOutcome.UP_TO_DATE, state),
        )
    }

    @Test
    fun neitherSourceNewerIsUpToDate() {
        val state = UpdateState(currentVersion = "0.0.74", latestVersion = "0.0.74", watchAppVersion = "0.0.74")

        assertEquals(UpdateCheckMessage.UpToDate, updateCheckMessage(UpdateCheckOutcome.UP_TO_DATE, state))
    }

    @Test
    fun failedFetchWithNoWatchUpdateIsFailed() {
        val state = UpdateState(currentVersion = "0.0.74", latestVersion = null, watchAppVersion = "0.0.74")

        assertEquals(UpdateCheckMessage.Failed, updateCheckMessage(UpdateCheckOutcome.FAILED, state))
    }

    @Test
    fun failedFetchWithWatchUpdateStillReportsTheVersion() {
        val state = UpdateState(currentVersion = "0.0.74", latestVersion = null, watchAppVersion = "0.0.75")

        assertEquals(
            UpdateCheckMessage.Available("0.0.75"),
            updateCheckMessage(UpdateCheckOutcome.FAILED, state),
        )
    }

    @Test
    fun githubTagHasItsLeadingVRemoved() {
        val state = UpdateState(currentVersion = "0.0.74", latestVersion = "v0.0.76", watchAppVersion = null)

        assertEquals(
            UpdateCheckMessage.Available("0.0.76"),
            updateCheckMessage(UpdateCheckOutcome.UPDATE_AVAILABLE, state),
        )
    }
}
