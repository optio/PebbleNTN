package com.pebblentn.app.notification.icon

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import java.util.concurrent.ConcurrentHashMap

/**
 * Renders a navigation app's own turn drawables, by resource name, into [IconReference]s (#74), the
 * way the apps draw their notification icon (intrinsic size, ARGB_8888). Cached per package,
 * version, screen density and candidate set, so an app update or a density change re-renders.
 * Needs the package in the manifest's `<queries>` (package visibility, Android 11+).
 */
class AppIconReferences(private val context: Context) {

    private data class Key(val packageName: String, val versionCode: Long, val density: Int, val candidates: Map<String, String>)

    private val cache = ConcurrentHashMap<Key, List<IconReference>>()

    /** The rendered references for [candidates] (drawable → maneuver); drawables not found are skipped. */
    fun references(packageName: String, candidates: Map<String, String>): List<IconReference> {
        val key = Key(packageName, versionCode(packageName), context.resources.displayMetrics.densityDpi, candidates)
        return cache.getOrPut(key) {
            cache.keys.removeAll { it.packageName == packageName }
            render(packageName, candidates)
        }
    }

    // Resource names are part of the apps' public build output; the lookup is by name on purpose.
    @SuppressLint("DiscouragedApi")
    private fun render(packageName: String, candidates: Map<String, String>): List<IconReference> {
        val other = runCatching { context.createPackageContext(packageName, 0) }.getOrNull() ?: return emptyList()
        return candidates.mapNotNull { (name, maneuver) ->
            val id = other.resources.getIdentifier(name, "drawable", packageName)
            if (id == 0) return@mapNotNull null
            val drawable = runCatching { other.getDrawable(id) }.getOrNull() ?: return@mapNotNull null
            alphaOf(drawable)?.let { IconReference(name, maneuver, it.scaledTo(IconMatcher.SIZE)) }
        }
    }

    private fun versionCode(packageName: String): Long =
        runCatching { context.packageManager.getPackageInfo(packageName, 0).longVersionCode }.getOrDefault(-1)

    companion object {
        /** The notification's large icon as an alpha mask, or null when it has none. */
        fun largeIconAlpha(context: Context, notification: Notification): AlphaImage? =
            notification.getLargeIcon()?.let { icon -> runCatching { icon.loadDrawable(context) }.getOrNull() }?.let(::alphaOf)

        private fun alphaOf(drawable: Drawable): AlphaImage? {
            val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: run {
                val w = drawable.intrinsicWidth.coerceIn(1, MAX_SIDE)
                val h = drawable.intrinsicHeight.coerceIn(1, MAX_SIDE)
                Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also {
                    drawable.setBounds(0, 0, w, h)
                    drawable.draw(Canvas(it))
                }
            }
            val w = bitmap.width
            val h = bitmap.height
            if (w <= 0 || h <= 0) return null
            val pixels = IntArray(w * h)
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
            return AlphaImage(w, h, IntArray(pixels.size) { pixels[it] ushr 24 })
        }

        /** Guards against an absurd intrinsic size; real turn icons are under 300 px. */
        private const val MAX_SIDE = 1024
    }
}
