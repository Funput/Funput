package app.funput.funput.ime.editing

import android.view.inputmethod.InputConnection
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.ime.editing.backspace.ImeBackspaceHandler
import app.funput.funput.ime.editing.gestures.ImeGestureEditor
import app.funput.funput.ime.editing.typing.ImeTypingHandler

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
    private val gestures = ImeGestureEditor(
        composition, editor, connection, ::backspace,
    ) { if (suggestionsAllowed) suggestions.reset() }
    var smartGesturesEnabled: Boolean by gestures::enabled
    private val typing = ImeTypingHandler(
        composition = composition,
        editor = editor,
        connection = connection,
        enterCommand = enterCommand,
        suggestions = suggestions,
        usesComposition = { usesComposition },
        suggestionsAllowed = { suggestionsAllowed },
        finish = ::finish,
    )
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
        gestures.reset()
    }

    fun onKeyAction(action: KeyAction) {
        if (gestures.consume(action)) return
        when (action) {
            is KeyAction.Input -> typing.input(action.text)
            KeyAction.Space -> typing.input(" ")
            KeyAction.Backspace -> backspace()
            KeyAction.Enter -> typing.enter()
            is KeyAction.ToggleLanguage -> toggleLanguage(action.language)
            is KeyAction.Shift,
            is KeyAction.MoveCursor,
            KeyAction.DeleteWord,
            KeyAction.Symbols,
            KeyAction.MoreSymbols,
            KeyAction.Letters,
            KeyAction.SwitchInputMethod,
            -> Unit
        }
    }

    fun onEmojiSelected(emoji: String) = typing.commitExternal(emoji)

    fun onClipboardSelected(text: String) = typing.commitExternal(text)

    fun finish() {
        composition.finish(connection())
        suggestions.reset()
        gestures.reset()
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

    private fun backspace() {
        val direct = !usesComposition
        backspaceHandler.perform()
        if (direct && suggestionsAllowed) suggestions.backspaceDirect()
    }

    private fun toggleLanguage(value: KeyboardLanguage) {
        if (!compositionAllowed) return
        finish()
        language = value
        composition.setEnabled(usesComposition)
        suggestions.reset()
    }

    private val usesComposition: Boolean
        get() = compositionAllowed && language == KeyboardLanguage.VIETNAMESE
}
