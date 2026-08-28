package app.funput.funput.keyboard.interaction.gestures

import app.funput.funput.keyboard.KeyboardHapticType
import app.funput.funput.keyboard.interaction.KeyboardInteractionController
import app.funput.funput.keyboard.layout.KeyBounds
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeySpec

internal class GestureControllerSubject {
    val actions = mutableListOf<KeyAction>()
    val haptics = mutableListOf<KeyboardHapticType>()
    val captured = mutableListOf<Int>()
    val scheduler = MultiScheduler()
    val controller = KeyboardInteractionController(
        keySpec = { id -> keys[id] },
        suggestionSelection = { null },
        onAction = { actions += it },
        onEmojiRequested = {},
        onSuggestionSelected = {},
        onHapticFeedback = { haptics += it },
        onVisualStateChanged = {},
        onSemanticStateChanged = {},
        schedule = scheduler::schedule,
        cancel = scheduler::cancel,
        keyBounds = { KeyBounds(102f, 198f, 138f, 242f) },
        surfaceBounds = { KeyBounds(0f, 0f, 390f, 304f) },
        touchSlop = 8f,
        onPointerCaptured = { captured += it },
        doubleTapTimeoutMillis = 300L,
        density = 1f,
    )
    private val keys = mutableMapOf<String, KeySpec>()

    fun begin(key: KeySpec, x: Float = 120f) {
        keys[key.id] = key
        controller.onPointerStarted(1, key.id, x, RestY)
        controller.onPointerKeyChanged(1, key.id)
    }

    fun move(key: KeySpec, x: Float, y: Float = RestY) = controller.onPointerMoved(1, key.id, x, y)

    fun release(key: KeySpec, x: Float) =
        controller.onKeyReleased(1, key.id, x, RestY, eventTimeMillis = 100L)

    companion object {
        /** Vertical centre of the fake spacebar; every gesture starts from here. */
        const val RestY = 220f
    }
}

internal class MultiScheduler {
    private val pending = mutableListOf<Pair<Long, Runnable>>()
    fun schedule(task: Runnable, delayMillis: Long) { pending += delayMillis to task }
    fun cancel(task: Runnable) { pending.removeAll { it.second === task } }
    fun hasPending(after: Long) = pending.any { it.first == after }
    fun fire(after: Long) {
        val index = pending.indexOfFirst { it.first == after }
        require(index >= 0) { "No task scheduled for $after" }
        pending.removeAt(index).second.run()
    }
}
