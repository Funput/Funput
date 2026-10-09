package app.funput.funput.ime.speech.integration.lifecycle

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import app.funput.funput.ime.ImeEditingSession
import app.funput.funput.ime.speech.integration.actions.SpeechHardwareBoundary
import app.funput.funput.ime.R
import app.funput.funput.ime.speech.integration.ImeSpeechRuntime
import app.funput.funput.keyboard.ui.FunputKeyboardView

/** Production gate prevents constructing any speech collaborator while the feature is unavailable. */
internal class ImeSpeechSession(service: InputMethodService, editing: ImeEditingSession) {
    private val runtime = if (service.resources.getBoolean(R.bool.speech_feature_available))
        ImeSpeechRuntime(service, editing) else null
    private val hardware = SpeechHardwareBoundary({ runtime?.back() == true }, ::invalidate)

    fun bind(view: View): View = view.also { if (it is FunputKeyboardView) runtime?.bind(it) }
    fun startInput(info: EditorInfo) { runtime?.startInput(info) }
    fun show() { runtime?.show() }
    fun setEnabled(value: Boolean) { runtime?.setEnabled(value) }
    fun selectionChanged(start: Int, end: Int) { runtime?.selectionChanged(start, end) }
    fun invalidate() { runtime?.invalidate() }
    fun hide() { runtime?.hide() }
    fun close() { runtime?.close() }

    fun keyDown(isBack: Boolean) = hardware.down(isBack)
    fun keyUp(isBack: Boolean) = hardware.up(isBack)
}
