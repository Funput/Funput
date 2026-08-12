package app.funput.funput.keyboard.interaction

import app.funput.funput.keyboard.KeyboardHapticType
import app.funput.funput.keyboard.layout.KeyBounds
import org.junit.Assert.assertEquals
import org.junit.Test

class ClipboardInteractionControllerTest {
    @Test
    fun `touch and accessibility dispatch clipboard exactly once with control feedback`() {
        var requests = 0
        val haptics = mutableListOf<KeyboardHapticType>()
        val controller = controller({ requests++ }, haptics::add)

        controller.onPointerStarted(1, ClipboardTargetId, 40f, 20f)
        controller.onKeyReleased(1, ClipboardTargetId, 40f, 20f, 10L)
        controller.onAccessibilityClick(ClipboardTargetId, 11L)

        assertEquals(2, requests)
        assertEquals(listOf(KeyboardHapticType.CONTROL, KeyboardHapticType.CONTROL), haptics)
    }

    private fun controller(
        onClipboard: () -> Unit,
        onHaptic: (KeyboardHapticType) -> Unit,
    ) = KeyboardInteractionController(
        keySpec = { null },
        suggestionSelection = { null },
        onAction = {},
        onSettingsRequested = {},
        onEmojiRequested = {},
        onClipboardRequested = onClipboard,
        onSuggestionSelected = {},
        onHapticFeedback = onHaptic,
        onVisualStateChanged = {},
        onSemanticStateChanged = {},
        schedule = { _, _ -> },
        cancel = {},
        keyBounds = { KeyBounds(0f, 0f, 100f, 40f) },
        surfaceBounds = { KeyBounds(0f, 0f, 320f, 260f) },
        touchSlop = 8f,
        onPointerCaptured = {},
        doubleTapTimeoutMillis = 300L,
        density = 1f,
    )
}
