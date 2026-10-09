package app.funput.funput.speech.catalog

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.core.view.doOnLayout
import java.io.File

/** Exports only the synthetic catalog panel, never an IME/editor or recognized transcript. */
internal fun snapshotSpeechCatalog(view: View) {
    view.doOnLayout {
        view.postOnAnimation {
            if (view.width <= 0 || view.height <= 0) return@postOnAnimation
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            try {
                view.draw(Canvas(bitmap))
                File(view.context.cacheDir, "speech-panel-preview.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            } finally {
                bitmap.recycle()
            }
        }
    }
}
