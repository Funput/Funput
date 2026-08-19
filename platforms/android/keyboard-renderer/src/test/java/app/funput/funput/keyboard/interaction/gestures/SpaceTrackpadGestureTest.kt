package app.funput.funput.keyboard.interaction.gestures

import app.funput.funput.keyboard.KeyboardHapticType
import app.funput.funput.keyboard.interaction.KeyboardInteractionController
import app.funput.funput.keyboard.layout.KeyBounds
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.KeySwipeAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpaceTrackpadGestureTest {
    private val space = KeySpec(
        id = "space",
        label = "Tiếng Việt",
        role = KeyRole.SPACE,
        horizontalSwipeAction = KeySwipeAction.TOGGLE_LANGUAGE,
    )

    @Test
    fun aQuickSwipeStillTogglesTheLanguage() {
        val subject = subject()
        subject.begin(space)
        subject.move(space, 180f)
        subject.release(space, 180f)

        assertTrue(subject.actions.any { it is KeyAction.ToggleLanguage })
        assertFalse(subject.actions.any { it is KeyAction.MoveCursor })
    }

    @Test
    fun holdingThenDraggingMovesTheCaretInstead() {
        val subject = subject()
        subject.begin(space)
        subject.scheduler.fire(after = 350L)
        subject.move(space, 145f)

        assertEquals(listOf(KeyAction.MoveCursor(2)), subject.actions)
        assertTrue(subject.captured.contains(1))
        assertEquals(listOf(KeyboardHapticType.SPACE, KeyboardHapticType.CONTROL, KeyboardHapticType.DELETE_REPEAT), subject.haptics)
    }

    @Test
    fun theCaretFollowsTheFingerBackAndForth() {
        val subject = subject()
        subject.begin(space)
        subject.scheduler.fire(after = 350L)
        subject.move(space, 150f)
        subject.move(space, 130f)

        assertEquals(listOf(KeyAction.MoveCursor(3), KeyAction.MoveCursor(-2)), subject.actions)
    }

    @Test
    fun withSmartGesturesOffTheSpacebarSwipesAsBefore() {
        val subject = subject()
        subject.controller.areSmartGesturesEnabled = false
        subject.begin(space)
        subject.move(space, 145f)
        subject.release(space, 145f)

        assertTrue(subject.actions.any { it is KeyAction.ToggleLanguage })
        assertFalse(subject.actions.any { it is KeyAction.MoveCursor })
    }

    @Test
    fun releasingATrackpadDragCommitsNoSpace() {
        val subject = subject()
        subject.begin(space)
        subject.scheduler.fire(after = 350L)
        subject.move(space, 150f)
        subject.release(space, 150f)

        assertFalse(subject.actions.any { it == KeyAction.Space })
        assertTrue(subject.actions.any { it is KeyAction.MoveCursor })
    }
}

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
        controller.onPointerStarted(1, key.id, x, 220f)
        controller.onPointerKeyChanged(1, key.id)
    }

    fun move(key: KeySpec, x: Float) = controller.onPointerMoved(1, key.id, x, 220f)

    fun release(key: KeySpec, x: Float) =
        controller.onKeyReleased(1, key.id, x, 220f, eventTimeMillis = 100L)
}

internal class MultiScheduler {
    private val pending = mutableListOf<Pair<Long, Runnable>>()
    fun schedule(task: Runnable, delayMillis: Long) { pending += delayMillis to task }
    fun cancel(task: Runnable) { pending.removeAll { it.second === task } }
    fun fire(after: Long) {
        val index = pending.indexOfFirst { it.first == after }
        require(index >= 0) { "No task scheduled for $after" }
        pending.removeAt(index).second.run()
    }
}

private fun subject() = GestureControllerSubject()
