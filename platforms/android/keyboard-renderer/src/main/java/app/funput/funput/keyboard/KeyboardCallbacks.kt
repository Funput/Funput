package app.funput.funput.keyboard

import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.SuggestionSelection

/** Host callbacks emitted by the keyboard surface. */
class KeyboardCallbacks {
    var onKeyAction: ((KeyAction) -> Unit)? = null
    var onEmojiRequested: (() -> Unit)? = null
    var onClipboardPanelRequested: (() -> Unit)? = null
    var onClipboardPasteRequested: (() -> Unit)? = null
    var onSuggestionSelected: ((SuggestionSelection) -> Unit)? = null

    internal fun dispatch(action: KeyAction) {
        onKeyAction?.invoke(action)
    }

    internal fun dispatchEmojiRequest() {
        onEmojiRequested?.invoke()
    }

    internal fun dispatchClipboardPasteRequest() {
        onClipboardPasteRequested?.invoke()
    }

    internal fun dispatchClipboardPanelRequest() {
        onClipboardPanelRequested?.invoke()
    }

    internal fun dispatchSuggestion(selection: SuggestionSelection) {
        onSuggestionSelected?.invoke(selection)
    }
}
