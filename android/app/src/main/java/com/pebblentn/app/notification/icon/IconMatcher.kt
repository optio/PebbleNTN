package com.pebblentn.app.notification.icon

import java.util.Locale
import kotlin.math.abs

/**
 * A monochrome mask: the alpha channel of an image, row-major, each value 0..255. Navigation
 * arrows are one colour on transparent, so their shape is all in the alpha channel, which also
 * makes the comparison independent of tint and theme (#59).
 */
class AlphaImage(val width: Int, val height: Int, val alpha: IntArray) {
    init {
        require(width > 0 && height > 0 && alpha.size == width * height) { "bad image ${width}x$height/${alpha.size}" }
    }

    /**
     * This image scaled to [size]×[size] by area averaging: each target pixel is the mean of the
     * source pixels it covers, so differences in screen density (84 / 126 / 168 px icons) and the
     * system's own down-scaling of large icons to 48 dp end up as near-identical masks.
     */
    fun scaledTo(size: Int): AlphaImage {
        if (width == size && height == size) return this
        val sum = LongArray(size * size)
        val count = IntArray(size * size)
        for (y in 0 until height) {
            val ty = y * size / height
            for (x in 0 until width) {
                val i = ty * size + x * size / width
                sum[i] += alpha[y * width + x].toLong()
                count[i]++
            }
        }
        // A source smaller than the target leaves some cells uncovered: fill them from the
        // nearest source pixel instead.
        val out = IntArray(size * size) { i ->
            if (count[i] > 0) {
                (sum[i] / count[i]).toInt()
            } else {
                alpha[(i / size * height / size) * width + (i % size * width / size)]
            }
        }
        return AlphaImage(size, size, out)
    }
}

/** A navigation app's own turn drawable, rendered and scaled to [IconMatcher.SIZE]. */
class IconReference(val drawable: String, val maneuver: String, val image: AlphaImage)

/** What [IconMatcher.match] found for a notification's large icon. */
sealed interface IconMatch {
    /** A short, non-personal description for debug history (drawable names and distances only). */
    val diagnostic: String

    data class Matched(val drawable: String, val maneuver: String, val distance: Double, val runnerUp: Double?) : IconMatch {
        override val diagnostic: String
            get() = "$drawable ${fmt(distance)}" + (runnerUp?.let { " (next maneuver ${fmt(it)})" } ?: "")
    }

    data class NoMatch(val reason: String) : IconMatch {
        override val diagnostic: String get() = "no match: $reason"
    }
}

/**
 * Recognises a maneuver from a notification's large icon by comparing it with the navigation app's
 * own turn drawables (#74). Pure: references are rendered elsewhere ([AppIconReferences]).
 *
 * The distance between two masks is the mean absolute alpha difference at [SIZE]×[SIZE], 0..255. A
 * maneuver's distance is that of its closest drawable (several drawables can share a maneuver:
 * Google Maps' on-ramp and turn arrows are identical). The best maneuver wins only when it is
 * close enough ([MAX_DISTANCE]) and clearly closer than the best *other* maneuver ([MIN_MARGIN]);
 * anything else is a [IconMatch.NoMatch], so an unclear icon stays UNKNOWN rather than becoming a
 * wrong arrow.
 *
 * Measured on the route-capture emulator (#59): CoMaps icons are 0–3 from their own drawable and
 * 40+ from any other; Google Maps' are 3–6 from theirs.
 */
object IconMatcher {
    const val SIZE = 32
    const val MAX_DISTANCE = 24.0
    const val MIN_MARGIN = 8.0

    fun match(icon: AlphaImage, references: List<IconReference>): IconMatch {
        if (references.isEmpty()) return IconMatch.NoMatch("no references")
        val mask = icon.scaledTo(SIZE)
        if (mask.alpha.all { it == 0 }) return IconMatch.NoMatch("blank icon")
        val bestPerManeuver = HashMap<String, Pair<String, Double>>()
        for (ref in references) {
            val d = distance(mask, ref.image)
            val current = bestPerManeuver[ref.maneuver]
            if (current == null || d < current.second) bestPerManeuver[ref.maneuver] = ref.drawable to d
        }
        val ranked = bestPerManeuver.entries.sortedBy { it.value.second }
        val (maneuver, best) = ranked[0].key to ranked[0].value
        val runnerUp = ranked.getOrNull(1)?.value?.second
        return when {
            best.second > MAX_DISTANCE -> IconMatch.NoMatch("closest ${best.first} ${fmt(best.second)} is too far")
            runnerUp != null && runnerUp - best.second < MIN_MARGIN ->
                IconMatch.NoMatch("ambiguous: ${best.first} ${fmt(best.second)} vs ${ranked[1].value.first} ${fmt(runnerUp)}")
            else -> IconMatch.Matched(best.first, maneuver, best.second, runnerUp)
        }
    }

    /** Mean absolute alpha difference of two [SIZE]×[SIZE] masks. */
    fun distance(a: AlphaImage, b: AlphaImage): Double {
        require(a.width == b.width && a.height == b.height) { "sizes differ" }
        var sum = 0L
        for (i in a.alpha.indices) sum += abs(a.alpha[i] - b.alpha[i])
        return sum.toDouble() / a.alpha.size
    }
}

private fun fmt(d: Double) = String.format(Locale.ROOT, "%.1f", d)
