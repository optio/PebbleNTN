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
        // Bundled rulesets are organized by app then language (rules/bundled/<app>/<language>.json),
        // so the asset tree must be walked recursively — AssetManager.list() returns immediate
        // children only. Every .json found is parsed and its rules flattened into one bundled layer.
        val files = runCatching { listJsonAssetsRecursively(BUNDLED_DIR) }.getOrNull().orEmpty()
        return files.flatMap { path ->
            runCatching {
                val text = assets.open(path).bufferedReader().use { it.readText() }
                RulesetCodec.parse(text).rules
            }.getOrElse { error ->
                onParseError(path, error)
                emptyList()
            }
        }
    }

    /** Depth-first list of every `.json` asset path under [dir] (a directory has children; a file
     *  lists as empty). Ordered for determinism so bundled precedence is stable across runs. */
    private fun listJsonAssetsRecursively(dir: String): List<String> {
        val children = context.assets.list(dir)?.sorted().orEmpty()
        val results = mutableListOf<String>()
        for (name in children) {
            val path = "$dir/$name"
            val grandChildren = context.assets.list(path)
            if (grandChildren.isNullOrEmpty()) {
                if (name.endsWith(".json")) results.add(path)
            } else {
                results.addAll(listJsonAssetsRecursively(path))
            }
        }
        return results
    }

    private companion object {
        const val BUNDLED_DIR = "rules/bundled"
    }
}
