package app.funput.funput.ime.speech.integration.actions

import app.funput.funput.keyboard.ui.FunputKeyboardView

/** Accessibility can dispatch actions without a touch event, so guard the actual callbacks. */
internal object SpeechSpikeInputActions {
    fun bind(view: FunputKeyboardView, invalidate: () -> Unit) = with(view.callbacks) {
        onKeyAction = guard(onKeyAction, invalidate)
        onInputMethodSwitchRequested = guard(onInputMethodSwitchRequested, invalidate)
        onSettingsRequested = guard(onSettingsRequested, invalidate)
        onPanelChanged = guard(onPanelChanged, invalidate)
        onEmojiSelected = guard(onEmojiSelected, invalidate)
        onClipboardPasteRequested = guard(onClipboardPasteRequested, invalidate)
        onClipboardEntrySelected = guard(onClipboardEntrySelected, invalidate)
        onSuggestionSelected = guard(onSuggestionSelected, invalidate)
        onPlacementChanged = guard(onPlacementChanged, invalidate)
    }

    private fun guard(action: (() -> Unit)?, before: () -> Unit): () -> Unit = {
        before()
        action?.invoke()
    }

    private fun <T> guard(action: ((T) -> Unit)?, before: () -> Unit): (T) -> Unit = { value ->
        before()
        action?.invoke(value)
    }
}
