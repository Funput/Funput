package app.funput.funput.keyboard.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.ui.emoji.panel.EmojiBrowserPanelView
import app.funput.funput.keyboard.ui.kaomoji.panel.KaomojiPanelView
import app.funput.funput.theme.KeyboardTheme

internal class EmojiPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {
    var onEmojiSelected: (String) -> Unit = {}
    var onLettersRequested: () -> Unit = {}
    var onBackspaceRequested: (KeyAction) -> Unit = {}
    var hapticsEnabled: Boolean
        get() = emoji.hapticsEnabled
        set(value) {
            emoji.hapticsEnabled = value
            kaomoji.hapticsEnabled = value
        }
    var soundsEnabled: Boolean
        get() = emoji.soundsEnabled
        set(value) {
            emoji.soundsEnabled = value
            kaomoji.soundsEnabled = value
        }
    private val emoji = EmojiBrowserPanelView(context)
    private val kaomoji = KaomojiPanelView(context)

    init {
        KeyboardComposeLifecycle.install(this)
        addView(emoji, matchParent())
        addView(kaomoji, matchParent())
        wireActions()
        showEmojiPanel()
    }

    fun updateTheme(theme: KeyboardTheme) {
        emoji.updateTheme(theme)
        kaomoji.updateTheme(theme)
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (changedView === this && visibility != VISIBLE) {
            emoji.reset()
            kaomoji.reset()
            showEmojiPanel()
        }
    }

    private fun wireActions() {
        emoji.onEmojiSelected = { onEmojiSelected(it) }
        emoji.onLettersRequested = { onLettersRequested() }
        emoji.onBackspaceRequested = { onBackspaceRequested(it) }
        emoji.onKaomojiRequested = ::showKaomojiPanel
        kaomoji.onKaomojiSelected = { onEmojiSelected(it) }
        kaomoji.onLettersRequested = { onLettersRequested() }
        kaomoji.onBackspaceRequested = { onBackspaceRequested(it) }
        kaomoji.onEmojiRequested = ::showEmojiPanel
    }

    internal fun showEmojiPanel() {
        kaomoji.visibility = GONE
        emoji.visibility = VISIBLE
    }

    internal fun showKaomojiPanel() {
        emoji.visibility = GONE
        kaomoji.visibility = VISIBLE
    }

    internal val isShowingKaomoji: Boolean get() = kaomoji.isVisible

    private fun matchParent() = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
}
