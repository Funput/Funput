package app.funput.funput.keyboard.ui.panel

import android.view.View
import app.funput.funput.keyboard.KeyboardSurfaceView
import app.funput.funput.keyboard.model.KeyboardLayoutMode
import app.funput.funput.keyboard.ui.EmojiPanelView
import app.funput.funput.keyboard.ui.KeyboardPanel
import app.funput.funput.keyboard.ui.KeyboardPanelController
import app.funput.funput.keyboard.ui.clipboard.ClipboardPanelView
import app.funput.funput.theme.KeyboardTheme

/** Owns panel presentation while the public keyboard view remains the host API. */
internal class KeyboardPanelCoordinator(
    private val keyboardSurface: KeyboardSurfaceView,
    private val createEmojiPanel: () -> EmojiPanelView,
    private val createClipboardPanel: () -> ClipboardPanelView,
    private val attachPanel: (View) -> Unit,
    private val onPanelChanged: (KeyboardPanel) -> Unit,
    private val syncSuggestions: () -> Unit,
) {
    private val state = KeyboardPanelController()
    private var emojiPanel: EmojiPanelView? = null
    private var clipboardPanel: ClipboardPanelView? = null

    val activePanel: KeyboardPanel get() = state.activePanel
    val loadedEmojiPanel: EmojiPanelView? get() = emojiPanel
    val loadedClipboardPanel: ClipboardPanelView? get() = clipboardPanel

    fun showEmoji() {
        if (!state.show(KeyboardPanel.EMOJI)) return
        val panel = emojiPanel ?: createEmojiPanel().also {
            emojiPanel = it
            attachPanel(it)
        }
        hideClipboardPanel()
        keyboardSurface.visibility = View.GONE
        panel.visibility = View.VISIBLE
        onPanelChanged(KeyboardPanel.EMOJI)
    }

    fun showClipboard() {
        if (!state.show(KeyboardPanel.CLIPBOARD)) return
        val panel = clipboardPanel ?: createClipboardPanel().also {
            clipboardPanel = it
            attachPanel(it)
        }
        hideEmojiPanel()
        keyboardSurface.visibility = View.GONE
        panel.visibility = View.VISIBLE
        syncSuggestions()
        onPanelChanged(KeyboardPanel.CLIPBOARD)
    }

    fun showSymbols(mode: KeyboardLayoutMode) {
        state.showSymbols(mode)
        hideEmojiPanel()
        hideClipboardPanel()
        keyboardSurface.layoutMode = mode
        keyboardSurface.visibility = View.VISIBLE
        syncSuggestions()
        onPanelChanged(KeyboardPanel.SYMBOLS)
    }

    fun showLetters() {
        if (!state.show(KeyboardPanel.LETTERS)) return
        hideEmojiPanel()
        hideClipboardPanel()
        keyboardSurface.layoutMode = KeyboardLayoutMode.LETTERS
        keyboardSurface.visibility = View.VISIBLE
        syncSuggestions()
        onPanelChanged(KeyboardPanel.LETTERS)
    }

    fun updateTheme(theme: KeyboardTheme) {
        emojiPanel?.updateTheme(theme)
        clipboardPanel?.updateTheme(theme)
    }

    private fun hideEmojiPanel() {
        emojiPanel?.visibility = View.GONE
    }

    private fun hideClipboardPanel() {
        clipboardPanel?.let { it.visibility = View.GONE; it.reset() }
    }
}
