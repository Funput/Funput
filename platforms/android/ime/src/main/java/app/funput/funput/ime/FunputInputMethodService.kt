package app.funput.funput.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import app.funput.funput.keyboard.ui.FunputKeyboardView

/** System entry point that owns the Funput keyboard view inside the IME window. */
class FunputInputMethodService : InputMethodService() {
    override fun onCreateInputView(): View = FunputKeyboardView(this)
}
