package app.funput.funput.keyboard.interaction

import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.ShiftState
import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardActionDispatcherTest {
    private val actions = mutableListOf<KeyAction>()
    private var shiftStateChangeCount = 0
    private val keys = listOf(
        key("shift", KeyRole.SHIFT),
        key("a", KeyRole.CHARACTER, label = "a", shiftedLabel = "A"),
        key("backspace", KeyRole.BACKSPACE),
        key("vni-1", KeyRole.VNI_MODIFIER, label = "1"),
    ).associateBy(KeySpec::id)
    private val dispatcher = KeyboardActionDispatcher(
        keySpec = keys::get,
        onAction = { action -> actions += action },
        onShiftStateChanged = { shiftStateChangeCount++ },
        doubleTapTimeoutMillis = DoubleTapTimeoutMillis,
    )

    @Test
    fun shiftIsConsumedAfterOneCharacter() {
        dispatcher.dispatch("shift", eventTimeMillis = 100L)
        dispatcher.dispatch("a", eventTimeMillis = 500L)

        assertEquals(listOf(KeyAction.Shift(ShiftState.ON), input("A")), actions)
        assertEquals(ShiftState.OFF, dispatcher.shiftState)
        assertEquals(2, shiftStateChangeCount)
    }

    @Test
    fun doubleTapEnablesPersistentCapsLock() {
        dispatcher.dispatch("shift", eventTimeMillis = 100L)
        dispatcher.dispatch("shift", eventTimeMillis = 250L)
        dispatcher.dispatch("a", eventTimeMillis = 500L)

        assertEquals(KeyAction.Shift(ShiftState.CAPS_LOCK), actions[1])
        assertEquals(input("A"), actions.last())
        assertEquals(ShiftState.CAPS_LOCK, dispatcher.shiftState)
    }

    @Test
    fun tappingCapsLockShiftTurnsItOff() {
        dispatcher.dispatch("shift", eventTimeMillis = 100L)
        dispatcher.dispatch("shift", eventTimeMillis = 250L)
        dispatcher.dispatch("shift", eventTimeMillis = 500L)

        assertEquals(KeyAction.Shift(ShiftState.OFF), actions.last())
        assertEquals(ShiftState.OFF, dispatcher.shiftState)
    }

    @Test
    fun slowSecondShiftTapTurnsOneShotOff() {
        dispatcher.dispatch("shift", eventTimeMillis = 100L)
        dispatcher.dispatch("shift", eventTimeMillis = 500L)

        assertEquals(KeyAction.Shift(ShiftState.OFF), actions.last())
        assertEquals(ShiftState.OFF, dispatcher.shiftState)
    }

    @Test
    fun commandAndVniKeysDoNotConsumeOneShotShift() {
        dispatcher.dispatch("shift", eventTimeMillis = 100L)
        dispatcher.dispatch("backspace", eventTimeMillis = 200L)
        dispatcher.dispatch("vni-1", eventTimeMillis = 250L)

        assertEquals(ShiftState.ON, dispatcher.shiftState)
        assertEquals(KeyAction.Backspace, actions[1])
        assertEquals(KeyAction.Input("vni-1", "1"), actions[2])
    }

    @Test
    fun externallyRequestedShiftUsesTheSameOneShotBehavior() {
        dispatcher.setShiftState(ShiftState.ON)

        dispatcher.dispatch("a", eventTimeMillis = 100L)

        assertEquals(listOf(input("A")), actions)
        assertEquals(ShiftState.OFF, dispatcher.shiftState)
        assertEquals(2, shiftStateChangeCount)
    }

    private fun input(text: String): KeyAction = KeyAction.Input("a", text)

    private fun key(
        id: String,
        role: KeyRole,
        label: String = role.name,
        shiftedLabel: String? = null,
    ): KeySpec = KeySpec(
        id = id,
        label = label,
        role = role,
        shiftedLabel = shiftedLabel,
        accessibilityLabel = role.name,
    )

    private companion object {
        const val DoubleTapTimeoutMillis = 300L
    }
}
