package app.funput.funput.ime

import app.funput.funput.ime.suggestions.dictionary.DictionaryEditKind
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.suggestion.actions.SuggestionAction
import app.funput.funput.keyboard.ui.FunputKeyboardView

internal object ImeKeyboardCallbackBinder {
    fun dispatch(editing: ImeEditingSession, action: KeyAction) = editing.dispatch(action)

    fun bind(
        view: FunputKeyboardView,
        editing: ImeEditingSession,
        switcher: SystemInputMethodSwitcher,
    ) = with(view.callbacks) {
        val handler = editing.actionHandler
        val suggestions = editing.suggestionService
        handler.typingSession.observe { hasTyped -> view.placementKeyVisible = !hasTyped }
        onKeyAction = { action ->
            editing.letterReturn.dispatch(action, view) { dispatch(editing, action) }
        }
        onSettingsRequested = ImeSettingsLauncher(view.context) {
            handler.finish()
            suggestions.finish()
        }::open
        onInputMethodSwitchRequested = {
            handler.finish()
            suggestions.finish()
            switcher.switch()
        }
        onEmojiSelected = handler::onEmojiSelected
        onPanelChanged = { panel ->
            suggestions.updatePanel(panel)
            // Swapping panels rebuilds the layout, and that resets Shift. Without
            // this the `!` and `?` on the symbol panel never capitalize: the space
            // after them raises Shift, and the trip back to ABC drops it again.
            editing.editorRuntime.updateCapitalization()
        }
        onSuggestionSelected = editing::select
        suggestionEditable = editing::canEdit
        onSuggestionAction = { selection, action -> editing.edit(selection, action.editKind) }
    }

    private val SuggestionAction.editKind: DictionaryEditKind
        get() = when (this) {
            SuggestionAction.BLOCK -> DictionaryEditKind.BLOCK
            SuggestionAction.FORGET -> DictionaryEditKind.FORGET
        }
}
