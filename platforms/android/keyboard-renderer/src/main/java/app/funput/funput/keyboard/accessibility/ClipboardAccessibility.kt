package app.funput.funput.keyboard.accessibility

import app.funput.funput.keyboard.model.KeyRole

/**
 * TalkBack custom action for opening clipboard history. Holding the key does the same for
 * sighted users; TalkBack cannot long-press, so the action stays on the key.
 */
internal object ClipboardAccessibility {
    const val OpenHistory = 0x01030006

    fun actions(role: KeyRole): List<SmartGestureAccessibility.Action> =
        if (role == KeyRole.CLIPBOARD) {
            listOf(SmartGestureAccessibility.Action(OpenHistory, "Lịch sử bảng nhớ tạm"))
        } else {
            emptyList()
        }

    fun isOpenHistory(actionId: Int): Boolean = actionId == OpenHistory
}
