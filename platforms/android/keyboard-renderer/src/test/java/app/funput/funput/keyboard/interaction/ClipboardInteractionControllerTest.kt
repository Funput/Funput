package app.funput.funput.keyboard.interaction

import app.funput.funput.keyboard.utility.KeyboardUtilityAction
import app.funput.funput.keyboard.utility.KeyboardUtilityDispatcher
import app.funput.funput.keyboard.KeyboardHapticType
import app.funput.funput.keyboard.layout.KeyBounds
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import org.junit.Assert.assertEquals
import org.junit.Test

class ClipboardInteractionControllerTest {
    @Test
    fun `tap pastes when offer is present and opens panel when empty`() {
        val actions = mutableListOf<String>()
        val withOffer = controller(actions::add, hasOffer = true)
        withOffer.onKeyReleased(1, "clipboard", 40f, 20f, 10L)
        val withoutOffer = controller(actions::add, hasOffer = false)
        withoutOffer.onKeyReleased(2, "clipboard", 40f, 20f, 11L)
        assertEquals(listOf("paste", "panel"), actions)
    }

    @Test
    fun `accessibility click pastes exactly once with control feedback`() {
        var requests = 0
        val haptics = mutableListOf<KeyboardHapticType>()
        val controller = controller({ if (it == "paste") requests++ }, hasOffer = true, onHaptic = haptics::add)
        controller.emitClick("clipboard", 11L)
        assertEquals(1, requests)
        assertEquals(listOf(KeyboardHapticType.CONTROL), haptics)
    }

    @Test
    fun `holding clipboard opens panel and skips the release paste`() {
        val actions = mutableListOf<String>()
        val pending = mutableListOf<Runnable>()
        val controller = controller(
            actions::add,
            hasOffer = true,
            schedule = { task, _ -> pending += task },
            cancel = { pending.remove(it) },
        )
        controller.onPointerStarted(1, "clipboard", 40f, 20f)
        pending.toList().forEach(Runnable::run)
        assertEquals(listOf("panel"), actions)
        controller.onKeyReleased(1, "clipboard", 40f, 20f, 10L)
        assertEquals(listOf("panel"), actions)
    }

    @Test
    fun `accessibility history action opens panel with control feedback`() {
        val actions = mutableListOf<String>()
        val haptics = mutableListOf<KeyboardHapticType>()
        val controller = controller(actions::add, hasOffer = true, onHaptic = haptics::add)
        controller.emitClipboardPanel()
        assertEquals(listOf("panel"), actions)
        assertEquals(listOf(KeyboardHapticType.CONTROL), haptics)
    }

    private fun controller(
        onAction: (String) -> Unit,
        hasOffer: Boolean,
        onHaptic: (KeyboardHapticType) -> Unit = {},
        schedule: (Runnable, Long) -> Unit = { _, _ -> },
        cancel: (Runnable) -> Unit = {},
    ) = KeyboardInteractionController(
        keySpec = { id -> KeySpec(id, "", KeyRole.CLIPBOARD, accessibilityLabel = "Dán") },
        suggestionSelection = { null },
        onAction = {},
        utilities = KeyboardUtilityDispatcher {
            when (it) {
                KeyboardUtilityAction.CLIPBOARD_PASTE -> onAction("paste")
                KeyboardUtilityAction.CLIPBOARD_PANEL -> onAction("panel")
                else -> Unit
            }
        },
        onSuggestionSelected = {},
        onHapticFeedback = onHaptic,
        onVisualStateChanged = {},
        onSemanticStateChanged = {},
        schedule = schedule,
        cancel = cancel,
        keyBounds = { KeyBounds(0f, 0f, 100f, 40f) },
        surfaceBounds = { KeyBounds(0f, 0f, 320f, 260f) },
        touchSlop = 8f,
        onPointerCaptured = {},
        doubleTapTimeoutMillis = 300L,
        density = 1f,
        hasPasteOffer = { hasOffer },
    )
}
