package app.funput.funput.keyboard.popover.interaction

import app.funput.funput.keyboard.interaction.KeyboardInteractionController
import app.funput.funput.keyboard.layout.KeyBounds
import app.funput.funput.keyboard.layout.commaKey
import app.funput.funput.keyboard.layout.periodKey
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeySpec
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

internal class CommaPopoverSubject {
    val comma = commaKey("comma")
    val period = periodKey("period")
    val source = KeyBounds(50f, 200f, 90f, 240f)
    val pending = mutableListOf<Runnable>()
    val input = mutableListOf<KeyAction>()
    val events = mutableListOf<String>()
    val controller = KeyboardInteractionController(
        keySpec = { id -> listOf(comma, period).firstOrNull { it.id == id } },
        suggestionSelection = { null },
        onAction = { input += it },
        onEmojiRequested = {},
        onPlacementEditorRequested = { opened("placement") },
        onSettingsRequested = { opened("settings") },
        onPointersCancelled = { events += "cancelPointers" },
        onSuggestionSelected = {},
        onHapticFeedback = {},
        onVisualStateChanged = {},
        onSemanticStateChanged = {},
        schedule = { task, _ -> pending += task },
        cancel = { pending.remove(it) },
        keyBounds = { source },
        surfaceBounds = { KeyBounds(0f, -400f, 320f, 300f) },
        touchSlop = 8f,
        onPointerCaptured = {},
        doubleTapTimeoutMillis = 300L,
        density = 1f,
    )

    fun down(pointer: Int = 1, key: KeySpec = comma) {
        controller.onPointerStarted(pointer, key.id, source.centerX, source.centerY)
        controller.onPointerKeyChanged(pointer, key.id)
    }

    fun hold() {
        pending.removeAt(0).run()
    }

    fun release(pointer: Int = 1, x: Float = source.centerX, y: Float = source.centerY, keyId: String? = comma.id) {
        controller.onKeyReleased(pointer, keyId, x, y, 500L)
    }

    private fun opened(name: String) {
        assertNull(controller.alternatePreview)
        assertTrue(pending.isEmpty())
        assertTrue(events.last() == "cancelPointers")
        events += name
    }
}
