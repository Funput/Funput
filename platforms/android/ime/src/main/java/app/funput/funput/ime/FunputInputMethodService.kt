package app.funput.funput.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import app.funput.funput.ime.editing.ImeEditCommand
import app.funput.funput.ime.editing.InputConnectionEditor
import app.funput.funput.ime.editing.toImeEditCommand
import app.funput.funput.keyboard.ui.FunputKeyboardView

/** System entry point that owns the Funput keyboard view inside the IME window. */
class FunputInputMethodService : InputMethodService() {
    private val editor = InputConnectionEditor()

    override fun onCreateInputView(): View = FunputKeyboardView(this).apply {
        callbacks.onKeyAction = { action ->
            action.toImeEditCommand()?.let(::execute)
        }
        callbacks.onEmojiSelected = { emoji ->
            execute(ImeEditCommand.CommitText(emoji))
        }
        callbacks.onSuggestionSelected = { suggestion ->
            execute(ImeEditCommand.CommitText(suggestion.text))
        }
    }

    private fun execute(command: ImeEditCommand) {
        editor.execute(currentInputConnection, command)
    }
}
