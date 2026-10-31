package app.funput.funput.keyboard.suggestion.actions

import app.funput.funput.keyboard.interaction.gestures.KeyHoldController
import app.funput.funput.keyboard.model.SuggestionSelection
import app.funput.funput.keyboard.suggestion.SuggestionChipKind
import kotlin.math.hypot

/**
 * Turns a finger resting on a suggestion into a long press.
 *
 * Only a word the host says it can edit is held: a repair, an undo, or a completion the
 * editor offered has nothing to forget, and a hold on one stays an ordinary tap. A finger that
 * wanders past the touch slop, or a second finger landing, means typing rather than holding.
 * Once the hold fires the contact is claimed, so lifting it does not also pick the word.
 */
internal class SuggestionHold(
    schedule: (Runnable, Long) -> Unit,
    cancel: (Runnable) -> Unit,
    private val touchSlop: Float,
    private val isEditable: (SuggestionSelection) -> Boolean,
    private val onHeld: (pointerId: Int, SuggestionSelection) -> Unit,
) {
    private class Pending(val selection: SuggestionSelection, val x: Float, val y: Float)

    private val pending = mutableMapOf<Int, Pending>()
    private val claimed = mutableSetOf<Int>()
    private val timer = KeyHoldController(schedule, cancel, HoldDelayMillis, ::fire)

    fun start(pointerId: Int, selection: SuggestionSelection?, x: Float, y: Float) {
        pending.keys.toList().forEach(::abandon)
        if (selection?.kind != SuggestionChipKind.SUGGESTION) return
        pending[pointerId] = Pending(selection, x, y)
        timer.start(pointerId)
    }

    fun move(pointerId: Int, x: Float, y: Float) {
        val start = pending[pointerId] ?: return
        if (hypot(x - start.x, y - start.y) > touchSlop) abandon(pointerId)
    }

    fun isClaimed(pointerId: Int): Boolean = pointerId in claimed

    /** Returns true when the hold fired, so the release must not pick the word. */
    fun finish(pointerId: Int): Boolean {
        abandon(pointerId)
        return claimed.remove(pointerId)
    }

    fun cancelAll() {
        timer.cancelAll()
        pending.clear()
        claimed.clear()
    }

    private fun abandon(pointerId: Int) {
        timer.cancel(pointerId)
        pending.remove(pointerId)
    }

    private fun fire(pointerId: Int) {
        val held = pending.remove(pointerId) ?: return
        if (!isEditable(held.selection)) return
        claimed += pointerId
        onHeld(pointerId, held.selection)
    }

    internal companion object {
        /** About the system long-press timeout, so it feels like a hold anywhere else on the phone. */
        const val HoldDelayMillis = 450L
    }
}
