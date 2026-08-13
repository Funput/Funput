package app.funput.funput.keyboard.ui

import app.funput.funput.keyboard.KeyboardSurfaceView
import app.funput.funput.keyboard.ui.clipboard.ClipboardPanelView

internal class KeyboardFeedbackController(
    private val keyboardSurface: KeyboardSurfaceView,
    private val emojiPanel: () -> EmojiPanelView?,
    private val clipboardPanel: () -> ClipboardPanelView?,
) {
    var hapticsEnabled: Boolean
        get() = keyboardSurface.isHapticFeedbackEnabled
        set(value) {
            keyboardSurface.isHapticFeedbackEnabled = value
            emojiPanel()?.hapticsEnabled = value
            clipboardPanel()?.hapticsEnabled = value
        }

    var soundsEnabled: Boolean
        get() = keyboardSurface.isSoundEffectsEnabled
        set(value) {
            keyboardSurface.isSoundEffectsEnabled = value
            emojiPanel()?.soundsEnabled = value
            clipboardPanel()?.soundsEnabled = value
        }
}
