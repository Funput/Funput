package app.funput.funput.ime.editing.reopen

import app.funput.funput.ime.editing.wordAtEnd

/**
 * The text before the caret as Backspace saw it, [before] its delete and [after] it.
 *
 * Hosts disagree on what the second read shows. Most answer with the text after the delete. Some
 * (Chrome's cached input state) still answer with the text before it inside the batch, which makes
 * the two reads equal; that text is judged with the erased characters taken off. Others withhold
 * the text altogether (null or empty), and then nothing here can confirm or deny a word.
 */
internal class BackspaceReads(private val before: CharSequence?, private val after: CharSequence?) {
    /** The host shows no text: only the caret anchor can vouch for a remembered word. */
    private val withheld = before.isNullOrEmpty() && after.isNullOrEmpty()

    /** The pre-delete text, when the read after the delete still returns it unchanged. */
    private val stale: CharSequence? = before?.takeIf { !withheld && it.toString() == after?.toString() }

    /** The word right before the caret as the editor shows it, unless that read is stale. */
    val editorWord: String? get() = if (stale != null) null else after?.wordAtEnd()

    /** Whether the editor's text agrees that [word] ends right before the caret once [erased] is gone. */
    fun confirms(word: String, erased: String): Boolean = when {
        withheld -> true
        stale != null -> stale.endsWith(erased) && stale.dropLast(erased.length).wordAtEnd() == word
        else -> after?.wordAtEnd() == word
    }
}
