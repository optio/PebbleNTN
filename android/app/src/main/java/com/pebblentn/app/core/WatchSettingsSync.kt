package com.pebblentn.app.core

/**
 * The phone side of the watch settings sync (REQ-ANDROID-021, #102): pure rules, no Android.
 *
 * Revisions run 0..99 and wrap from 99 to 0; each edit, on either side, moves its side's revision
 * on by one. `a` is newer than `b` when it is 1 to 49 steps ahead; 50 steps apart neither is newer
 * and the watch's settings stand. This mirrors watchapp/src/c/revision.c and settings_sync.c.
 *
 * The phone keeps a mirror of the watch's settings with its own revision, and the settings edited
 * on the phone that the watch has not confirmed yet ([WatchSettingsSyncState.pending]). Those are
 * sent only while the watchapp is open; the controller decides when that is.
 */
object WatchSettingsSync {

    const val REVISION_MODULO = 100

    fun isValidRevision(revision: Int): Boolean = revision in 0 until REVISION_MODULO

    /** Whether revision [a] is newer than [b]; false when either is out of range. */
    fun isNewer(a: Int, b: Int): Boolean {
        if (!isValidRevision(a) || !isValidRevision(b)) return false
        val ahead = (a - b + REVISION_MODULO) % REVISION_MODULO
        return ahead in 1 until REVISION_MODULO / 2
    }

    /** The revision after [revision]: 99 is followed by 0; an out-of-range one restarts at 0. */
    fun next(revision: Int): Int = if (isValidRevision(revision)) (revision + 1) % REVISION_MODULO else 0

    /** What the watch's settings arriving at the phone lead to. */
    enum class Decision {
        /** The watch's settings win: they replace the mirror and the pending edits they carry. */
        ADOPT_WATCH,

        /** The phone's edits are newer: they stay pending and go to the watch. */
        KEEP_PHONE,

        /** The watch confirms what the phone has: the pending edits are done. */
        SAME,
    }

    /**
     * Compare the phone's revision with the watch's. [differs] is whether any setting the watch sent
     * differs from the mirror; [hasPending] whether the phone has edits the watch has not confirmed.
     * The phone keeps its settings only when it really has newer edits; otherwise the watch's win,
     * exactly as on the watch (an equal revision with different values, and 50 apart, go to the watch).
     */
    fun decide(phoneRevision: Int, watchRevision: Int, differs: Boolean, hasPending: Boolean): Decision = when {
        hasPending && isNewer(phoneRevision, watchRevision) -> Decision.KEEP_PHONE
        phoneRevision == watchRevision && !differs -> Decision.SAME
        else -> Decision.ADOPT_WATCH
    }

    /**
     * Merge settings the watch sent ([incoming], all 23 of them from a 1.3 watchapp) into [state].
     * The platform and capabilities always come from the watch.
     */
    fun onWatchSettings(state: WatchSettingsSyncState, incoming: WatchSettingsModel): MergeResult {
        val mirror = state.mirror
        val received = state.copy(receivedFromWatch = true, watchappOutdated = false)
        if (mirror == null) {
            return MergeResult(received.copy(mirror = incoming, pending = emptySet()), Decision.ADOPT_WATCH)
        }
        val differs = WatchSetting.entries.any { incoming[it] != mirror[it] }
        return when (val decision = decide(mirror.revision, incoming.revision, differs, state.pending.isNotEmpty())) {
            Decision.KEEP_PHONE -> MergeResult(
                received.copy(mirror = mirror.copy(platform = incoming.platform, capabilities = incoming.capabilities)),
                decision,
            )
            Decision.SAME -> MergeResult(received.copy(mirror = incoming, pending = emptySet()), decision)
            // The watch carries every setting, so its values replace every pending edit.
            Decision.ADOPT_WATCH -> MergeResult(
                received.copy(mirror = incoming, pending = state.pending - incoming.values.keys),
                decision,
            )
        }
    }

    /**
     * A setting edited on the phone: the mirror takes the value, the phone's revision moves on and
     * the setting is pending until the watch confirms it. Ignored before any watch settings arrived,
     * since the phone cannot know what the watch offers, and for a value the watch would not take.
     */
    fun onPhoneEdit(state: WatchSettingsSyncState, setting: WatchSetting, value: Int): WatchSettingsSyncState {
        val mirror = state.mirror ?: return state
        if (!WatchSettingsRules.isValid(setting, value, mirror.capabilities) || mirror[setting] == value) return state
        return state.copy(
            mirror = mirror.with(setting, value).copy(revision = next(mirror.revision)),
            pending = state.pending + setting,
        )
    }

    /** The values to send for the pending edits, with the phone's revision; null when none are pending. */
    fun outgoing(state: WatchSettingsSyncState): Pair<Int, Map<WatchSetting, Int>>? {
        val mirror = state.mirror ?: return null
        if (state.pending.isEmpty()) return null
        return mirror.revision to state.pending.associateWith { mirror[it] }
    }
}

/**
 * What the phone knows of the watch's settings (REQ-ANDROID-021).
 *
 * @property mirror the watch's settings as last confirmed, with the phone's own edits applied; null
 *   until a watchapp first sent its settings.
 * @property pending settings edited on the phone that the watch has not confirmed yet.
 * @property receivedFromWatch a watchapp has sent its settings at least once (the screen can show them).
 * @property watchappOutdated the watchapp last heard from is too old to send its settings (protocol < 1.3).
 */
data class WatchSettingsSyncState(
    val mirror: WatchSettingsModel? = null,
    val pending: Set<WatchSetting> = emptySet(),
    val receivedFromWatch: Boolean = false,
    val watchappOutdated: Boolean = false,
)

/** The state after watch settings arrived, and what the comparison decided. */
data class MergeResult(val state: WatchSettingsSyncState, val decision: WatchSettingsSync.Decision)
