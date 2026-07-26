package app.funput.funput.keyboard.background

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

internal class KeyboardBackgroundImageLoader(
    private val contentResolver: ContentResolver,
    private val density: Float,
    private val maxBitmapSizePx: Int = 1200,
) {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var generation = 0
    private var currentRequest: Request? = null
    var bitmap: Bitmap? = null
        private set

    /**
     * The blur radius is part of the request, not just the source.
     *
     * Blur is baked into the bitmap so it costs nothing per frame, which means changing the radius
     * has to produce a new decode — keying only on the source would leave the slider inert.
     */
    fun load(source: String?, blurRadiusDp: Float, onLoaded: () -> Unit) {
        val request = source?.let { Request(it, blurRadiusDp) }
        if (request == currentRequest) return
        currentRequest = request
        generation += 1
        bitmap = null
        if (request == null) {
            onLoaded()
            return
        }
        val requestGeneration = generation
        executor.execute {
            val decoded = decodeBitmap(request.source)?.let { decoded ->
                BitmapBlur.applied(decoded, request.blurRadiusDp, density)
            }
            mainHandler.post {
                if (requestGeneration != generation) return@post
                bitmap = decoded
                onLoaded()
            }
        }
    }

    fun shutdown() {
        generation += 1
        executor.shutdownNow()
        bitmap = null
    }

    private fun decodeBitmap(source: String): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
        }
        openStream(source)?.use { BitmapFactory.decodeStream(it, null, options) }
    }.getOrNull()

    /** Stored assets are plain paths; a theme saved before the asset store holds a content URI. */
    private fun openStream(source: String) =
        if (source.startsWith("/")) {
            File(source).takeIf(File::exists)?.inputStream()
        } else {
            contentResolver.openInputStream(Uri.parse(source))
        }

    private fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (width / sample > maxBitmapSizePx || height / sample > maxBitmapSizePx) {
            sample *= 2
        }
        return sample
    }

    private data class Request(val source: String, val blurRadiusDp: Float)
}
