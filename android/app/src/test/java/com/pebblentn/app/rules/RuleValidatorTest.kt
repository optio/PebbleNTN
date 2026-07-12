package com.pebblentn.app.rules

import org.junit.Assert.assertTrue
import org.junit.Test

class RuleValidatorTest {

    private fun ruleJson(
        id: String = "turn-right-en",
        priority: Int = 100,
        conditions: String = """[{"field":"combinedText","operator":"regex","value":"(?i)turn right"}]""",
        output: String = """{"maneuver":{"type":"literal","value":"RIGHT"}}""",
    ) = """
        {"id":"$id","enabled":true,"priority":$priority,"packageNames":["com.google.android.apps.maps"],
         "conditions":$conditions,"output":$output}
    """.trimIndent()

    private fun invalidErrors(json: String): List<String> {
        val result = RuleValidator.validate(json)
        assertTrue("expected Invalid but was $result", result is RuleValidationResult.Invalid)
        return (result as RuleValidationResult.Invalid).errors
    }

    @Test
    fun validRuleIsAccepted() {
        val result = RuleValidator.validate(ruleJson())
        assertTrue(result is RuleValidationResult.Valid)
    }

    @Test
    fun malformedJsonIsInvalid() {
        assertTrue(invalidErrors("{ not json ").isNotEmpty())
    }

    @Test
    fun unknownOperatorIsInvalid() {
        val json = ruleJson(conditions = """[{"field":"text","operator":"matches","value":"x"}]""")
        assertTrue(invalidErrors(json).isNotEmpty())
    }

    @Test
    fun badIdIsReported() {
        val errors = invalidErrors(ruleJson(id = "Turn_Right"))
        assertTrue(errors.any { it.contains("kebab-case") })
    }

    @Test
    fun equalsWithoutValueIsReported() {
        val json = ruleJson(conditions = """[{"field":"text","operator":"equals"}]""")
        assertTrue(invalidErrors(json).any { it.contains("requires a non-empty 'value'") })
    }

    @Test
    fun inWithoutValuesIsReported() {
        val json = ruleJson(conditions = """[{"field":"category","operator":"in"}]""")
        assertTrue(invalidErrors(json).any { it.contains("non-empty 'values'") })
    }

    @Test
    fun existsWithoutValueIsValid() {
        val json = ruleJson(conditions = """[{"field":"text","operator":"exists"}]""")
        assertTrue(RuleValidator.validate(json) is RuleValidationResult.Valid)
    }

    @Test
    fun invalidRegexIsReported() {
        val json = ruleJson(conditions = """[{"field":"text","operator":"regex","value":"([unclosed"}]""")
        assertTrue(invalidErrors(json).any { it.contains("invalid regex") })
    }

    @Test
    fun overlongRegexIsReported() {
        val huge = "a".repeat(SafeRegex.MAX_PATTERN_LENGTH + 1)
        val json = ruleJson(conditions = """[{"field":"text","operator":"regex","value":"$huge"}]""")
        assertTrue(invalidErrors(json).any { it.contains("exceeds") })
    }

    @Test
    fun priorityOutOfRangeIsReported() {
        assertTrue(invalidErrors(ruleJson(priority = 200_000)).any { it.contains("priority") })
    }

    @Test
    fun invalidRegexCaptureExtractorIsReported() {
        val json = ruleJson(output = """{"primaryText":{"type":"regexCapture","field":"text","pattern":"([bad"}}""")
        assertTrue(invalidErrors(json).any { it.contains("regexCapture") })
    }
}
