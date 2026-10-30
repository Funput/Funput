package app.funput.funput.keyboard.accessibility

import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyRole

/**
 * TalkBack custom actions for the gestures TalkBack otherwise swallows: the three smart gestures,
 * and holding Enter for a line break where Enter performs the field's action.
 */
internal object SmartGestureAccessibility {
    const val DeleteWord = 0x01030000
    const val CursorLeft = 0x01030001
    const val CursorRight = 0x01030002
    const val CursorUp = 0x01030003
    const val CursorDown = 0x01030004
    const val LineBreak = 0x01030005

    data class Action(val actionId: Int, val label: String)

    /** The line break is offered whenever holding Enter types one; it is not a smart gesture. */
    fun actions(role: KeyRole, enabled: Boolean, breaksLineOnEnterHold: Boolean = false): List<Action> =
        when {
            role == KeyRole.ENTER && breaksLineOnEnterHold -> listOf(Action(LineBreak, "Xuống dòng"))
            !enabled -> emptyList()
            role == KeyRole.BACKSPACE -> listOf(Action(DeleteWord, "Xóa cả từ"))
            role == KeyRole.SPACE -> listOf(
                Action(CursorLeft, "Con trỏ sang trái"),
                Action(CursorRight, "Con trỏ sang phải"),
                Action(CursorUp, "Con trỏ lên"),
                Action(CursorDown, "Con trỏ xuống"),
            )
            else -> emptyList()
        }

    fun keyAction(actionId: Int): KeyAction? = when (actionId) {
        DeleteWord -> KeyAction.DeleteWord
        CursorLeft -> KeyAction.MoveCursor(columns = -1)
        CursorRight -> KeyAction.MoveCursor(columns = 1)
        CursorUp -> KeyAction.MoveCursor(columns = 0, lines = -1)
        CursorDown -> KeyAction.MoveCursor(columns = 0, lines = 1)
        LineBreak -> KeyAction.NewLine
        else -> null
    }
}
