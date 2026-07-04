package app.funput.funput.ime.editing

import android.view.inputmethod.InputConnection
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.model.SuggestionSelection

/** Routes semantic keyboard actions through composition or direct editor commands. */
internal class ImeKeyActionHandler(
    private val composition: AndroidCompositionSession,
    private val editor: InputConnectionEditor,
    private val connection: () -> InputConnection?,
    private val enterCommand: () -> ImeEditCommand,
) {
    private var compositionAllowed = true

    var language: KeyboardLanguage = KeyboardLanguage.VIETNAMESE
        private set

    fun start(inputMethod: KeyboardInputMethod, allowComposition: Boolean = true) {
        compositionAllowed = allowComposition
        composition.reset()
        composition.setInputMethod(inputMethod)
        composition.setEnabled(usesComposition)
    }

    fun onKeyAction(action: KeyAction) {
        when (action) {
            is KeyAction.Input -> inputText(action.text)
            KeyAction.Space -> inputText(" ")
            KeyAction.Backspace -> backspace()
            KeyAction.Enter -> enter()
            is KeyAction.ToggleLanguage -> toggleLanguage(action.language)
            is KeyAction.Shift,
            KeyAction.Symbols,
            KeyAction.MoreSymbols,
            KeyAction.Letters,
            -> Unit
        }
    }

    fun onEmojiSelected(emoji: String) = commitExternalText(emoji)

    fun onSuggestionSelected(selection: SuggestionSelection) = commitExternalText(selection.text)

    fun finish() = composition.finish(connection())

    fun onSelectionChanged(newStart: Int, newEnd: Int, composingEnd: Int) {
        if (composition.isComposing && (newStart != composingEnd || newEnd != composingEnd)) finish()
    }

    private fun inputText(text: String) {
        if (usesComposition) {
            composition.input(connection(), text)
        } else {
            execute(ImeEditCommand.CommitText(text))
        }
    }

    private fun backspace() {
        if (!composition.backspace(connection())) execute(ImeEditCommand.DeleteBackward)
    }

    private fun enter() {
        val command = enterCommand()
        if (usesComposition && command == NewLineCommand) {
            composition.input(connection(), "\n")
        } else {
            finish()
            execute(command)
        }
    }

    private fun toggleLanguage(value: KeyboardLanguage) {
        if (!compositionAllowed) return
        finish()
        language = value
        composition.setEnabled(usesComposition)
    }

    private fun commitExternalText(text: String) {
        finish()
        execute(ImeEditCommand.CommitText(text))
    }

    private fun execute(command: ImeEditCommand) {
        editor.execute(connection(), command)
    }

    private val usesComposition: Boolean
        get() = compositionAllowed && language == KeyboardLanguage.VIETNAMESE

    private companion object {
        val NewLineCommand = ImeEditCommand.CommitText("\n")
    }
}
