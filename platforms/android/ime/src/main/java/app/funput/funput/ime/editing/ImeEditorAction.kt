package app.funput.funput.ime.editing

import app.funput.funput.keyboard.model.KeyboardEnterAction

/** UI presentation and editor command for the current field's Enter key. */
internal data class ImeEditorAction(
    val presentation: KeyboardEnterAction,
    val command: ImeEditCommand,
) {
    companion object {
        val NewLine = ImeEditorAction(
            presentation = KeyboardEnterAction.Standard.NEW_LINE,
            command = ImeEditCommand.CommitText("\n"),
        )
    }
}
