package com.pebblentn.app.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** #94 (REQ-ANDROID-018, FR7.2, FR7.3): the stale-check item and the weekly-check question. */
class UpdatePromptTest {

    @Test
    fun staleCheckPutsTheCheckItemFirst() {
        assertEquals(UpdatePrompt.STALE_CHECK, updatePrompt(stale = true, autoCheckEnabled = false, staleCheckFinished = false))
        assertEquals(UpdatePrompt.STALE_CHECK, updatePrompt(stale = true, autoCheckEnabled = true, staleCheckFinished = false))
    }

    @Test
    fun finishedStaleCheckWithWeeklyOffAsksTheQuestion() {
        // The check just ran, so the repository is no longer stale.
        assertEquals(UpdatePrompt.WEEKLY_QUESTION, updatePrompt(stale = false, autoCheckEnabled = false, staleCheckFinished = true))
        // Even a failed check, which still records its attempt, asks.
        assertEquals(UpdatePrompt.WEEKLY_QUESTION, updatePrompt(stale = true, autoCheckEnabled = false, staleCheckFinished = true))
    }

    @Test
    fun weeklyOnNeverAsksTheQuestion() {
        for (stale in listOf(true, false)) {
            for (finished in listOf(true, false)) {
                assertNotEquals(
                    "stale=$stale finished=$finished",
                    UpdatePrompt.WEEKLY_QUESTION,
                    updatePrompt(stale = stale, autoCheckEnabled = true, staleCheckFinished = finished),
                )
            }
        }
        assertEquals(UpdatePrompt.NONE, updatePrompt(stale = false, autoCheckEnabled = true, staleCheckFinished = true))
    }

    /** REQ-ANDROID-022 (FR2a): a Google Play install never gets the stale item or the weekly question. */
    @Test
    fun noGitHubCheckMeansNoPrompt() {
        for (stale in listOf(true, false)) {
            for (finished in listOf(true, false)) {
                assertEquals(
                    "stale=$stale finished=$finished",
                    UpdatePrompt.NONE,
                    updatePrompt(stale = stale, autoCheckEnabled = false, staleCheckFinished = finished, githubCheckAllowed = false),
                )
            }
        }
    }

    @Test
    fun nothingPendingShowsNothing() {
        assertEquals(UpdatePrompt.NONE, updatePrompt(stale = false, autoCheckEnabled = false, staleCheckFinished = false))
    }
}
