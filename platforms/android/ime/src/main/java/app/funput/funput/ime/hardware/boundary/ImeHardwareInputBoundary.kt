package app.funput.funput.ime.hardware.boundary

import android.view.KeyEvent
import app.funput.funput.ime.hardware.HardwareKeyboard
import app.funput.funput.ime.speech.integration.lifecycle.ImeSpeechSession

/** The framework boundary sees navigation/modifier strokes that the soft-key dispatcher never sees. */
internal class ImeHardwareInputBoundary(
    private val hardware: () -> HardwareKeyboard,
    private val speech: ImeSpeechSession,
) {
    fun down(code: Int, event: KeyEvent, fallback: () -> Boolean): Boolean {
        if (speech.keyDown(code == KeyEvent.KEYCODE_BACK)) return true
        return hardware().onKeyDown(event) || fallback()
    }

    fun up(code: Int, event: KeyEvent, fallback: () -> Boolean): Boolean {
        if (speech.keyUp(code == KeyEvent.KEYCODE_BACK)) return true
        return hardware().onKeyUp(event) || fallback()
    }

    fun multiple(fallback: () -> Boolean): Boolean {
        speech.invalidate()
        return fallback()
    }
}
