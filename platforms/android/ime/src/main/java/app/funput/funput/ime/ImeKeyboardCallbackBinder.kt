package app.funput.funput.ime

import app.funput.funput.ime.editing.ImeEditorRuntime
import app.funput.funput.ime.editing.ImeKeyActionHandler
import app.funput.funput.ime.suggestions.PersonalSuggestionService
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.ui.FunputKeyboardView

internal object ImeKeyboardCallbackBinder {
    fun dispatch(
        handler: ImeKeyActionHandler,
        suggestions: PersonalSuggestionService,
        action: KeyAction,
    ) {
        handler.onKeyAction(action)
        suggestions.consume(handler.takeSuggestionUpdate())
    }

    fun bind(
        view: FunputKeyboardView,
        handler: ImeKeyActionHandler,
        runtime: ImeEditorRuntime,
        suggestions: PersonalSuggestionService,
        switcher: SystemInputMethodSwitcher,
    ) = with(view.callbacks) {
        onKeyAction = { action -> dispatch(handler, suggestions, action) }
        onInputMethodSwitchRequested = {
            handler.finish()
            suggestions.finish()
            switcher.switch()
        }
        onEmojiSelected = handler::onEmojiSelected
        onPanelChanged = suggestions::updatePanel
        onSuggestionSelected = { selection ->
            if (!runtime.selectCompletion(selection, handler::finish)) {
                suggestions.select(selection, handler)
            }
        }
    }
}
