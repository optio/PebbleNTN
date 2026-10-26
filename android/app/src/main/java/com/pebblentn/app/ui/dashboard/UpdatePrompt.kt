package com.pebblentn.app.ui.dashboard

/**
 * What the dashboard puts first about update checks (REQ-ANDROID-018, #94). Pure, so it is
 * JVM-tested without Compose.
 */
enum class UpdatePrompt {
    /** Nothing extra: "Check for updates now" stays in its usual place in the Updates section. */
    NONE,

    /** The last check is stale (over 30 days, or never): "Check for updates" is the first item. */
    STALE_CHECK,

    /** A check started from the stale item finished and weekly checks are off: offer to turn them on. */
    WEEKLY_QUESTION,
}

/**
 * Pick the prompt. The weekly question wins only after a check started from the stale item
 * finished ([staleCheckFinished]) while weekly checks are off; turning them on, or dismissing
 * the question, clears [staleCheckFinished]. Otherwise a stale check shows the stale item. An
 * install source that never asks GitHub ([githubCheckAllowed] false, a Google Play install,
 * REQ-ANDROID-022) gets neither.
 */
fun updatePrompt(
    stale: Boolean,
    autoCheckEnabled: Boolean,
    staleCheckFinished: Boolean,
    githubCheckAllowed: Boolean = true,
): UpdatePrompt = when {
    !githubCheckAllowed -> UpdatePrompt.NONE
    staleCheckFinished && !autoCheckEnabled -> UpdatePrompt.WEEKLY_QUESTION
    stale -> UpdatePrompt.STALE_CHECK
    else -> UpdatePrompt.NONE
}
