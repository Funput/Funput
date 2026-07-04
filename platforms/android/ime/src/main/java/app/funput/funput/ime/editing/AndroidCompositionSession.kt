package app.funput.funput.ime.editing

import android.text.SpannableString
import android.view.inputmethod.InputConnection
import app.funput.funput.ime.nativebridge.VietnameseEngine
import app.funput.funput.keyboard.model.KeyboardInputMethod

/** Maps the shared engine buffer onto Android's composing-text primitives. */
internal class AndroidCompositionSession(
    private val engine: VietnameseEngine,
    private val composingTextFactory: (String) -> CharSequence = ::unstyledComposingText,
) {
    var composingText: String = ""
        private set

    val isComposing: Boolean get() = composingText.isNotEmpty()

    fun setInputMethod(method: KeyboardInputMethod) = engine.setInputMethod(method)

    fun setEnabled(enabled: Boolean) = engine.setEnabled(enabled)

    fun input(connection: InputConnection?, text: String): Boolean {
        if (connection == null || text.isEmpty()) return false
        val codePoint = text.singleCodePointOrNull() ?: return commitRaw(connection, text)
        return if (CompositionBoundary.isBoundary(codePoint)) {
            commitBoundary(connection, text, codePoint)
        } else {
            updateComposing(connection, engine.process(codePoint), text)
        }
    }

    fun backspace(connection: InputConnection?): Boolean {
        if (connection == null || !isComposing) return false
        composingText = engine.backspace()
        return connection.setComposingText(composingTextFactory(composingText), CursorAfterText)
    }

    fun finish(connection: InputConnection?) {
        if (isComposing) connection?.finishComposingText()
        reset()
    }

    fun reset() {
        composingText = ""
        engine.clear()
    }

    private fun commitBoundary(
        connection: InputConnection,
        text: String,
        codePoint: Int,
    ): Boolean {
        val replacement = engine.processBoundary(codePoint)
        composingText = ""
        if (replacement != null) return connection.commitText(replacement, CursorAfterText)
        connection.finishComposingText()
        return connection.commitText(text, CursorAfterText)
    }

    private fun updateComposing(
        connection: InputConnection,
        buffer: String,
        rawText: String,
    ): Boolean {
        composingText = buffer
        return if (buffer.isEmpty()) commitRaw(connection, rawText) else {
            connection.setComposingText(composingTextFactory(buffer), CursorAfterText)
        }
    }

    private fun commitRaw(connection: InputConnection, text: String): Boolean {
        finish(connection)
        return connection.commitText(text, CursorAfterText)
    }

    private fun String.singleCodePointOrNull(): Int? {
        val first = codePointAt(0)
        return first.takeIf { Character.charCount(it) == length }
    }

    private companion object {
        const val CursorAfterText = 1
    }
}

private fun unstyledComposingText(text: String): CharSequence = SpannableString(text)

/** Mirrors the shared engine's current word-boundary contract. */
internal object CompositionBoundary {
    fun isBoundary(codePoint: Int): Boolean =
        Character.isWhitespace(codePoint) || codePoint.isAsciiPunctuation()

    private fun Int.isAsciiPunctuation(): Boolean =
        this in 0x21..0x2F ||
            this in 0x3A..0x40 ||
            this in 0x5B..0x60 ||
            this in 0x7B..0x7E
}
