package app.funput.funput.ime.editing.caret

import android.view.KeyEvent
import android.view.inputmethod.InputConnection
import kotlin.math.abs
import kotlin.math.min

/**
 * Moves the caret vertically by handing arrow keys to the editor.
 *
 * Deliberately not computed here. The editor is the only party that knows where its text wraps,
 * and a wrapped paragraph is what most vertical drags actually cross — measuring newlines from
 * this side would step over the whole paragraph instead of the line the user can see. iOS has no
 * equivalent, which is why it has no vertical pan at all.
 */
internal object CaretLineKeys {
    /** Which arrow, and how many presses, a step of [lines] becomes. */
    data class Plan(val keyCode: Int, val presses: Int)

    /** Null when the step moves nothing. Pure, so the arithmetic is testable off-device. */
    fun plan(lines: Int): Plan? {
        if (lines == 0) return null
        val keyCode = if (lines < 0) KeyEvent.KEYCODE_DPAD_UP else KeyEvent.KEYCODE_DPAD_DOWN
        return Plan(keyCode, min(abs(lines), MaxLinesPerStep))
    }

    /** Returns whether any key went out. */
    fun move(connection: InputConnection, lines: Int): Boolean {
        val plan = plan(lines) ?: return false
        repeat(plan.presses) {
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, plan.keyCode))
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, plan.keyCode))
        }
        return true
    }

    /**
     * A single drag step is one or two lines in practice. The cap only guards against a wild value
     * turning into a burst of key events the editor has to chew through.
     */
    private const val MaxLinesPerStep = 8
}
