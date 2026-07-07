package app.funput.funput.keyboard.surface

import android.content.Context
import app.funput.funput.keyboard.background.KeyboardBackgroundImageLoader
import app.funput.funput.theme.KeyboardThemeBackgroundImage

internal class KeyboardSurfaceBackgroundState(
    context: Context,
    private val invalidate: () -> Unit,
) {
    private val loader = KeyboardBackgroundImageLoader(context.contentResolver)
    var image: KeyboardThemeBackgroundImage? = null
        private set
    val bitmap get() = loader.bitmap

    fun update(image: KeyboardThemeBackgroundImage?) {
        if (this.image == image) return
        this.image = image
        loader.load(image?.source, invalidate)
        invalidate()
    }

    fun clear() {
        loader.shutdown()
    }
}
