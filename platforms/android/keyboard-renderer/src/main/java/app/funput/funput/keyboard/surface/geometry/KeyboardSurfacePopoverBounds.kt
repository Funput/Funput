package app.funput.funput.keyboard.surface.geometry

import android.view.View
import app.funput.funput.keyboard.layout.KeyBounds

/** Popovers may use the screen above the keyboard, without allocating on each move. */
internal class KeyboardSurfacePopoverBounds(private val host: View) {
    private val screenLocation = IntArray(2)

    fun resolve(): KeyBounds {
        host.getLocationOnScreen(screenLocation)
        return KeyBounds(0f, -screenLocation[1].toFloat(), host.width.toFloat(), host.height.toFloat())
    }
}
