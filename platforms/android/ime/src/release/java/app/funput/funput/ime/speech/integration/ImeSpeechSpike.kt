package app.funput.funput.ime.speech.integration

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import app.funput.funput.ime.ImeEditingSession

/** Release builds have no spike entry point, controls, or speech client lifecycle. */
@Suppress("UNUSED_PARAMETER")
internal class ImeSpeechSpike(service: InputMethodService, session: ImeEditingSession) {
    fun wrap(view: View): View = view
    fun startInput(info: EditorInfo) = Unit
    fun setEnabled(value: Boolean) = Unit
    fun show() = Unit
    fun selectionChanged(start: Int, end: Int) = Unit
    fun invalidate() = Unit
    fun hide() = Unit
    fun close() = Unit
}
