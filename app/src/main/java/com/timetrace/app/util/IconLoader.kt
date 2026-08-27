package com.timetrace.app.util

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Tiny in-memory app-icon cache. Icons are requested a lot across the app
 * (dashboard, apps list, timeline, app detail), and PackageManager icon
 * lookups involve disk I/O, so results are cached for the process lifetime
 * rather than pulling in a full image-loading library just for this.
 */
object IconLoader {
    private val cache = ConcurrentHashMap<String, ImageBitmap?>()

    suspend fun load(context: Context, packageName: String): ImageBitmap? {
        cache[packageName]?.let { return it }

        return withContext(Dispatchers.IO) {
            val bitmap = try {
                context.packageManager
                    .getApplicationIcon(packageName)
                    .toBitmap()
                    .asImageBitmap()
            } catch (e: Exception) {
                // Uninstalled app, adaptive icon that failed to rasterize, etc. -
                // callers fall back to a placeholder icon (see AppIcon composable).
                null
            }
            cache[packageName] = bitmap
            bitmap
        }
    }
}
