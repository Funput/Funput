package app.funput.funput.keyboard.utility

import app.funput.funput.keyboard.KeyboardCallbacks
import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardUtilityCallbacksTest {
    @Test fun commonUtilityContractPreservesEveryHostCallbackWithoutTyping() {
        val requests = mutableListOf<String>()
        val callbacks = KeyboardCallbacks().apply {
            onMicrophoneRequested = { requests += "microphone" }
            onEmojiRequested = { requests += "emoji" }
            onPlacementEditorRequested = { requests += "placement" }
            onSettingsRequested = { requests += "settings" }
            onClipboardPanelRequested = { requests += "panel" }
            onClipboardPasteRequested = { requests += "paste" }
            onKeyAction = { error("A utility must not emit a typing action") }
            onSuggestionSelected = { error("A utility must not emit a candidate") }
        }
        val utilities: KeyboardUtilityDispatcher = callbacks
        KeyboardUtilityAction.entries.forEach(utilities::dispatch)
        assertEquals(listOf("emoji", "microphone", "placement", "settings", "panel", "paste"), requests)
    }

    @Test fun unattachedCallbacksAreInert() {
        val callbacks: KeyboardUtilityDispatcher = KeyboardCallbacks()
        KeyboardUtilityAction.entries.forEach(callbacks::dispatch)
    }
}
