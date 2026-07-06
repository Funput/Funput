package app.funput.funput.keyboard.interaction

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.ShiftState

internal class ShiftStateController(
    private val doubleTapTimeoutMillis: Long = DefaultDoubleTapTimeoutMillis,
) {
    var state: ShiftState = ShiftState.OFF
        private set

    private var lastShiftUpTimeMillis: Long? = null

    init {
        require(doubleTapTimeoutMillis > 0L) { "Double-tap timeout must be positive" }
    }

    fun onShiftReleased(eventTimeMillis: Long): ShiftState {
        state = when (state) {
            ShiftState.OFF -> ShiftState.ON
            ShiftState.ON -> if (isDoubleTap(eventTimeMillis)) ShiftState.CAPS_LOCK else ShiftState.OFF
            ShiftState.CAPS_LOCK -> ShiftState.OFF
        }
        lastShiftUpTimeMillis = if (state == ShiftState.ON) eventTimeMillis else null
        return state
    }

    fun consumeAfter(role: KeyRole): Boolean {
        if (state != ShiftState.ON || role != KeyRole.CHARACTER) return false
        state = ShiftState.OFF
        lastShiftUpTimeMillis = null
        return true
    }

    fun setState(value: ShiftState): Boolean {
        if (state == value) return false
        state = value
        lastShiftUpTimeMillis = null
        return true
    }

    fun reset(): Boolean {
        if (state == ShiftState.OFF) return false
        state = ShiftState.OFF
        lastShiftUpTimeMillis = null
        return true
    }

    private fun isDoubleTap(eventTimeMillis: Long): Boolean {
        val previousTime = lastShiftUpTimeMillis ?: return false
        val elapsed = eventTimeMillis - previousTime
        return elapsed in 0..doubleTapTimeoutMillis
    }

    private companion object {
        const val DefaultDoubleTapTimeoutMillis = 300L
    }
}
