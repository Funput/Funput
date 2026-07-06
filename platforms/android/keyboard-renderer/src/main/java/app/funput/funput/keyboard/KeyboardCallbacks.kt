package app.funput.funput.keyboard

import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.SuggestionSelection

/** Host callbacks emitted by the keyboard surface. */
class KeyboardCallbacks {
    var onKeyAction: ((KeyAction) -> Unit)? = null
    var onSettingsRequested: (() -> Unit)? = null
    var onEmojiRequested: (() -> Unit)? = null
    var onSuggestionSelected: ((SuggestionSelection) -> Unit)? = null

    internal fun dispatch(action: KeyAction) {
        onKeyAction?.invoke(action)
    }

    internal fun dispatchSettingsRequest() {
        onSettingsRequested?.invoke()
    }

    internal fun dispatchEmojiRequest() {
        onEmojiRequested?.invoke()
    }

    internal fun dispatchSuggestion(selection: SuggestionSelection) {
        onSuggestionSelected?.invoke(selection)
    }
}
