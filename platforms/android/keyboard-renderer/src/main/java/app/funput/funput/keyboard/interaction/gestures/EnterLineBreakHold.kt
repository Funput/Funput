package app.funput.funput.keyboard.interaction.gestures

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.KeyboardEnterAction

/**
 * Holding Enter types a line break where Enter performs the field's action, such as Send in a chat
 * app. Once the hold fires the contact is claimed, so lifting the finger does not also send.
 *
 * Where Enter already breaks the line there is nothing to add, and the key behaves as it always has.
 */
internal class EnterLineBreakHold(
    schedule: (Runnable, Long) -> Unit,
    cancel: (Runnable) -> Unit,
    private val onClaimed: (pointerId: Int) -> Unit,
) {
    var enterAction: KeyboardEnterAction = KeyboardEnterAction.Standard.NEW_LINE


    private val claimed = mutableSetOf<Int>()
    private val hold = KeyHoldController(schedule, cancel) { pointerId ->
        claimed += pointerId
        onClaimed(pointerId)
    }

    fun start(pointerId: Int, key: KeySpec) {
        if (isAvailable(enterAction) && key.role == KeyRole.ENTER) hold.start(pointerId)
    }

    /** A finger that slid off before the hold fired is a tap or a swipe, not a hold. */
    fun abandon(pointerId: Int) = hold.cancel(pointerId)

    fun isClaimed(pointerId: Int): Boolean = pointerId in claimed

    /** Returns true when the hold fired, so release must not emit the ordinary Enter. */
    fun finish(pointerId: Int): Boolean {
        hold.cancel(pointerId)
        return claimed.remove(pointerId)
    }

    fun cancelAll() {
        hold.cancelAll()
        claimed.clear()
    }

    companion object {
        /** Whether holding an Enter key showing [action] does something other than a tap on it. */
        fun isAvailable(action: KeyboardEnterAction): Boolean = action != KeyboardEnterAction.Standard.NEW_LINE
    }
}
