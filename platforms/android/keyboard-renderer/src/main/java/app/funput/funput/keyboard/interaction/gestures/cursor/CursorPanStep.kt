package app.funput.funput.keyboard.interaction.gestures.cursor

/**
 * One tick of the spacebar trackpad, expressed in document units rather than pixels.
 *
 * Both axes travel in the same step: the caret follows the finger instead of locking to whichever
 * axis happened to move first.
 *
 * @property columns characters to move within the line; positive moves right.
 * @property lines lines to move — logical lines separated by a newline, not soft-wrapped ones,
 *   which an IME cannot see; positive moves down.
 */
internal data class CursorPanStep(val columns: Int = 0, val lines: Int = 0) {
    val isEmpty: Boolean get() = columns == 0 && lines == 0
}
