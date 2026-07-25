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
    private val suggestionTracker = AuthoredTokenTracker()

    var language: KeyboardLanguage = KeyboardLanguage.VIETNAMESE
        private set

    /**
     * The input method is not applied here: `ImeSettingsController` owns engine
     * configuration and has already pushed it (see its `applyEngineConfiguration`).
     */
    fun start(allowComposition: Boolean = true) {
        compositionAllowed = allowComposition
        composition.reset()
        composition.setEnabled(usesComposition)
        suggestionTracker.reset()
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
            KeyAction.SwitchInputMethod,
            -> Unit
        }
    }

    fun onEmojiSelected(emoji: String) = commitExternalText(emoji)

    fun finish() {
        composition.finish(connection())
        suggestionTracker.reset()
    }

    fun onSelectionChanged(newStart: Int, newEnd: Int, composingEnd: Int) {
        if (composition.isComposing && (newStart != composingEnd || newEnd != composingEnd)) finish()
    }

    fun takeSuggestionUpdate(): AuthoredSuggestionUpdate = suggestionTracker.consume()

    fun acceptSuggestion(candidate: String, prefix: String): Boolean {
        if (!usesComposition || composition.composingText != prefix) return false
        val current = connection() ?: return false
        if (!current.getSelectedText(0).isNullOrEmpty()) return false
        val suffix = current.getTextBeforeCursor(prefix.length, 0)?.toString() ?: return false
        if (!suffix.endsWith(prefix)) return false
        current.beginBatchEdit()
        val accepted = try {
            composition.acceptSuggestion(current, prefix, candidate)
        } finally {
            current.endBatchEdit()
        }
        if (accepted) suggestionTracker.accepted(candidate)
        return accepted
    }

    private fun inputText(text: String) {
        if (usesComposition) {
            val current = connection()
            if (current == null) return suggestionTracker.reset()
            composition.input(current, text)
            updateSuggestionTracker()
        } else {
            execute(ImeEditCommand.CommitText(text))
            suggestionTracker.reset()
        }
    }

    private fun backspace() {
        if (!composition.backspace(connection())) {
            // Delete the previous grapheme and (when Vietnamese) re-open the word
            // behind the caret as one batch. Two top-level edits would each fire
            // onUpdateSelection; the first arrives with candidatesEnd=-1 after
            // reopen has already set isComposing, and onSelectionChanged finishes
            // the composition we just restored — which is exactly the real-device
            // failure unit tests miss (they never invoke onSelectionChanged).
            val current = connection()
            if (current != null && usesComposition) {
                current.beginBatchEdit()
                try {
                    execute(ImeEditCommand.DeleteBackward)
                    composition.reopenPreviousWord(current)
                } finally {
                    current.endBatchEdit()
                }
                if (composition.isComposing) updateSuggestionTracker() else suggestionTracker.reset()
            } else {
                execute(ImeEditCommand.DeleteBackward)
                suggestionTracker.reset()
            }
        } else {
            updateSuggestionTracker()
        }
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
        suggestionTracker.reset()
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

    private fun updateSuggestionTracker() {
        suggestionTracker.update(composition.composingText, composition.takeCompletedToken())
    }

    private companion object {
        val NewLineCommand = ImeEditCommand.CommitText("\n")
    }
}
