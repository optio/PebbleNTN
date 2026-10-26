package com.pebblentn.app.update

/** The message a manual "Check for updates" shows (REQ-ANDROID-013, REQ-ANDROID-019). */
sealed interface UpdateCheckMessage {
    /** Neither GitHub nor the watchapp version reports a newer version. */
    object UpToDate : UpdateCheckMessage

    /** The GitHub fetch failed and the watchapp version shows no update. */
    object Failed : UpdateCheckMessage

    /** A newer [version] is known, with any leading `v`/`V` removed. */
    data class Available(val version: String) : UpdateCheckMessage
}

/**
 * Pick the manual check's message from the GitHub [outcome] and the combined [state] read after it,
 * so the message never contradicts the update card. The version comparison is the default and needs
 * no network: an update the watchapp version reveals is reported even when the GitHub fetch failed.
 * [UpdateCheckOutcome.SKIPPED_NOT_DUE] (never seen on the forced manual path) and
 * [UpdateCheckOutcome.NOT_ALLOWED] (a Google Play install, which has no manual check) map like
 * [UpdateCheckOutcome.UP_TO_DATE].
 */
fun updateCheckMessage(outcome: UpdateCheckOutcome, state: UpdateState): UpdateCheckMessage {
    val latest = state.effectiveLatest
    if (state.updateAvailable && latest != null) {
        val trimmed = latest.trim()
        val version = if (trimmed.startsWith('v') || trimmed.startsWith('V')) trimmed.substring(1) else trimmed
        return UpdateCheckMessage.Available(version)
    }
    return if (outcome == UpdateCheckOutcome.FAILED) UpdateCheckMessage.Failed else UpdateCheckMessage.UpToDate
}
