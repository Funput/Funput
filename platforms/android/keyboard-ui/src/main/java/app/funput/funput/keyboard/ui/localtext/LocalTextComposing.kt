package app.funput.funput.keyboard.ui.localtext

import app.funput.funput.keyboard.model.KeyboardInputMethod

/** One edit a panel's keys make to a [LocalTextComposing] field. */
sealed interface LocalTextEdit {
    /** Text from a character, VNI modifier or punctuation key, already cased by Shift. */
    data class Text(val value: String) : LocalTextEdit
    data object Space : LocalTextEdit
    data object DeleteBackward : LocalTextEdit
}

/**
 * A text field owned by the keyboard itself — the emoji search today — edited by a
 * panel's own keys instead of the host editor.
 *
 * The panel only reports what was pressed; whether that composes Vietnamese is up to
 * the implementation, so keyboard-ui never needs to know about the engine.
 */
interface LocalTextComposing {
    val text: String

    /**
     * The method this field composes with, or null while it keeps keys literal. The
     * panel shapes its keys from it: VNI needs its modifier digits, Telex its hints.
     */
    val inputMethod: KeyboardInputMethod?

    fun apply(edit: LocalTextEdit)

    /** Starts a fresh field; an engine-backed field picks up the current settings here. */
    fun reset()
}

/** The spacing rule every local field shares: never a leading space, never two in a row. */
object LocalTextSpacing {
    fun accepts(after: String): Boolean = after.isNotEmpty() && !after.last().isWhitespace()
}
