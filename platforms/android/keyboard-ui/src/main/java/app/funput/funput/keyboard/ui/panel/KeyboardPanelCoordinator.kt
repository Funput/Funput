package app.funput.funput.keyboard.ui.panel

import android.view.View
import app.funput.funput.keyboard.KeyboardSurfaceView
import app.funput.funput.keyboard.model.KeyboardLayoutMode
import app.funput.funput.keyboard.ui.EmojiPanelView
import app.funput.funput.keyboard.ui.KeyboardPanel
import app.funput.funput.keyboard.ui.KeyboardPanelController
import app.funput.funput.theme.KeyboardTheme

/** Owns panel presentation while the public keyboard view remains the host API. */
internal class KeyboardPanelCoordinator(
    private val keyboardSurface: KeyboardSurfaceView,
    private val createEmojiPanel: () -> EmojiPanelView,
    private val attachPanel: (EmojiPanelView) -> Unit,
    private val onPanelChanged: (KeyboardPanel) -> Unit,
    private val syncSuggestions: () -> Unit,
) {
    private val state = KeyboardPanelController()
    private var emojiPanel: EmojiPanelView? = null

    val activePanel: KeyboardPanel get() = state.activePanel
    val loadedEmojiPanel: EmojiPanelView? get() = emojiPanel

    fun showEmoji() {
        if (!state.show(KeyboardPanel.EMOJI)) return
        val panel = emojiPanel ?: createEmojiPanel().also {
            emojiPanel = it
            attachPanel(it)
        }
        keyboardSurface.visibility = View.GONE
        panel.visibility = View.VISIBLE
        onPanelChanged(KeyboardPanel.EMOJI)
    }

    fun showSymbols(mode: KeyboardLayoutMode) {
        state.showSymbols(mode)
        hideEmojiPanel()
        keyboardSurface.layoutMode = mode
        keyboardSurface.visibility = View.VISIBLE
        syncSuggestions()
        onPanelChanged(KeyboardPanel.SYMBOLS)
    }

    fun showLetters() {
        if (!state.show(KeyboardPanel.LETTERS)) return
        hideEmojiPanel()
        keyboardSurface.layoutMode = KeyboardLayoutMode.LETTERS
        keyboardSurface.visibility = View.VISIBLE
        syncSuggestions()
        onPanelChanged(KeyboardPanel.LETTERS)
    }

    fun updateTheme(theme: KeyboardTheme) {
        emojiPanel?.updateTheme(theme)
    }

    private fun hideEmojiPanel() {
        emojiPanel?.visibility = View.GONE
    }
}
