package app.funput.funput.ui.theme.custom.background

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import app.funput.funput.theme.DominantColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    val scaled = Bitmap.createScaledBitmap(this, SampleSize, SampleSize, true)
    val pixels = IntArray(SampleSize * SampleSize)
    scaled.getPixels(pixels, 0, SampleSize, 0, 0, SampleSize, SampleSize)
    if (scaled !== this) scaled.recycle()
    return pixels
}

private const val SampleSize = 64
