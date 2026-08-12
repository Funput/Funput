package app.funput.funput.keyboard.ui

import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.SuggestionSelection
import app.funput.funput.keyboard.ui.clipboard.KeyboardClipboardEntry

/** Host callbacks emitted by the complete Funput keyboard UI. */
class FunputKeyboardCallbacks {
    var onKeyAction: ((KeyAction) -> Unit)? = null
    var onInputMethodSwitchRequested: (() -> Unit)? = null
    var onSettingsRequested: (() -> Unit)? = null
    var onEmojiPanelOpened: (() -> Unit)? = null
    var onPanelChanged: ((KeyboardPanel) -> Unit)? = null
    var onEmojiSelected: ((String) -> Unit)? = null
    var onClipboardPasteRequested: (() -> Unit)? = null
    var onClipboardPanelOpened: (() -> Unit)? = null
    var onClipboardEntrySelected: ((KeyboardClipboardEntry) -> Unit)? = null
    var onClipboardPinToggled: ((KeyboardClipboardEntry) -> Unit)? = null
    var onClipboardEntryRemoved: ((KeyboardClipboardEntry) -> Unit)? = null
    var onClipboardClearRequested: (() -> Unit)? = null
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

    internal fun dispatchClipboardPasteRequest() {
        onClipboardPasteRequested?.invoke()
    }

    internal fun dispatchClipboardPanelOpened() { onClipboardPanelOpened?.invoke() }
    internal fun dispatchClipboardEntry(entry: KeyboardClipboardEntry) { onClipboardEntrySelected?.invoke(entry) }
    internal fun dispatchClipboardPin(entry: KeyboardClipboardEntry) { onClipboardPinToggled?.invoke(entry) }
    internal fun dispatchClipboardRemove(entry: KeyboardClipboardEntry) { onClipboardEntryRemoved?.invoke(entry) }
    internal fun dispatchClipboardClear() { onClipboardClearRequested?.invoke() }

    internal fun dispatchSuggestion(selection: SuggestionSelection) {
        onSuggestionSelected?.invoke(selection)
    }
}
