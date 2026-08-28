package app.funput.funput.keyboard.accessibility

import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyRole

/** TalkBack custom actions for the three smart gestures VoiceOver/TalkBack otherwise swallow. */
internal object SmartGestureAccessibility {
    const val DeleteWord = 0x01030000
    const val CursorLeft = 0x01030001
    const val CursorRight = 0x01030002
    const val CursorUp = 0x01030003
    const val CursorDown = 0x01030004

    data class Action(val actionId: Int, val label: String)

    fun actions(role: KeyRole, enabled: Boolean): List<Action> {
        if (!enabled) return emptyList()
        return when (role) {
            KeyRole.BACKSPACE -> listOf(Action(DeleteWord, "Xóa cả từ"))
            KeyRole.SPACE -> listOf(
                Action(CursorLeft, "Con trỏ sang trái"),
                Action(CursorRight, "Con trỏ sang phải"),
                Action(CursorUp, "Con trỏ lên"),
                Action(CursorDown, "Con trỏ xuống"),
            )
            else -> emptyList()
        }
    }

    fun keyAction(actionId: Int): KeyAction? = when (actionId) {
        DeleteWord -> KeyAction.DeleteWord
        CursorLeft -> KeyAction.MoveCursor(columns = -1)
        CursorRight -> KeyAction.MoveCursor(columns = 1)
        CursorUp -> KeyAction.MoveCursor(columns = 0, lines = -1)
        CursorDown -> KeyAction.MoveCursor(columns = 0, lines = 1)
        else -> null
    }
}
