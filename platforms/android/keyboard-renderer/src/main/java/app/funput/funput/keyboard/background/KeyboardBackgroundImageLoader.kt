package app.funput.funput.keyboard.background

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

internal class KeyboardBackgroundImageLoader(
    private val contentResolver: ContentResolver,
    private val maxBitmapSizePx: Int = 1200,
) {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var generation = 0
    private var currentSource: String? = null
    var bitmap: Bitmap? = null
        private set

    fun load(source: String?, onLoaded: () -> Unit) {
        if (source == currentSource) return
        currentSource = source
        generation += 1
        bitmap = null
        if (source == null) {
            onLoaded()
            return
        }
        val requestGeneration = generation
        executor.execute {
            val decoded = decodeBitmap(source)
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
        val uri = Uri.parse(source)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
        }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }.getOrNull()

    private fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (width / sample > maxBitmapSizePx || height / sample > maxBitmapSizePx) {
            sample *= 2
        }
        return sample
    }
}
