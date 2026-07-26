package app.funput.funput.keyboard.ui.emoji

import android.view.View

/**
 * Warms the device-specific catalog after the primary keyboard has had time to render.
 * Scheduling is intentionally cheap; parsing and glyph checks run on a low-priority worker.
 */
object EmojiCatalogPreloader {
    private const val DelayMillis = 750L

    fun schedule(keyboardView: View) {
        val applicationContext = keyboardView.context.applicationContext
        keyboardView.postDelayed({ EmojiCatalogLoader.preload(applicationContext) }, DelayMillis)
    }
}
