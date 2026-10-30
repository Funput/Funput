package app.funput.funput.ime.editing.entersend

import android.view.inputmethod.EditorInfo
import app.funput.funput.ime.editing.EditorInfoPolicy
import app.funput.funput.ime.editing.ImeEditCommand
import app.funput.funput.ime.editing.ImeEditorAction
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardEnterAction

/**
 * Turns Enter into Send in the message box of an app the user chose.
 *
 * Chat apps such as Messenger keep Enter as a new line by setting `IME_FLAG_NO_ENTER_ACTION`,
 * and the action they declare beside it is not always the one that sends: Messenger declares
 * DONE, which only hides the keyboard, yet sends on `IME_ACTION_SEND`. So the command is always
 * Send, whatever the field declared.
 *
 * Only a multi-line text field that would otherwise insert a new line changes. A field that
 * already shows an action (a search box) and single-line or non-text fields are left as they are.
 */
internal object EnterToSendPolicy {
    val Send = ImeEditorAction(
        presentation = KeyboardEnterAction.Standard.SEND,
        command = ImeEditCommand.PerformEditorAction(EditorInfo.IME_ACTION_SEND),
    )

    fun apply(policy: EditorInfoPolicy, packageName: String?, packages: Set<String>): EditorInfoPolicy {
        val applies = packageName != null &&
            packageName in packages &&
            policy.isMultiline &&
            policy.editorMode == KeyboardEditorMode.TEXT &&
            policy.editorAction == ImeEditorAction.NewLine
        return if (applies) policy.copy(editorAction = Send) else policy
    }
}
