package app.funput.funput.keyboard.interaction

import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.KeySwipeAction
import app.funput.funput.keyboard.model.KeyboardLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardInteractionControllerTest {
    private val actions = mutableListOf<KeyAction>()
    private var visualStateChangeCount = 0
    private val space = KeySpec(
        id = "space",
        label = "Tiếng Việt",
        role = KeyRole.SPACE,
        horizontalSwipeAction = KeySwipeAction.TOGGLE_LANGUAGE,
    )
    private val controller = KeyboardInteractionController(
        keySpec = { id -> space.takeIf { it.id == id } },
        onAction = { action -> actions += action },
        onVisualStateChanged = { visualStateChangeCount++ },
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

    private fun swipe(pointerId: Int, fromX: Float, toX: Float) {
        controller.onPointerStarted(pointerId, space.id, fromX, 50f)
        controller.onKeyReleased(pointerId, space.id, toX, 50f, eventTimeMillis = 100L)
    }
}
