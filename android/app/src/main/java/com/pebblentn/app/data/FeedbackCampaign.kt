package com.pebblentn.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/**
 * A temporary, dismissible invitation to report bugs, request features and suggest a new name
 * (REQ-ANDROID-015, #29). It runs until [endDate] and then disappears without a new release; a
 * later campaign uses a new [id], so it can be shown once more even to people who dismissed this one.
 */
data class FeedbackCampaign(val id: String, val endDate: LocalDate) {

    /**
     * Shown only after PebbleNTN has been used for navigation, before the end date, and unless the
     * user dismissed it for good or snoozed it past [today].
     */
    fun isVisible(today: LocalDate, usedForNavigation: Boolean, state: FeedbackCampaignState): Boolean =
        usedForNavigation &&
            !today.isAfter(endDate) &&
            !state.dismissed &&
            (state.snoozedUntil == null || today.isAfter(state.snoozedUntil))

    companion object {
        /** The current campaign. Change the id (and date) to run a new one. */
        val CURRENT = FeedbackCampaign(id = "feedback-2026q4", endDate = LocalDate.of(2026, 12, 31))

        /** "Not now" hides the card for this long. */
        const val SNOOZE_DAYS = 14L
    }
}

/** What the user did with a campaign's card. */
data class FeedbackCampaignState(val dismissed: Boolean = false, val snoozedUntil: LocalDate? = null)

/** Remembers "Not now" and "Don't show again" per campaign id, on this device only. */
class FeedbackCampaignRepository(context: Context, private val campaign: FeedbackCampaign = FeedbackCampaign.CURRENT) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<FeedbackCampaignState> = _state.asStateFlow()

    fun snooze(today: LocalDate) {
        prefs.edit().putLong(key("snoozed_until"), today.plusDays(FeedbackCampaign.SNOOZE_DAYS).toEpochDay()).apply()
        _state.value = read()
    }

    fun dismiss() {
        prefs.edit().putBoolean(key("dismissed"), true).apply()
        _state.value = read()
    }

    private fun read() = FeedbackCampaignState(
        dismissed = prefs.getBoolean(key("dismissed"), false),
        snoozedUntil = prefs.getLong(key("snoozed_until"), -1L).takeIf { it >= 0 }?.let(LocalDate::ofEpochDay),
    )

    private fun key(name: String) = "campaign_${campaign.id}_$name"

    private companion object {
        const val PREFS_NAME = "pebblentn_settings"
    }
}
