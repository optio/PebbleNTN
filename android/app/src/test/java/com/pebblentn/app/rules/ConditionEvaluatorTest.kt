package com.pebblentn.app.rules

import com.pebblentn.app.notification.NotificationSnapshot
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ConditionEvaluatorTest {

    private val evaluator = ConditionEvaluator()

    private fun snapshot(title: String? = null, text: String? = null, category: String? = null) =
        NotificationSnapshot(
            packageName = "com.google.android.apps.maps",
            notificationId = 1,
            title = title,
            text = text,
            category = category,
        )

    private fun cond(field: String, op: ConditionOperator, value: String? = null, values: List<String> = emptyList()) =
        Condition(field = field, operator = op, value = value, values = values)

    @Test
    fun existsAndNotExists() {
        val s = snapshot(title = "Turn right")
        assertTrue(evaluator.evaluate(cond("title", ConditionOperator.EXISTS), s))
        assertFalse(evaluator.evaluate(cond("subText", ConditionOperator.EXISTS), s))
        assertTrue(evaluator.evaluate(cond("subText", ConditionOperator.NOT_EXISTS), s))
    }

    @Test
    fun equalsAndIgnoreCase() {
        val s = snapshot(title = "Turn Right")
        assertTrue(evaluator.evaluate(cond("title", ConditionOperator.EQUALS, "Turn Right"), s))
        assertFalse(evaluator.evaluate(cond("title", ConditionOperator.EQUALS, "turn right"), s))
        assertTrue(evaluator.evaluate(cond("title", ConditionOperator.EQUALS_IGNORE_CASE, "turn right"), s))
    }

    @Test
    fun containsStartsEnds() {
        val s = snapshot(text = "In 500 m, turn right")
        assertTrue(evaluator.evaluate(cond("text", ConditionOperator.CONTAINS, "turn right"), s))
        assertTrue(evaluator.evaluate(cond("text", ConditionOperator.CONTAINS_IGNORE_CASE, "TURN"), s))
        assertTrue(evaluator.evaluate(cond("text", ConditionOperator.STARTS_WITH, "In 500"), s))
        assertTrue(evaluator.evaluate(cond("text", ConditionOperator.ENDS_WITH, "turn right"), s))
        assertFalse(evaluator.evaluate(cond("text", ConditionOperator.STARTS_WITH, "turn"), s))
    }

    @Test
    fun inMembership() {
        val s = snapshot(category = "navigation")
        assertTrue(evaluator.evaluate(cond("category", ConditionOperator.IN, values = listOf("navigation", "recommendation")), s))
        assertFalse(evaluator.evaluate(cond("category", ConditionOperator.IN, values = listOf("email")), s))
    }

    @Test
    fun regexMatch() {
        val s = snapshot(text = "In 500 m, turn right onto Main St")
        assertTrue(evaluator.evaluate(cond("text", ConditionOperator.REGEX, "(?i)\\bturn\\s+right\\b"), s))
        assertFalse(evaluator.evaluate(cond("text", ConditionOperator.REGEX, "(?i)\\bturn\\s+left\\b"), s))
    }

    @Test
    fun missingFieldOrValueIsFalse() {
        val s = snapshot()
        assertFalse(evaluator.evaluate(cond("title", ConditionOperator.EQUALS, "x"), s))
        assertFalse(evaluator.evaluate(cond("title", ConditionOperator.CONTAINS, null), s))
        assertFalse(evaluator.evaluate(cond("title", ConditionOperator.REGEX, "x"), s))
    }

    @Test
    fun overlongPatternIsRejected() {
        val s = snapshot(text = "abc")
        val huge = "a".repeat(SafeRegex.MAX_PATTERN_LENGTH + 1)
        assertThrows(IllegalArgumentException::class.java) {
            evaluator.evaluate(cond("text", ConditionOperator.REGEX, huge), s)
        }
    }

    @Test
    fun regexAbortsWhenTimeBudgetExceeded() {
        // Defense-in-depth against catastrophic backtracking: with a zero budget the deadline is
        // already past, so evaluation must abort deterministically rather than run unbounded.
        val zeroBudget = ConditionEvaluator(regexBudgetMillis = 0L)
        val s = snapshot(text = "some navigation text")
        assertThrows(RegexTimeoutException::class.java) {
            zeroBudget.evaluate(cond("text", ConditionOperator.REGEX, "navigation"), s)
        }
    }

    @Test
    fun regexOnlyTestsBoundedInput() {
        // Match token placed just beyond the input bound must not be found.
        val padded = "x".repeat(SafeRegex.MAX_INPUT_LENGTH) + "NEEDLE"
        val s = snapshot(text = padded)
        assertFalse(evaluator.evaluate(cond("text", ConditionOperator.REGEX, "NEEDLE"), s))
    }
}
