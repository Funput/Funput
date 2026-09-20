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
    var extraBottomInset: Int = 0
        set(value) {
            if (field == value) return
            field = value
            applyPadding()
        }

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
        applyPadding()
    }

    private fun applyPadding() {
        host.updatePadding(
            left = current.left,
            right = current.right,
            bottom = current.bottom + extraBottomInset,
        )
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
