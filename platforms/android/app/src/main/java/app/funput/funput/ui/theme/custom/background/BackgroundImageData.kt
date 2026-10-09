package app.funput.funput.ui.theme.custom.background

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import app.funput.funput.theme.DominantColors
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Decodes a background image for the editor preview.
 *
 * Small enough not to warrant an image-loading dependency: the editor shows one picture at a time,
 * downscaled, and the keyboard renderer already does its own decoding for the same reason.
 */
@Composable
internal fun rememberBackgroundBitmap(source: String?): State<Bitmap?> {
    val context = LocalContext.current
    return produceState<Bitmap?>(initialValue = null, source) {
        value = source?.let { withContext(Dispatchers.IO) { decode(context, it) } }
    }
}

private fun decode(context: Context, source: String): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    openStream(context, source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
    }
    openStream(context, source)?.use { BitmapFactory.decodeStream(it, null, options) }
}.getOrNull()

/** Stored assets are plain paths; an image just picked is still a content URI. */
private fun openStream(context: Context, source: String) =
    if (source.startsWith("/")) {
        File(source).takeIf(File::exists)?.inputStream()
    } else {
        context.contentResolver.openInputStream(source.toUri())
    }

private fun sampleSize(width: Int, height: Int): Int {
    var sample = 1
    while (width / sample > MaxPreviewPx || height / sample > MaxPreviewPx) sample *= 2
    return sample
}

private const val MaxPreviewPx = 720

/**
 * The colours of the chosen background image.
 *
 * The bitmap is sampled down to a thumbnail first. A palette does not get more accurate with more
 * pixels — the answer is the same to the eye — and reading a full photograph on the main thread's
 * heels is how a picker starts to stutter.
 */
@Composable
internal fun rememberImagePalette(bitmap: Bitmap?): State<List<Int>> =
    produceState(initialValue = emptyList(), bitmap) {
        value = bitmap?.let { source ->
            withContext(Dispatchers.Default) { DominantColors.extract(source.samplePixels()) }
        }.orEmpty()
    }

private fun Bitmap.samplePixels(): IntArray {
    val scaled = scale(SampleSize, SampleSize)
    val pixels = IntArray(SampleSize * SampleSize)
    scaled.getPixels(pixels, 0, SampleSize, 0, 0, SampleSize, SampleSize)
    if (scaled !== this) scaled.recycle()
    return pixels
}

private const val SampleSize = 64
