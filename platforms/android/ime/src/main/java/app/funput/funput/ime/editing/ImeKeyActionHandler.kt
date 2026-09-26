package app.funput.funput.ime.editing

import android.view.inputmethod.InputConnection
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.ime.editing.backspace.ImeBackspaceHandler
import app.funput.funput.ime.editing.gestures.ImeGestureEditor
import app.funput.funput.ime.editing.typing.ImeTypingHandler
import app.funput.funput.ime.editing.typing.ImeTypingSession
import app.funput.funput.ime.shortcuts.ImeShortcutSession
import app.funput.funput.ime.shortcuts.ImeShortcutCoordinator
import app.funput.funput.shortcuts.model.ShortcutLibrary

/** Routes semantic keyboard actions through composition or direct editor commands. */
internal class ImeKeyActionHandler(
    private val composition: AndroidCompositionSession,
    private val editor: InputConnectionEditor,
    private val connection: () -> InputConnection?,
    private val enterCommand: () -> ImeEditCommand,
    private val shortcuts: ImeShortcutSession = ImeShortcutSession(composition.engine),
) {
    private var compositionAllowed = true
    private var suggestionsAllowed = true
    private var shortcutsAllowed = true
    private val suggestions = ImeSuggestionSession(composition, connection)
    private val gestures = ImeGestureEditor(
        composition, editor, connection, ::backspace,
    ) { if (suggestionsAllowed) suggestions.reset() }
    var smartGesturesEnabled: Boolean by gestures::enabled
    val typingSession = ImeTypingSession()
    private val typing = ImeTypingHandler(
        composition = composition,
        editor = editor,
        connection = connection,
        enterCommand = enterCommand,
        suggestions = suggestions,
        usesVietnameseComposition = { usesVietnameseComposition },
        usesEnglishShortcuts = { usesEnglishShortcuts },
        suggestionsAllowed = { suggestionsAllowed },
        shortcuts = shortcuts,
        onTyping = typingSession::recordInput,
        inputTracked = { text -> shortcutCoordinator.track(text, usesVietnameseComposition) },
        finish = ::finish,
    )
    private val backspaceHandler = ImeBackspaceHandler(
        composition = composition,
        editor = editor,
        connection = connection,
        usesComposition = { usesVietnameseComposition },
        onCompositionChanged = suggestions::updateComposition,
        onCompositionCleared = { if (usesVietnameseComposition) suggestions.reset() },
    )
    private val shortcutCoordinator = ImeShortcutCoordinator(composition, shortcuts, connection)

    var language: KeyboardLanguage = KeyboardLanguage.VIETNAMESE
        private set
    fun start(
        allowComposition: Boolean = true,
        allowSuggestions: Boolean = true,
        allowShortcuts: Boolean = true,
        renderMode: CompositionRenderMode = CompositionRenderMode.COMPOSING,
    ) {
        compositionAllowed = allowComposition
        suggestionsAllowed = allowSuggestions
        shortcutsAllowed = allowShortcuts
        composition.reset()
        composition.setRenderMode(renderMode)
        composition.setEnabled(usesVietnameseComposition)
        shortcutCoordinator.finish()
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
        shortcutCoordinator.finish()
        suggestions.reset()
        gestures.reset()
    }
    fun beginShortcutActivation() = shortcutCoordinator.beginActivation()

    fun receiveShortcuts(library: ShortcutLibrary) {
        val snapshot = if (shortcutsAllowed) library else ShortcutLibrary(isEnabled = false)
        shortcutCoordinator.receive(snapshot)
        composition.setEnabled(usesVietnameseComposition)
    }

    fun onSelectionChanged(newStart: Int, newEnd: Int, composingEnd: Int) {
        gestures.onSelectionChanged(newStart)
        if (!usesVietnameseComposition) {
            if (suggestionsAllowed) suggestions.reconcileDirectSelection()
            shortcutCoordinator.reconcile()
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
        suggestionsAllowed && suggestions.accept(candidate, prefix, usesVietnameseComposition)

    private fun backspace() {
        val direct = !usesVietnameseComposition
        backspaceHandler.perform()
        if (direct && suggestionsAllowed) suggestions.backspaceDirect()
        shortcutCoordinator.backspace(usesEnglishShortcuts)
    }

    private fun toggleLanguage(value: KeyboardLanguage) {
        if (!compositionAllowed) return
        finish()
        language = value
        composition.setEnabled(usesVietnameseComposition)
        suggestions.reset()
    }

    private val usesVietnameseComposition: Boolean
        get() = compositionAllowed && language == KeyboardLanguage.VIETNAMESE
    private val usesEnglishShortcuts: Boolean
        get() = shortcutsAllowed && compositionAllowed && language == KeyboardLanguage.ENGLISH &&
            shortcutCoordinator.runsInEnglish
}
