package app.funput.funput.keyboard.ui.host

import app.funput.funput.keyboard.KeyboardSurfaceView
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.ui.FunputKeyboardCallbacks

internal fun bindKeyboardToolbar(
    surface: KeyboardSurfaceView,
    callbacks: FunputKeyboardCallbacks,
    openEmoji: () -> Unit,
    openClipboard: () -> Unit,
    openPlacement: () -> Unit,
) {
    surface.callbacks.onEmojiRequested = openEmoji
    surface.callbacks.onClipboardPasteRequested = callbacks::dispatchClipboardPasteRequest
    surface.callbacks.onClipboardPanelRequested = openClipboard
    surface.callbacks.onPlacementEditorRequested = openPlacement
    surface.callbacks.onSettingsRequested = callbacks::dispatchSettingsRequest
    surface.callbacks.onMicrophoneRequested = {
        if (surface.microphone.visible && surface.editorMode in listOf(
                KeyboardEditorMode.TEXT, KeyboardEditorMode.SEARCH, KeyboardEditorMode.URL)) {
            callbacks.dispatchSpeechRequest()
        }
    }
}
