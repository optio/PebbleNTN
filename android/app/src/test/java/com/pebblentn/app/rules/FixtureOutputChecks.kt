package com.pebblentn.app.rules

import com.pebblentn.app.catalog.RuleFamilies
import com.pebblentn.app.core.NavigationInstruction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import java.time.ZoneId

/**
 * The #134 fixture fields that every rule regression checks the same way, in step with the Python
 * workbench (`check_fixture`): `compactText`, `secondaryText` and `etaEpochSeconds`, and the
 * `noCompactText` / `noSecondaryText` / `noEta` assertions that a value is absent. A fixture states
 * its post time (`nowEpochSeconds`, default 0) and the phone's `timeZone` (default UTC), so an ETA
 * read from an arrival clock is deterministic.
 */
internal class FixtureOutputChecks(private val families: RuleFamilies = RuleFamilies.NONE) {

    private val engines = mutableMapOf<String, RuleEngine>()

    /** The engine reading arrival clocks in [timeZone], as the phone in that zone would. */
    fun engine(timeZone: String): RuleEngine =
        engines.getOrPut(timeZone) { RuleEngine(families = families, zone = { ZoneId.of(timeZone) }) }

    fun check(
        name: String,
        instruction: NavigationInstruction,
        compactText: String?,
        noCompactText: Boolean,
        secondaryText: String?,
        noSecondaryText: Boolean,
        etaEpochSeconds: Long?,
        noEta: Boolean,
    ) {
        compactText?.let { assertEquals("fixture '$name' compact road line", it, instruction.compactText) }
        if (noCompactText) assertNull("fixture '$name' must have no compact road line", instruction.compactText)
        secondaryText?.let { assertEquals("fixture '$name' second line", it, instruction.secondaryText) }
        if (noSecondaryText) assertNull("fixture '$name' must have no second line", instruction.secondaryText)
        etaEpochSeconds?.let { assertEquals("fixture '$name' ETA epoch", it, instruction.etaEpochSeconds) }
        if (noEta) assertNull("fixture '$name' must have no ETA", instruction.etaEpochSeconds)
    }
}
