package app.funput.funput.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import app.funput.funput.ime.editing.EditorInfoActionResolver
import app.funput.funput.ime.editing.ImeEditCommand
import app.funput.funput.ime.editing.ImeEditorAction
import app.funput.funput.ime.editing.InputConnectionEditor
import app.funput.funput.ime.editing.toImeEditCommand
import app.funput.funput.keyboard.ui.FunputKeyboardView

/** System entry point that owns the Funput keyboard view inside the IME window. */
class FunputInputMethodService : InputMethodService() {
    private val editor = InputConnectionEditor()
    private var editorAction = ImeEditorAction.NewLine
    private var keyboardView: FunputKeyboardView? = null

    override fun onCreateInputView(): View = FunputKeyboardView(this).also { view ->
        keyboardView = view
        view.enterAction = editorAction.presentation
        bindCallbacks(view)
    }

    override fun onStartInputView(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInputView(attribute, restarting)
        editorAction = EditorInfoActionResolver.resolve(attribute)
        keyboardView?.enterAction = editorAction.presentation
    }

    override fun onDestroy() {
        keyboardView = null
        super.onDestroy()
    }

    private fun bindCallbacks(view: FunputKeyboardView) = with(view.callbacks) {
        onKeyAction = { action ->
            action.toImeEditCommand(editorAction.command)?.let(::execute)
        }
        onEmojiSelected = { emoji ->
            execute(ImeEditCommand.CommitText(emoji))
        }
        onSuggestionSelected = { suggestion ->
            execute(ImeEditCommand.CommitText(suggestion.text))
        }
    }

    private fun execute(command: ImeEditCommand) {
        editor.execute(currentInputConnection, command)
    }
}
