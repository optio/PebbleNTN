package com.pebblentn.app.rules

import com.pebblentn.app.notification.NotificationSnapshot

/** Result of running an [Extractor]: text, a number, or nothing. */
sealed interface ExtractionResult {
    data class Text(val value: String) : ExtractionResult
    data class Num(val value: Long) : ExtractionResult
    data object None : ExtractionResult
}

/**
 * Runs a single fixed extractor against a snapshot. Pure and deterministic; produces typed values
 * that the engine assembles into a NavigationInstruction (spec/200-architecture/RuleEngine.md).
 */
class ExtractorRunner(
    private val regexBudgetMillis: Long = SafeRegex.DEFAULT_BUDGET_MILLIS,
) {
    fun run(extractor: Extractor, snapshot: NotificationSnapshot): ExtractionResult = when (extractor) {
        is LiteralExtractor -> ExtractionResult.Text(extractor.value)

        is FieldCopyExtractor -> textOrNone(field(snapshot, extractor.field))

        is FirstNonEmptyExtractor ->
            textOrNone(extractor.fields.firstNotNullOfOrNull { field(snapshot, it)?.takeIf(String::isNotEmpty) })

        is RegexCaptureExtractor -> {
            val value = field(snapshot, extractor.field)
            if (value == null) {
                ExtractionResult.None
            } else {
                val pattern = SafeRegex.compile(extractor.pattern)
                textOrNone(SafeRegex.captureGroup(pattern, value, extractor.group, regexBudgetMillis))
            }
        }

        is DistanceExtractor ->
            field(snapshot, extractor.field)?.let(DistanceParser::parseMeters)?.let { ExtractionResult.Num(it.toLong()) }
                ?: ExtractionResult.None

        is DurationExtractor ->
            field(snapshot, extractor.field)?.let(DurationParser::parseSeconds)?.let { ExtractionResult.Num(it.toLong()) }
                ?: ExtractionResult.None

        is ManeuverMapExtractor -> runManeuverMap(extractor, snapshot)

        is NormalizeStringExtractor -> {
            val value = field(snapshot, extractor.field)
            if (value == null) ExtractionResult.None else ExtractionResult.Text(normalize(value, extractor))
        }

        is BoundedJoinExtractor -> {
            val joined = extractor.fields
                .mapNotNull { field(snapshot, it)?.takeIf(String::isNotEmpty) }
                .joinToString(extractor.separator)
            if (joined.isEmpty()) ExtractionResult.None else ExtractionResult.Text(bound(joined, extractor.maxLength))
        }
    }

    private fun runManeuverMap(extractor: ManeuverMapExtractor, snapshot: NotificationSnapshot): ExtractionResult {
        val value = field(snapshot, extractor.field)
        val mapped = when {
            value == null -> null
            extractor.mapping.containsKey(value) -> extractor.mapping[value]
            else -> extractor.mapping.entries.firstOrNull { it.key.equals(value, ignoreCase = true) }?.value
        }
        return textOrNone(mapped ?: extractor.default)
    }

    private fun field(snapshot: NotificationSnapshot, name: String): String? =
        SnapshotFields.resolve(snapshot, name)

    private fun textOrNone(value: String?): ExtractionResult =
        if (value.isNullOrEmpty()) ExtractionResult.None else ExtractionResult.Text(value)

    private fun normalize(value: String, extractor: NormalizeStringExtractor): String {
        var result = value
        if (extractor.collapseWhitespace) result = result.replace(WHITESPACE, " ")
        if (extractor.trim) result = result.trim()
        if (extractor.lowercase) result = result.lowercase()
        return result
    }

    private fun bound(value: String, maxLength: Int): String {
        if (value.length <= maxLength) return value
        var end = maxLength
        if (end > 0 && Character.isHighSurrogate(value[end - 1])) end -= 1
        return value.substring(0, end)
    }

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }
}
