package com.pebblentn.app.ui.dashboard

/**
 * The value of the Status section's "Watchapp version" row: "{version} (last seen)" through
 * [lastSeen] when a version is stored, shown exactly as stored, or [none] when it is null or blank.
 * "Last seen" because the phone keeps the version from the last WATCH_READY it received and never
 * clears it, so the row proves only that one arrived at least once. Pure, so the text is unit-tested
 * apart from the Compose row.
 */
fun watchAppVersionText(version: String?, lastSeen: (String) -> String, none: String): String =
    if (version.isNullOrBlank()) none else lastSeen(version)
