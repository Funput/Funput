package app.funput.funput.keyboard.interaction

import app.funput.funput.keyboard.KeyboardHapticType
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.KeySwipeAction
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.model.SuggestionSelection
import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardInteractionControllerTest {
    private val actions = mutableListOf<KeyAction>()
    private val selections = mutableListOf<SuggestionSelection>()
    private val haptics = mutableListOf<KeyboardHapticType>()
    private var settingsRequestCount = 0
    private var emojiRequestCount = 0
    private var visualStateChangeCount = 0
    private var semanticStateChangeCount = 0
    private val space = KeySpec(
        id = "space",
        label = "Tiếng Việt",
        role = KeyRole.SPACE,
        horizontalSwipeAction = KeySwipeAction.TOGGLE_LANGUAGE,
    )
    private val settings = KeySpec("settings", "", KeyRole.SETTINGS, accessibilityLabel = "Settings")
    private val emoji = KeySpec("emoji", "", KeyRole.EMOJI, accessibilityLabel = "Emoji")
    private val controller = KeyboardInteractionController(
        keySpec = { id -> listOf(space, settings, emoji).firstOrNull { it.id == id } },
        suggestionSelection = { id ->
            SuggestionSelection(1, "chào").takeIf { id == "suggestion-1" }
        },
        onAction = { action -> actions += action },
        onSettingsRequested = { settingsRequestCount++ },
        onEmojiRequested = { emojiRequestCount++ },
        onSuggestionSelected = { selection -> selections += selection },
        onHapticFeedback = { type -> haptics += type },
        onVisualStateChanged = { visualStateChangeCount++ },
        onSemanticStateChanged = { semanticStateChangeCount++ },
        schedule = { _, _ -> },
        cancel = {},
        doubleTapTimeoutMillis = 300L,
        density = 1f,
    )

    @Test
    fun horizontalSwipeTogglesLanguageAndEmitsAction() {
        swipe(pointerId = 3, fromX = 100f, toX = 140f)

        assertEquals(KeyboardLanguage.ENGLISH, controller.language)
        assertEquals(KeyAction.ToggleLanguage(KeyboardLanguage.ENGLISH), actions.single())
        assertEquals(1, visualStateChangeCount)
        assertEquals(1, semanticStateChangeCount)
    }

    @Test
    fun accessibilityClickUsesTheSameSemanticDispatch() {
        controller.onAccessibilityClick("settings", eventTimeMillis = 100L)
        controller.onAccessibilityClick("space", eventTimeMillis = 101L)

        assertEquals(1, settingsRequestCount)
        assertEquals(KeyAction.Space, actions.single())
        assertEquals(listOf(KeyboardHapticType.CONTROL, KeyboardHapticType.SPACE), haptics)
    }

    @Test
    fun swipingAgainReturnsToVietnamese() {
        swipe(pointerId = 3, fromX = 100f, toX = 140f)
        swipe(pointerId = 3, fromX = 140f, toX = 100f)

        assertEquals(KeyboardLanguage.VIETNAMESE, controller.language)
        assertEquals(KeyAction.ToggleLanguage(KeyboardLanguage.VIETNAMESE), actions.last())
    }

    @Test
    fun tappingSpaceStillEmitsSpaceAction() {
        swipe(pointerId = 3, fromX = 100f, toX = 110f)

        assertEquals(KeyboardLanguage.VIETNAMESE, controller.language)
        assertEquals(KeyAction.Space, actions.single())
    }

    @Test
    fun settingsUsesDedicatedCallback() {
        controller.onPointerStarted(3, settings.id, 100f, 50f)
        controller.onKeyReleased(3, settings.id, 100f, 50f, eventTimeMillis = 100L)

        assertEquals(1, settingsRequestCount)
        assertEquals(emptyList<KeyAction>(), actions)
    }

    @Test
    fun emojiUsesDedicatedCallback() {
        controller.onPointerStarted(3, emoji.id, 100f, 50f)
        controller.onKeyReleased(3, emoji.id, 100f, 50f, eventTimeMillis = 100L)

        assertEquals(1, emojiRequestCount)
        assertEquals(emptyList<KeyAction>(), actions)
    }

    @Test
    fun suggestionUsesDedicatedCallback() {
        controller.onPointerStarted(3, "suggestion-1", 100f, 50f)
        controller.onKeyReleased(3, "suggestion-1", 100f, 50f, eventTimeMillis = 100L)

        assertEquals(listOf(SuggestionSelection(1, "chào")), selections)
        assertEquals(emptyList<KeyAction>(), actions)
    }

    @Test
    fun accessibilitySuggestionUsesValidatedCallbackOnce() {
        controller.onAccessibilitySuggestion("suggestion-1")

        assertEquals(listOf(SuggestionSelection(1, "chào")), selections)
        assertEquals(listOf(KeyboardHapticType.CONTROL), haptics)
    }

    @Test
    fun pointerDownEmitsHapticForEachTargetType() {
        controller.onPointerStarted(3, space.id, 100f, 50f)
        controller.onPointerStarted(4, settings.id, 100f, 50f)
        controller.onPointerStarted(5, emoji.id, 100f, 50f)
        controller.onPointerStarted(6, "suggestion-1", 100f, 50f)
        controller.onPointerStarted(7, null, 100f, 50f)

        assertEquals(
            listOf(
                KeyboardHapticType.SPACE,
                KeyboardHapticType.CONTROL,
                KeyboardHapticType.CONTROL,
                KeyboardHapticType.CONTROL,
            ),
            haptics,
        )
    }

    private fun swipe(pointerId: Int, fromX: Float, toX: Float) {
        controller.onPointerStarted(pointerId, space.id, fromX, 50f)
        controller.onKeyReleased(pointerId, space.id, toX, 50f, eventTimeMillis = 100L)
    }
}
