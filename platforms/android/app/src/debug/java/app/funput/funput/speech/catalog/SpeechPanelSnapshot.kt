package app.funput.funput.speech.catalog

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.core.view.doOnLayout
import java.io.File
import kotlin.concurrent.thread

/** Exports only the synthetic catalog panel, never an IME/editor or recognized transcript. */
internal fun snapshotSpeechCatalog(view: View) {
    view.doOnLayout {
        view.postOnAnimation {
            if (view.width <= 0 || view.height <= 0) return@postOnAnimation
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val target = File(view.context.cacheDir, "speech-panel-preview.png")
            // Drawing needs the UI thread; PNG encoding and disk I/O must stay off it.
            thread(name = "speech-panel-snapshot") {
                try {
                    target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                } finally {
                    bitmap.recycle()
                }
            }
        }
    }
}
