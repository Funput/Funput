package app.funput.funput.ime.speech.integration.actions

import app.funput.funput.keyboard.ui.FunputKeyboardView
import app.funput.funput.keyboard.ui.KeyboardPanel
import app.funput.funput.keyboard.ui.speech.SpeechPanelAction

/** Guard callbacks rather than touches, including accessibility and local panel editor actions. */
internal object ImeSpeechInputActions {
    fun bind(
        view: FunputKeyboardView,
        current: () -> Boolean,
        presenting: () -> Boolean,
        invalidate: () -> Unit,
        refresh: () -> Unit,
        start: () -> Unit,
        speechAction: (SpeechPanelAction) -> Unit,
    ) = with(view.callbacks) {
        fun before() { invalidate() }
        onKeyAction = guard(onKeyAction, current, ::before, refresh)
        onInputMethodSwitchRequested = guard(onInputMethodSwitchRequested, current, ::before, refresh)
        onSettingsRequested = guard(onSettingsRequested, current, ::before, refresh)
        onEmojiSelected = guard(onEmojiSelected, current, ::before, refresh)
        onClipboardPasteRequested = guard(onClipboardPasteRequested, current, ::before, refresh)
        onClipboardEntrySelected = guard(onClipboardEntrySelected, current, ::before, refresh)
        onSuggestionSelected = guard(onSuggestionSelected, current, ::before, refresh)
        onPlacementChanged = guard(onPlacementChanged, current, ::before, refresh)
        onPlacementEditorRequested = { if (current()) invalidate() }
        onPanelChanging = { panel ->
            if (current() && !presenting() && panel != KeyboardPanel.SPEECH) invalidate()
        }
        onSpeechRequested = { if (current()) start() }
        onSpeechAction = { action -> if (current() && !presenting()) speechAction(action) }
    }

    private fun guard(action: (() -> Unit)?, current: () -> Boolean, before: () -> Unit,
        after: () -> Unit): () -> Unit = {
        if (current()) {
            before()
            try { action?.invoke() } finally { after() }
        }
    }

    private fun <T> guard(action: ((T) -> Unit)?, current: () -> Boolean, before: () -> Unit,
        after: () -> Unit): (T) -> Unit = { value ->
        if (current()) {
            before()
            try { action?.invoke(value) } finally { after() }
        }
    }
}
