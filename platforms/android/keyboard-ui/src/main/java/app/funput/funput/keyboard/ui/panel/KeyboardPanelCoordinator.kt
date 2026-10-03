package app.funput.funput.keyboard.ui.panel

import android.view.View
import app.funput.funput.keyboard.ui.speech.SpeechPanelView
import app.funput.funput.keyboard.ui.speech.SpeechPanelAction
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
    private val createSpeechPanel: () -> SpeechPanelView,
    private val onSpeechAction: (SpeechPanelAction) -> Unit,
    private val clearSpeech: () -> Unit,
    private val createClipboardPanel: () -> ClipboardPanelView,
    private val attachPanel: (View) -> Unit,
    private val onPanelChanged: (KeyboardPanel) -> Unit,
    private val syncSuggestions: () -> Unit,
) {
    private val state = KeyboardPanelController()
    private var speechPanel: SpeechPanelView? = null
    private var emojiPanel: EmojiPanelView? = null
    private var clipboardPanel: ClipboardPanelView? = null

    val activePanel: KeyboardPanel get() = state.activePanel
    val loadedEmojiPanel: EmojiPanelView? get() = emojiPanel
    val loadedClipboardPanel: ClipboardPanelView? get() = clipboardPanel

    fun showSpeech() {
        if (!state.show(KeyboardPanel.SPEECH)) return
        val panel = speechPanel ?: createSpeechPanel().also {
            speechPanel = it
            attachPanel(it)
        }
        hideEmojiPanel()
        hideClipboardPanel()
        keyboardSurface.visibility = View.GONE
        panel.visibility = View.VISIBLE
        syncSuggestions()
        onPanelChanged(KeyboardPanel.SPEECH)
    }

    fun showEmoji() {
        if (!state.show(KeyboardPanel.EMOJI)) return
        val panel = emojiPanel ?: createEmojiPanel().also {
            emojiPanel = it
            attachPanel(it)
        }
        hideClipboardPanel()
        val speechClosed = hideSpeechPanel()
        keyboardSurface.visibility = View.GONE
        panel.visibility = View.VISIBLE
        notifyChanged(KeyboardPanel.EMOJI, speechClosed)
    }

    fun showClipboard() {
        if (!state.show(KeyboardPanel.CLIPBOARD)) return
        val panel = clipboardPanel ?: createClipboardPanel().also {
            clipboardPanel = it
            attachPanel(it)
        }
        hideEmojiPanel()
        val speechClosed = hideSpeechPanel()
        keyboardSurface.visibility = View.GONE
        panel.visibility = View.VISIBLE
        syncSuggestions()
        notifyChanged(KeyboardPanel.CLIPBOARD, speechClosed)
    }

    fun showSymbols(mode: KeyboardLayoutMode) {
        state.showSymbols(mode)
        hideEmojiPanel()
        hideClipboardPanel()
        val speechClosed = hideSpeechPanel()
        keyboardSurface.layoutMode = mode
        keyboardSurface.visibility = View.VISIBLE
        syncSuggestions()
        notifyChanged(KeyboardPanel.SYMBOLS, speechClosed)
    }

    fun showLetters() {
        if (!state.show(KeyboardPanel.LETTERS)) return
        hideEmojiPanel()
        hideClipboardPanel()
        val speechClosed = hideSpeechPanel()
        keyboardSurface.layoutMode = KeyboardLayoutMode.LETTERS
        keyboardSurface.visibility = View.VISIBLE
        syncSuggestions()
        notifyChanged(KeyboardPanel.LETTERS, speechClosed)
    }

    fun updateTheme(theme: KeyboardTheme) {
        emojiPanel?.updateTheme(theme)
        clipboardPanel?.updateTheme(theme)
        speechPanel?.updateTheme(theme)
    }

    private fun hideSpeechPanel(): Boolean {
        val panel = speechPanel ?: return false
        if (panel.visibility != View.VISIBLE) return false
        panel.visibility = View.GONE
        clearSpeech()
        return true
    }

    private fun notifyChanged(panel: KeyboardPanel, speechClosed: Boolean) {
        if (speechClosed) onSpeechAction(SpeechPanelAction.CANCEL)
        if (activePanel == panel) onPanelChanged(panel)
    }

    private fun hideEmojiPanel() {
        emojiPanel?.visibility = View.GONE
    }

    private fun hideClipboardPanel() {
        clipboardPanel?.let { it.visibility = View.GONE; it.reset() }
    }
}
