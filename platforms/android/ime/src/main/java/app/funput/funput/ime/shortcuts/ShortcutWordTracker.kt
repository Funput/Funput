package app.funput.funput.ime.shortcuts

import android.view.inputmethod.InputConnection

/** Tracks the raw word even before an asynchronous shortcut snapshot arrives. */
internal class ShortcutWordTracker {
    private var word = ""
    val isActive: Boolean get() = word.isNotEmpty()

    fun input(text: String, isBoundary: (Int) -> Boolean): Boolean {
        val codePoint = text.singleCodePointOrNull()
        if (codePoint == null || isBoundary(codePoint)) {
            clear()
            return true
        }
        word += text
        return false
    }

    fun backspace(): Boolean {
        if (word.isNotEmpty()) word = word.substring(0, word.offsetByCodePoints(word.length, -1))
        return word.isEmpty()
    }

    fun reconcile(connection: InputConnection?): Boolean {
        if (word.isEmpty()) return true
        val before = connection?.getTextBeforeCursor(word.length, 0)?.toString()
        val valid = connection != null && connection.getSelectedText(0).isNullOrEmpty() &&
            before?.endsWith(word) == true
        if (!valid) clear()
        return valid
    }

    fun clear() {
        word = ""
    }

    private fun String.singleCodePointOrNull(): Int? =
        takeIf { it.codePointCount(0, length) == 1 }?.codePointAt(0)
}
