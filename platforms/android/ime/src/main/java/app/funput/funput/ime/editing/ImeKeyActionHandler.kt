package app.funput.funput.ime.editing

import android.view.inputmethod.InputConnection
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.ime.editing.backspace.ImeBackspaceHandler

/** Routes semantic keyboard actions through composition or direct editor commands. */
internal class ImeKeyActionHandler(
    private val composition: AndroidCompositionSession,
    private val editor: InputConnectionEditor,
    private val connection: () -> InputConnection?,
    private val enterCommand: () -> ImeEditCommand,
) {
    private var compositionAllowed = true
    private var suggestionsAllowed = true
    private val suggestions = ImeSuggestionSession(composition, connection)
    private val backspaceHandler = ImeBackspaceHandler(
        composition = composition,
        editor = editor,
        connection = connection,
        usesComposition = { usesComposition },
        onCompositionChanged = suggestions::updateComposition,
        onCompositionCleared = { if (usesComposition) suggestions.reset() },
    )

    var language: KeyboardLanguage = KeyboardLanguage.VIETNAMESE
        private set

    /**
     * The input method is not applied here: `ImeSettingsController` owns engine
     * configuration and has already pushed it (see its `applyEngineConfiguration`).
     */
    fun start(
        allowComposition: Boolean = true,
        allowSuggestions: Boolean = true,
        renderMode: CompositionRenderMode = CompositionRenderMode.COMPOSING,
    ) {
        compositionAllowed = allowComposition
        suggestionsAllowed = allowSuggestions
        composition.reset()
        composition.setRenderMode(renderMode)
        composition.setEnabled(usesComposition)
        suggestions.reset()
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

    fun onClipboardSelected(text: String) = commitExternalText(text)

    fun finish() {
        composition.finish(connection())
        suggestions.reset()
    }

    fun onSelectionChanged(newStart: Int, newEnd: Int, composingEnd: Int) {
        if (!usesComposition) {
            if (suggestionsAllowed) suggestions.reconcileDirectSelection()
            return
        }
        if (
            composition.isComposing &&
            !composition.ownsSelection(connection(), newStart, newEnd, composingEnd)
        ) {
            finish()
        }
    }

    fun takeSuggestionUpdate(): AuthoredSuggestionUpdate =
        if (suggestionsAllowed) suggestions.takeUpdate() else AuthoredSuggestionUpdate.Empty

    fun acceptSuggestion(candidate: String, prefix: String): Boolean =
        suggestionsAllowed && suggestions.accept(candidate, prefix, usesComposition)

    private fun inputText(text: String) {
        if (usesComposition) {
            val current = connection()
            if (current == null) return suggestions.reset()
            composition.input(current, text)
            suggestions.updateComposition()
        } else {
            if (execute(ImeEditCommand.CommitText(text)) && suggestionsAllowed) {
                suggestions.inputDirect(text)
            } else {
                suggestions.reset()
            }
        }
    }

    private fun backspace() {
        val direct = !usesComposition
        backspaceHandler.perform()
        if (direct && suggestionsAllowed) suggestions.backspaceDirect()
    }

    private fun enter() {
        val command = enterCommand()
        if (usesComposition && command == NewLineCommand) {
            composition.input(connection(), "\n")
        } else {
            if (usesComposition) finish()
            else if (suggestionsAllowed) suggestions.inputDirect("\n")
            execute(command)
        }
    }

    private fun toggleLanguage(value: KeyboardLanguage) {
        if (!compositionAllowed) return
        finish()
        language = value
        composition.setEnabled(usesComposition)
        suggestions.reset()
    }

    private fun commitExternalText(text: String) {
        finish()
        execute(ImeEditCommand.CommitText(text))
    }

    private fun execute(command: ImeEditCommand) = editor.execute(connection(), command)

    private val usesComposition: Boolean
        get() = compositionAllowed && language == KeyboardLanguage.VIETNAMESE

    private companion object {
        val NewLineCommand = ImeEditCommand.CommitText("\n")
    }
}
