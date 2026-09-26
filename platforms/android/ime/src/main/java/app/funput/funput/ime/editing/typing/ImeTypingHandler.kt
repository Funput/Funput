package app.funput.funput.ime.editing.typing

import android.view.inputmethod.InputConnection
import app.funput.funput.ime.editing.AndroidCompositionSession
import app.funput.funput.ime.editing.ImeEditCommand
import app.funput.funput.ime.editing.ImeSuggestionSession
import app.funput.funput.ime.editing.InputConnectionEditor
import app.funput.funput.ime.shortcuts.ImeShortcutSession

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
    private val usesVietnameseComposition: () -> Boolean,
    private val usesEnglishShortcuts: () -> Boolean,
    private val suggestionsAllowed: () -> Boolean,
    private val shortcuts: ImeShortcutSession,
    private val inputTracked: (String) -> Unit,
    private val finish: () -> Unit,
    private val onTyping: () -> Unit = {},
) {
    fun input(text: String) {
        if (text.isNotEmpty()) onTyping()
        val handled = if (usesVietnameseComposition()) {
            val current = connection()
            if (current == null) {
                suggestions.reset()
            } else {
                composition.input(current, text)
            }
            suggestions.updateComposition()
            current != null
        } else if (usesEnglishShortcuts()) {
            val current = connection()
            if (current == null) {
                suggestions.reset()
                false
            } else {
                val expanded = shortcuts.inputEnglish(current, text)
                if (expanded) suggestions.reset()
                else if (suggestionsAllowed()) suggestions.inputDirect(text)
                true
            }
        } else {
            if (execute(ImeEditCommand.CommitText(text)) && suggestionsAllowed()) {
                suggestions.inputDirect(text)
            } else {
                suggestions.reset()
            }
            true
        }
        if (handled) inputTracked(text)
    }

    fun enter() {
        onTyping()
        val command = enterCommand()
        if ((usesVietnameseComposition() || usesEnglishShortcuts()) &&
            command == ImeEditCommand.CommitText("\n")) {
            input("\n")
        } else {
            if (usesVietnameseComposition() || usesEnglishShortcuts()) finish()
            else if (suggestionsAllowed()) suggestions.inputDirect("\n")
            execute(command)
        }
    }

    /** Text the user picked from a panel rather than typed; never joins a composition. */
    fun commitExternal(text: String) {
        if (text.isNotEmpty()) onTyping()
        finish()
        execute(ImeEditCommand.CommitText(text))
    }

    private fun execute(command: ImeEditCommand) = editor.execute(connection(), command)
}
