package com.pebblentn.app.rules

import android.content.Context

/** Supplies the active rules, grouped by precedence layer, to the engine. */
fun interface RuleRepository {
    fun current(): LayeredRules
}

/**
 * Loads bundled official rulesets from app assets (the `rules/bundled` directory, staged from the
 * repo). User and downloaded layers are empty until the editor (M5) and remote updates (M11) land.
 * Bundled
 * rules are parsed once and cached; a malformed bundled file is skipped rather than crashing the
 * app (it is a packaging error, surfaced in logs).
 */
class AssetRuleRepository(
    private val context: Context,
    private val onParseError: (String, Throwable) -> Unit = { _, _ -> },
) : RuleRepository {

    private val bundled: List<Rule> by lazy { loadBundled() }

    override fun current(): LayeredRules = LayeredRules(bundled = bundled)

    private fun loadBundled(): List<Rule> {
        val assets = context.assets
        val files = runCatching { assets.list(BUNDLED_DIR)?.filter { it.endsWith(".json") } }
            .getOrNull().orEmpty()
        return files.flatMap { name ->
            runCatching {
                val text = assets.open("$BUNDLED_DIR/$name").bufferedReader().use { it.readText() }
                RulesetCodec.parse(text).rules
            }.getOrElse { error ->
                onParseError(name, error)
                emptyList()
            }
        }
    }

    private companion object {
        const val BUNDLED_DIR = "rules/bundled"
    }
}
