package app.funput.funput.ime

import app.funput.funput.ime.editing.ImeEditorRuntime
import app.funput.funput.ime.editing.ImeKeyActionHandler
import app.funput.funput.ime.suggestions.PersonalSuggestionService
import app.funput.funput.keyboard.ui.FunputKeyboardView

internal object ImeKeyboardCallbackBinder {
    fun bind(
        view: FunputKeyboardView,
        handler: ImeKeyActionHandler,
        runtime: ImeEditorRuntime,
        suggestions: PersonalSuggestionService,
        switcher: SystemInputMethodSwitcher,
    ) = with(view.callbacks) {
        onKeyAction = { action ->
            handler.onKeyAction(action)
            suggestions.updateLanguage(handler.language)
            suggestions.consume(handler.takeSuggestionUpdate())
        }
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
