package app.funput.funput.keyboard.ui.panel

import android.content.Context
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.ui.EmojiPanelView
import app.funput.funput.keyboard.ui.FunputKeyboardCallbacks
import app.funput.funput.keyboard.ui.clipboard.ClipboardPanelView
import app.funput.funput.theme.KeyboardTheme

internal class FunputPanelFactory(
    private val context: Context,
    private val callbacks: FunputKeyboardCallbacks,
    private val theme: () -> KeyboardTheme,
    private val haptics: () -> Boolean,
    private val sounds: () -> Boolean,
    private val clipboardState: KeyboardClipboardPanelState,
    private val showLetters: () -> Unit,
) {
    fun createEmoji() = EmojiPanelView(context).apply {
        updateTheme(theme())
        hapticsEnabled = haptics()
        soundsEnabled = sounds()
        onEmojiSelected = callbacks::dispatchEmoji
        onBackspaceRequested = callbacks::dispatch
        onLettersRequested = showLetters
    }

    fun createClipboard() = ClipboardPanelView(context).apply {
        updateTheme(theme())
        hapticsEnabled = haptics()
        soundsEnabled = sounds()
        clipboardState.attach(this)
        onSelect = callbacks::dispatchClipboardEntry
        onTogglePin = callbacks::dispatchClipboardPin
        onRemove = callbacks::dispatchClipboardRemove
        onClearAll = callbacks::dispatchClipboardClear
        onDelete = { callbacks.dispatch(KeyAction.Backspace) }
        onReturn = showLetters
    }
}
