package app.funput.funput.keyboard

import app.funput.funput.keyboard.utility.KeyboardUtilityAction
import app.funput.funput.keyboard.utility.KeyboardUtilityDispatcher
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.SuggestionSelection

/** Host callbacks emitted by the keyboard surface. */
class KeyboardCallbacks : KeyboardUtilityDispatcher {
    var onKeyAction: ((KeyAction) -> Unit)? = null
    var onMicrophoneRequested: (() -> Unit)? = null
    var onEmojiRequested: (() -> Unit)? = null
    var onPlacementEditorRequested: (() -> Unit)? = null
    var onSettingsRequested: (() -> Unit)? = null
    var onClipboardPanelRequested: (() -> Unit)? = null
    var onClipboardPasteRequested: (() -> Unit)? = null
    var onSuggestionSelected: ((SuggestionSelection) -> Unit)? = null

    internal fun dispatch(action: KeyAction) {
        onKeyAction?.invoke(action)
    }

    override fun dispatch(action: KeyboardUtilityAction) {
        when (action) {
            KeyboardUtilityAction.MICROPHONE -> onMicrophoneRequested?.invoke()
            KeyboardUtilityAction.EMOJI -> onEmojiRequested?.invoke()
            KeyboardUtilityAction.PLACEMENT -> onPlacementEditorRequested?.invoke()
            KeyboardUtilityAction.SETTINGS -> onSettingsRequested?.invoke()
            KeyboardUtilityAction.CLIPBOARD_PANEL -> onClipboardPanelRequested?.invoke()
            KeyboardUtilityAction.CLIPBOARD_PASTE -> onClipboardPasteRequested?.invoke()
        }
    }

    internal fun dispatchSuggestion(selection: SuggestionSelection) {
        onSuggestionSelected?.invoke(selection)
    }
}
