package app.funput.funput.keyboard.ui

import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import androidx.core.view.updatePadding

/** Keeps tappable keyboard content outside the system-owned IME navigation area. */
internal class KeyboardSafeAreaController(private val host: View) {
    var current: Insets = Insets.NONE
        private set
    val horizontalInset: Int get() = current.left + current.right
    val bottomInset: Int get() = current.bottom

    fun install() {
        ViewCompat.setOnApplyWindowInsetsListener(host) { _, windowInsets ->
            update(resolveKeyboardSafeArea(windowInsets))
            windowInsets
        }
        host.doOnAttach { ViewCompat.requestApplyInsets(it) }
    }

    private fun update(insets: Insets) {
        if (insets == current) return
        current = insets
        host.updatePadding(left = insets.left, right = insets.right, bottom = insets.bottom)
        host.requestLayout()
    }
}

internal fun resolveKeyboardSafeArea(windowInsets: WindowInsetsCompat): Insets {
    val navigation = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
    val caption = windowInsets.getInsets(WindowInsetsCompat.Type.captionBar())
    return mergeKeyboardSafeAreas(navigation, caption)
}

internal fun mergeKeyboardSafeAreas(first: Insets, second: Insets): Insets = Insets.of(
    maxOf(first.left, second.left),
    maxOf(first.top, second.top),
    maxOf(first.right, second.right),
    maxOf(first.bottom, second.bottom),
)
