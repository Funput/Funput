package app.funput.funput.keyboard.interaction.gestures

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec

/**
 * Holding Enter types a line break where Enter performs the field's action, such as Send in a chat
 * app. Once the hold fires the contact is claimed, so lifting the finger does not also send.
 *
 * The IME turns it on through [isAvailable], only for a multi-line field whose Enter shows an
 * action: anywhere else a hold would just perform that action early, or break nothing.
 */
internal class EnterLineBreakHold(
    schedule: (Runnable, Long) -> Unit,
    cancel: (Runnable) -> Unit,
    private val onClaimed: (pointerId: Int) -> Unit,
) {
    var isAvailable = false

    private val claimed = mutableSetOf<Int>()
    private val hold = KeyHoldController(schedule, cancel) { pointerId ->
        claimed += pointerId
        onClaimed(pointerId)
    }

    fun start(pointerId: Int, key: KeySpec) {
        if (isAvailable && key.role == KeyRole.ENTER) hold.start(pointerId)
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
}
