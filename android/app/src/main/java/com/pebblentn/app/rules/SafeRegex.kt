package com.pebblentn.app.rules

import java.util.regex.Pattern

/** Thrown when a regex evaluation exceeds its time budget (likely catastrophic backtracking). */
class RegexTimeoutException(message: String) : RuntimeException(message)

/**
 * Bounded regular-expression evaluation (spec/200-architecture/RuleEngine.md "Regex safety").
 *
 * Enforces a maximum pattern length and a maximum tested input length, and aborts evaluation once a
 * time budget elapses. The time budget is enforced without spawning a thread: the matcher reads the
 * input through a [DeadlineCharSequence] that throws once the deadline passes, which unwinds even a
 * pathologically backtracking match.
 */
object SafeRegex {
    const val MAX_PATTERN_LENGTH = 1024
    const val MAX_INPUT_LENGTH = 4096
    const val DEFAULT_BUDGET_MILLIS = 50L

    /** Compile a pattern, rejecting over-long patterns. Callers may cache the result. */
    fun compile(pattern: String): Pattern {
        require(pattern.length <= MAX_PATTERN_LENGTH) {
            "regex pattern exceeds $MAX_PATTERN_LENGTH characters"
        }
        return Pattern.compile(pattern)
    }

    /**
     * True if [pattern] finds a match within [input] (input truncated to [MAX_INPUT_LENGTH]).
     * @throws RegexTimeoutException if evaluation exceeds [budgetMillis].
     */
    fun containsMatch(pattern: Pattern, input: String, budgetMillis: Long = DEFAULT_BUDGET_MILLIS): Boolean {
        val bounded = if (input.length > MAX_INPUT_LENGTH) input.substring(0, MAX_INPUT_LENGTH) else input
        val deadlineNanos = System.nanoTime() + budgetMillis * 1_000_000
        return pattern.matcher(DeadlineCharSequence(bounded, deadlineNanos)).find()
    }

    private class DeadlineCharSequence(
        private val delegate: CharSequence,
        private val deadlineNanos: Long,
    ) : CharSequence {
        override val length: Int get() = delegate.length

        override fun get(index: Int): Char {
            if (System.nanoTime() > deadlineNanos) {
                throw RegexTimeoutException("regex evaluation exceeded time budget")
            }
            return delegate[index]
        }

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
            DeadlineCharSequence(delegate.subSequence(startIndex, endIndex), deadlineNanos)

        override fun toString(): String = delegate.toString()
    }
}
