package app.funput.funput.keyboard.ui

import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.SuggestionSelection

/** Host callbacks emitted by the complete Funput keyboard UI. */
class FunputKeyboardCallbacks {
    var onKeyAction: ((KeyAction) -> Unit)? = null
    var onInputMethodSwitchRequested: (() -> Unit)? = null
    var onSettingsRequested: (() -> Unit)? = null
    var onEmojiPanelOpened: (() -> Unit)? = null
    var onPanelChanged: ((KeyboardPanel) -> Unit)? = null
    var onEmojiSelected: ((String) -> Unit)? = null
    var onSuggestionSelected: ((SuggestionSelection) -> Unit)? = null

    internal fun dispatch(action: KeyAction) {
        onKeyAction?.invoke(action)
    }

    internal fun dispatchInputMethodSwitchRequest() {
        onInputMethodSwitchRequested?.invoke()
    }

    internal fun dispatchSettingsRequest() {
        onSettingsRequested?.invoke()
    }

    internal fun dispatchEmojiPanelOpened() {
        onEmojiPanelOpened?.invoke()
    }

    internal fun dispatchPanelChanged(panel: KeyboardPanel) {
        onPanelChanged?.invoke(panel)
    }

    internal fun dispatchEmoji(emoji: String) {
        onEmojiSelected?.invoke(emoji)
    }

    internal fun dispatchSuggestion(selection: SuggestionSelection) {
        onSuggestionSelected?.invoke(selection)
    }
}
