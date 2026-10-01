package com.pebblentn.app.data

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/** REQ-ANDROID-015 (#29): when the temporary feedback card shows, and how it goes away. */
@RunWith(RobolectricTestRunner::class)
class FeedbackCampaignTest {

    private val campaign = FeedbackCampaign(id = "test-campaign", endDate = LocalDate.of(2026, 12, 31))
    private val today = LocalDate.of(2026, 10, 1)
    private val fresh = FeedbackCampaignState()

    @Test
    fun hiddenUntilTheAppWasUsedForNavigation() {
        assertFalse(campaign.isVisible(today, usedForNavigation = false, state = fresh))
        assertTrue(campaign.isVisible(today, usedForNavigation = true, state = fresh))
    }

    @Test
    fun shownUpToAndIncludingTheEndDateThenGone() {
        assertTrue(campaign.isVisible(LocalDate.of(2026, 12, 31), true, fresh))
        assertFalse(campaign.isVisible(LocalDate.of(2027, 1, 1), true, fresh))
    }

    @Test
    fun dismissedNeverComesBackAndSnoozeLastsUntilItsDate() {
        assertFalse(campaign.isVisible(today, true, FeedbackCampaignState(dismissed = true)))
        val snoozed = FeedbackCampaignState(snoozedUntil = today.plusDays(14))
        assertFalse(campaign.isVisible(today.plusDays(14), true, snoozed))
        assertTrue(campaign.isVisible(today.plusDays(15), true, snoozed))
    }

    @Test
    fun choicesAreRememberedPerCampaign() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        FeedbackCampaignRepository(context, campaign).snooze(today)
        assertEquals(today.plusDays(FeedbackCampaign.SNOOZE_DAYS), FeedbackCampaignRepository(context, campaign).state.value.snoozedUntil)

        FeedbackCampaignRepository(context, campaign).dismiss()
        assertTrue(FeedbackCampaignRepository(context, campaign).state.value.dismissed)

        val next = FeedbackCampaign(id = "next-campaign", endDate = LocalDate.of(2027, 6, 30))
        assertFalse("a new campaign id starts fresh", FeedbackCampaignRepository(context, next).state.value.dismissed)
    }
}
