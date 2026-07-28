package app.funput.funput.keyboard.popover.interaction

import app.funput.funput.keyboard.interaction.KeyboardActionDispatcher
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.keyboard.popover.model.KeyAlternate
import org.junit.Assert.assertEquals
import org.junit.Test

class AlternateDispatchTest {
    private val actions = mutableListOf<KeyAction>()
    private val key = KeySpec("character-a", "a", KeyRole.CHARACTER)
    private val dispatcher = KeyboardActionDispatcher(
        keySpec = { key },
        onAction = { actions += it },
        onShiftStateChanged = {},
        doubleTapTimeoutMillis = 300L,
    )

    @Test
    fun `alternate follows shift and consumes one shot state`() {
        dispatcher.setShiftState(ShiftState.ON)
        dispatcher.dispatchAlternate(key, KeyAlternate("á"))

        assertEquals(KeyAction.Input(key.id, "Á"), actions.single())
        assertEquals(ShiftState.OFF, dispatcher.shiftState)
    }

    @Test
    fun `caps lock remains active after alternate`() {
        dispatcher.setShiftState(ShiftState.CAPS_LOCK)
        dispatcher.dispatchAlternate(key, KeyAlternate("ư"))

        assertEquals(KeyAction.Input(key.id, "Ư"), actions.single())
        assertEquals(ShiftState.CAPS_LOCK, dispatcher.shiftState)
    }
}
