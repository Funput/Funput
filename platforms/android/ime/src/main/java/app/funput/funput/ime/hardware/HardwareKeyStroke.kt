package app.funput.funput.ime.hardware

import android.view.KeyEvent

/**
 * A hardware key press that unit tests can construct without calling KeyEvent getters.
 *
 * Android's JVM stubs throw on those getters; the IME converts a real event at runtime.
 */
internal data class HardwareKeyStroke(
    val keyCode: Int,
    val codePoint: Int = 0,
    val repeatCount: Int = 0,
    val isCtrl: Boolean = false,
    val isAlt: Boolean = false,
    val isMeta: Boolean = false,
    val isShift: Boolean = false,
    val isCanceled: Boolean = false,
)

internal fun KeyEvent.toHardwareKeyStroke() = HardwareKeyStroke(
    keyCode = keyCode,
    codePoint = unicodeChar,
    repeatCount = repeatCount,
    isCtrl = isCtrlPressed,
    isAlt = isAltPressed,
    isMeta = isMetaPressed,
    isShift = isShiftPressed,
    isCanceled = isCanceled,
)
