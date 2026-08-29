package app.funput.funput.ime.editing.typing

import android.view.inputmethod.InputConnection
import app.funput.funput.ime.editing.AndroidCompositionSession
import app.funput.funput.ime.editing.ImeEditCommand
import app.funput.funput.ime.editing.ImeSuggestionSession
import app.funput.funput.ime.editing.InputConnectionEditor

/**
 * The ordinary path text takes into the document: characters, Enter, and text arriving from the
 * emoji and clipboard panels.
 *
 * Sits beside the backspace and gesture handlers rather than inside the router, which is left to
 * decide *which* path an action takes rather than how each one writes.
 */
internal class ImeTypingHandler(
    private val composition: AndroidCompositionSession,
    private val editor: InputConnectionEditor,
    private val connection: () -> InputConnection?,
    private val enterCommand: () -> ImeEditCommand,
    private val suggestions: ImeSuggestionSession,
    private val usesComposition: () -> Boolean,
    private val suggestionsAllowed: () -> Boolean,
    private val finish: () -> Unit,
) {
    fun input(text: String) {
        if (usesComposition()) {
            val current = connection()
            if (current == null) return suggestions.reset()
            composition.input(current, text)
            suggestions.updateComposition()
        } else {
            if (execute(ImeEditCommand.CommitText(text)) && suggestionsAllowed()) {
                suggestions.inputDirect(text)
            } else {
                suggestions.reset()
            }
        }
    }

    fun enter() {
        val command = enterCommand()
        if (usesComposition() && command == ImeEditCommand.CommitText("\n")) {
            composition.input(connection(), "\n")
        } else {
            if (usesComposition()) finish()
            else if (suggestionsAllowed()) suggestions.inputDirect("\n")
            execute(command)
        }
    }

    /** Text the user picked from a panel rather than typed; never joins a composition. */
    fun commitExternal(text: String) {
        finish()
        execute(ImeEditCommand.CommitText(text))
    }

    private fun execute(command: ImeEditCommand) = editor.execute(connection(), command)
}
