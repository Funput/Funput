package app.funput.funput.ime

import android.inputmethodservice.InputMethodService

/**
 * Keeps the app anchored to the real keyboard when the input view grows a
 * transparent overlay so an alternate palette can sit above a top-row key.
 */
internal object ImeOverlayInsets {
    fun apply(outInsets: InputMethodService.Insets, overlayPadTop: Int) {
        val next = adjustment(outInsets.contentTopInsets, outInsets.visibleTopInsets, overlayPadTop)
        outInsets.contentTopInsets = next.contentTopInsets
        outInsets.visibleTopInsets = next.visibleTopInsets
        if (next.touchableFrame) {
            outInsets.touchableInsets = InputMethodService.Insets.TOUCHABLE_INSETS_FRAME
        }
    }

    fun adjustment(
        contentTopInsets: Int,
        visibleTopInsets: Int,
        overlayPadTop: Int,
    ): Adjustment {
        if (overlayPadTop <= 0) return Adjustment(contentTopInsets, visibleTopInsets, false)
        return Adjustment(
            contentTopInsets = contentTopInsets - overlayPadTop,
            visibleTopInsets = visibleTopInsets - overlayPadTop,
            touchableFrame = true,
        )
    }

    data class Adjustment(
        val contentTopInsets: Int,
        val visibleTopInsets: Int,
        val touchableFrame: Boolean,
    )
}
