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
        invalidate: (Boolean) -> Unit,
        refresh: () -> Unit,
        start: () -> Unit,
        speechAction: (SpeechPanelAction) -> Unit,
    ) = with(view.callbacks) {
        fun beforeEdit() { invalidate(true) }
        fun beforeNavigation() { invalidate(false) }
        onKeyAction = guard(onKeyAction, current, { invalidate(SpeechInputEditPolicy.expectsSelection(it)) }, refresh)
        onInputMethodSwitchRequested = guard(onInputMethodSwitchRequested, current, ::beforeNavigation, refresh)
        onSettingsRequested = guard(onSettingsRequested, current, ::beforeNavigation, refresh)
        onEmojiSelected = guard(onEmojiSelected, current, { beforeEdit() }, refresh)
        onClipboardPasteRequested = guard(onClipboardPasteRequested, current, ::beforeEdit, refresh)
        onClipboardEntrySelected = guard(onClipboardEntrySelected, current, { beforeEdit() }, refresh)
        onSuggestionSelected = guard(onSuggestionSelected, current, { beforeEdit() }, refresh)
        onPlacementChanged = guard(onPlacementChanged, current, { beforeNavigation() }, refresh)
        onPlacementEditorRequested = { if (current()) invalidate(false) }
        onPanelChanging = { panel ->
            if (current() && !presenting() && panel != KeyboardPanel.SPEECH) invalidate(false)
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

    private fun <T> guard(action: ((T) -> Unit)?, current: () -> Boolean, before: (T) -> Unit,
        after: () -> Unit): (T) -> Unit = { value ->
        if (current()) {
            before(value)
            try { action?.invoke(value) } finally { after() }
        }
    }
}
