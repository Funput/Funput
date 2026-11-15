package app.funput.funput.keyboard.interaction.clipboard

import app.funput.funput.keyboard.interaction.gestures.KeyHoldController
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import kotlin.math.hypot

/**
 * Holding the clipboard key opens history. A short tap pastes when content is offered, or opens
 * history when the offer is empty; once the hold fires the release must not also tap.
 */
internal class ClipboardKeyHold(
    schedule: (Runnable, Long) -> Unit,
    cancel: (Runnable) -> Unit,
    private val touchSlop: Float,
    private val onClaimed: (pointerId: Int) -> Unit,
) {
    private val claimed = mutableSetOf<Int>()
    private val origins = mutableMapOf<Int, FloatArray>()
    private val hold = KeyHoldController(schedule, cancel) { pointerId ->
        claimed += pointerId
        onClaimed(pointerId)
    }

    fun start(pointerId: Int, key: KeySpec?, x: Float, y: Float) {
        if (key?.role != KeyRole.CLIPBOARD) return
        origins[pointerId] = floatArrayOf(x, y)
        hold.start(pointerId)
    }

    fun moved(pointerId: Int, x: Float, y: Float) {
        val origin = origins[pointerId] ?: return
        if (hypot((x - origin[0]).toDouble(), (y - origin[1]).toDouble()) > touchSlop) {
            abandon(pointerId)
        }
    }

    fun abandon(pointerId: Int) {
        hold.cancel(pointerId)
        origins.remove(pointerId)
    }

    fun isClaimed(pointerId: Int): Boolean = pointerId in claimed

    /** Returns true when the hold already opened history, so release must not paste. */
    fun finish(pointerId: Int): Boolean {
        hold.cancel(pointerId)
        origins.remove(pointerId)
        return claimed.remove(pointerId)
    }

    fun cancelAll() {
        hold.cancelAll()
        claimed.clear()
        origins.clear()
    }
}
