package com.pebblentn.app.rules

import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RulesetCodecTest {

    private val exampleRuleset = """
        {
          "schemaVersion": 1,
          "rulesetVersion": "bundled-initial",
          "minimumAppVersionCode": 1,
          "createdAt": "2026-07-12T00:00:00Z",
          "publisher": "PebbleNTN maintainers",
          "rules": [
            {
              "id": "google-maps-example-turn-right-en",
              "enabled": true,
              "priority": 100,
              "packageNames": ["com.google.android.apps.maps"],
              "locales": ["en"],
              "conditions": [
                {"field": "combinedText", "operator": "regex", "value": "(?i)\\bturn\\s+right\\b"}
              ],
              "output": {
                "maneuver": {"type": "literal", "value": "RIGHT"},
                "distanceMeters": {"type": "distance", "field": "combinedText"},
                "primaryText": {"type": "firstNonEmpty", "fields": ["text", "title"]}
              }
            }
          ]
        }
    """.trimIndent()

    @Test
    fun parsesExampleRulesetAndExtractorTypes() {
        val ruleset = RulesetCodec.parse(exampleRuleset)
        assertEquals(1, ruleset.rules.size)
        val rule = ruleset.rules.single()
        assertEquals("google-maps-example-turn-right-en", rule.id)
        assertEquals(ConditionOperator.REGEX, rule.conditions.single().operator)
        assertTrue(rule.output.maneuver is LiteralExtractor)
        assertEquals("RIGHT", (rule.output.maneuver as LiteralExtractor).value)
        assertTrue(rule.output.distanceMeters is DistanceExtractor)
        assertTrue(rule.output.primaryText is FirstNonEmptyExtractor)
    }

    @Test
    fun canonicalizeIsStableAndReparses() {
        val ruleset = RulesetCodec.parse(exampleRuleset)
        val canonical = RulesetCodec.canonicalize(ruleset)
        assertTrue(canonical.contains("  \"schemaVersion\": 1"))
        // Round-trips to an equal object, and canonicalizing again is idempotent.
        assertEquals(ruleset, RulesetCodec.parse(canonical))
        assertEquals(canonical, RulesetCodec.canonicalize(RulesetCodec.parse(canonical)))
    }

    @Test
    fun rulesSortedByPackageThenPriorityDescThenId() {
        val ruleset = RulesetCodec.parse(
            """
            {"schemaVersion":1,"rulesetVersion":"v","minimumAppVersionCode":1,"createdAt":"2026-01-01T00:00:00Z","publisher":"p","rules":[
              {"id":"b-low","enabled":true,"priority":10,"packageNames":["com.b"],"conditions":[],"output":{}},
              {"id":"a-high","enabled":true,"priority":50,"packageNames":["com.a"],"conditions":[],"output":{}},
              {"id":"a-low","enabled":true,"priority":10,"packageNames":["com.a"],"conditions":[],"output":{}}
            ]}
            """.trimIndent(),
        )
        val canonical = RulesetCodec.canonicalize(ruleset)
        val order = RulesetCodec.parse(canonical).rules.map { it.id }
        assertEquals(listOf("a-high", "a-low", "b-low"), order)
    }

    @Test
    fun unknownExtractorTypeIsRejected() {
        val json = """
            {"schemaVersion":1,"rulesetVersion":"v","minimumAppVersionCode":1,"createdAt":"2026-01-01T00:00:00Z","publisher":"p","rules":[
              {"id":"r","enabled":true,"priority":1,"packageNames":["com.a"],"conditions":[],"output":{"maneuver":{"type":"evilEval","value":"x"}}}
            ]}
        """.trimIndent()
        assertThrows(SerializationException::class.java) { RulesetCodec.parse(json) }
    }

    @Test
    fun unknownFieldIsRejected() {
        val json = """
            {"schemaVersion":1,"rulesetVersion":"v","minimumAppVersionCode":1,"createdAt":"2026-01-01T00:00:00Z","publisher":"p","surprise":true,"rules":[]}
        """.trimIndent()
        assertThrows(SerializationException::class.java) { RulesetCodec.parse(json) }
    }

    @Test
    fun unsupportedSchemaVersionIsRejected() {
        val json = """{"schemaVersion":2,"rulesetVersion":"v","minimumAppVersionCode":1,"createdAt":"2026-01-01T00:00:00Z","publisher":"p","rules":[]}"""
        assertThrows(IllegalArgumentException::class.java) { RulesetCodec.parse(json) }
    }

    @Test
    fun duplicateRuleIdIsRejected() {
        val json = """
            {"schemaVersion":1,"rulesetVersion":"v","minimumAppVersionCode":1,"createdAt":"2026-01-01T00:00:00Z","publisher":"p","rules":[
              {"id":"dup","enabled":true,"priority":1,"packageNames":["com.a"],"conditions":[],"output":{}},
              {"id":"dup","enabled":true,"priority":1,"packageNames":["com.a"],"conditions":[],"output":{}}
            ]}
        """.trimIndent()
        assertThrows(IllegalArgumentException::class.java) { RulesetCodec.parse(json) }
    }
}
