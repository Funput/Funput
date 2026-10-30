package app.funput.funput.ui.settings.enterkey.components

import android.content.Context
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import app.funput.funput.ui.kit.theme.FunputUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The size of an app icon in a row: the same as the tinted tile other rows show. */
private val AppIconSize: Dp = 32.dp

/**
 * [packageName]'s launcher icon at row size, or null while it decodes and when the app is gone.
 * Only rows on screen ask, and each icon is decoded once per process, not on every visit.
 */
@Composable
internal fun rememberAppIcon(packageName: String): ImageBitmap? {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { AppIconSize.roundToPx() }
    val icon by produceState(AppIcons.cached(packageName), packageName, sizePx) {
        value = AppIcons.load(context, packageName, sizePx)
    }
    return icon
}

/** An app's own icon, clipped like the row tiles so a list of apps lines up with the rest. */
@Composable
internal fun AppImage(icon: ImageBitmap) {
    Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(AppIconSize).clip(FunputUi.shapes.iconTile))
}

private object AppIcons {
    /** A few hundred row-size icons; sized in bytes so a high-density screen cannot grow it. */
    private val cache = object : LruCache<String, ImageBitmap>(MaxBytes) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.asAndroidBitmap().byteCount
    }

    fun cached(packageName: String): ImageBitmap? = cache.get(packageName)

    suspend fun load(context: Context, packageName: String, sizePx: Int): ImageBitmap? =
        cached(packageName) ?: withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getApplicationIcon(packageName).toBitmap(sizePx, sizePx).asImageBitmap()
            }.getOrNull()
        }?.also { cache.put(packageName, it) }

    private const val MaxBytes = 8 * 1024 * 1024
}
