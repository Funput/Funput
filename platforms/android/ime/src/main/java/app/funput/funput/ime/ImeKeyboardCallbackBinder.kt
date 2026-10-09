package app.funput.funput.ime

import app.funput.funput.ime.editing.ImeEditorRuntime
import app.funput.funput.ime.editing.ImeKeyActionHandler
import app.funput.funput.ime.editing.layout.LetterPageReturn
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
        letterReturn: LetterPageReturn,
    ) = with(view.callbacks) {
        handler.typingSession.observe { hasTyped -> view.placementKeyVisible = !hasTyped }
        onKeyAction = { action ->
            letterReturn.dispatch(action, view) { dispatch(handler, suggestions, action) }
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
            runtime.updateCapitalization()
        }
        onSuggestionSelected = { selection ->
            if (!runtime.selectCompletion(selection, handler::finish)) {
                suggestions.select(selection, handler)
            }
        }
    }
}
